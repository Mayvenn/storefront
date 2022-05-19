(ns storefront.system.contentful
  (:require [clojure.walk :as walk]
            [com.stuartsierra.component :as component]
            [lambdaisland.uri :as uri]
            [storefront.system.scheduler :as scheduler]
            [storefront.utils :as utils]
            [ring.util.response :as util.response]
            [spice.date :as date]
            [spice.maps :as maps]
            [tugboat.core :as tugboat]
            [clojure.string :as string]
            [clojure.set :as set]))

(defn contentful-request
  "Wrapper for the Content Delivery endpoint"
  [{:keys [endpoint api-key space-id]} resource-type params]
  (try
    (tugboat/request {:endpoint endpoint}
                     :get (str "/spaces/" space-id "/" resource-type)
                     {:socket-timeout 30000
                      :conn-timeout   30000
                      :as             :json
                      :query-params   (merge {:access_token api-key} params)})
    (catch java.io.IOException ioe
      nil)))

(defn extract-fields
  "Contentful resources are boxed.

  We extract the fields, merging useful meta-data."
  [{:keys [fields sys]}]
  (merge (maps/kebabify fields)
         {:content/updated-at (spice.date/to-millis (:updatedAt sys 0))
          :content/type       (or
                               (some-> sys :contentType :sys :id)
                               "Asset")
          :content/id         (-> sys :id)}))

(defn extract-latest-by
  "Extract a resource (e.g. items), grouped by a key, and taking the latest"
  [resource index-key]
  (->> (map extract-fields resource)
       (group-by index-key)
       (maps/map-values (partial apply max-key :content/updated-at))))

(defn extract
  "Extract resources in a response body"
  [body]
  (-> body
      (update-in [:items]
                 extract-latest-by :content/type)
      (update-in [:includes :Entry]
                 extract-latest-by :content/id)
      (update-in [:includes :Asset]
                 extract-latest-by :content/id)))

(defn ^:private link->value [includes link]
  (let [id        (some-> link :sys :id)
        link-type (some-> link :sys :link-type keyword)]
    (get-in includes [link-type id] link)))

(defn resolve-link
  "Resolves contentful links in field values by stitching included
  resources in situ."
  [includes [key value]]
  [key (cond
         (map? value)    (link->value includes value)
         (vector? value) (mapv (partial link->value includes) value)
         :else           value)])

(defn resolve-children
  "Resolves child links in parent maps by lookup in includes."
  [includes parent-map]
  (maps/map-values (fn [child-map]
                     (into {}
                           (map (partial resolve-link includes))
                           child-map))
                   parent-map))

(defn resolve-all
  "Resolves Assets under Entries within items."
  [{:keys [items includes]}]
  (-> includes
      (update :Entry (partial resolve-children includes))
      (resolve-children items)))

(defn resolve-all-collection
  "Resolves Assets under Entries within items."
  [{:keys [items includes]}]
  (let [resolved-includes (-> includes
                              (update :Asset (partial mapv (partial resolve-children includes)))
                              (update :Asset (partial maps/index-by (comp :id :sys)))
                              (update :Entry (partial mapv (partial resolve-children includes)))
                              (update :Entry (partial maps/index-by (comp :id :sys))))]
    (walk/postwalk
     (fn [form]
       (if (and (map? form)
                (-> form :sys :type (= "Link")))
         (if-let [resolved-include (or (get-in resolved-includes [:Entry (-> form :sys :id)])
                                       (get-in resolved-includes [:Asset (-> form :sys :id)]))]
           (extract-fields resolved-include)
           form)
         form))
     items)))

;; Used for homepage

(defn ^:private condense
  [data]
  (->> data
       (mapv (fn [asset-or-entry]
               (let [id (-> asset-or-entry :sys :id)
                     type (-> asset-or-entry :sys :contentType :sys :id)]
                 [id (-> asset-or-entry
                         :fields
                         (assoc :content/id id)
                         (assoc :content/type type))])))
       (into {})))

(defn ^:private replace-data [lookup-table]
  (fn [form]
    (if (and (map? form)
             (-> form :sys :type (= "Link")))
      (if-let [value (get lookup-table (-> form :sys :id))]
        value
        form)
      form)))

(defn condense-items-with-includes
  [{:keys [items includes]}]
  (let [assets  (condense (:Asset includes))
        entries (condense (:Entry includes))]
    (->> items
         (mapv (fn [item]
                 (assoc-in item [:fields :content/id] (-> item :sys :id))))
         (walk/postwalk (replace-data entries))
         (walk/postwalk (replace-data assets))
         (mapv (comp maps/kebabify :fields)))))

(defn format-answer [{:as answer :keys [content]}]
  (mapv (fn [paragraph]
          {:paragraph
           (->> paragraph
                :content
                (map
                 (fn [{:keys [node-type content data value] :as node}]
                   (cond (= node-type "text")
                         {:text value}

                         (= node-type "hyperlink")
                         {:text (-> content first :value)
                          :url  (-> data :uri)}))))})
       content))

(defn fetch-all
  [{:keys [logger exception-handler cache env-param] :as contentful}]
  (let [limit 500]
    (->> ["entries" "assets"]
         (map
          (fn [resource-type]
            (loop [skip     0
                   acc      {}
                   failures 0]
              (when (> 2 failures)
                (let [{:keys [status body]} (contentful-request contentful
                                                                resource-type
                                                                {:include 0
                                                                 :limit   limit
                                                                 :skip    skip})
                      merged-content        (into acc (some->> body
                                                               :items
                                                               (map (fn [item] [(-> item :sys :id keyword) item]))))]
                  (cond (not (and status (<= 200 status 299)))
                        (recur skip acc (inc failures))

                        (> (:total body) (+ skip limit))
                        (recur (+ skip limit) merged-content failures)

                        :else
                        merged-content))))))
         (apply merge))))

;; GROT: Deprecated in favor of full CMS DB pull, and query-side assembly
(defn do-fetch-entries
  ([contentful content-params]
   (do-fetch-entries contentful content-params 1))
  ([{:keys [logger exception-handler cache env-param] :as contentful}
    {:keys [content-type
            latest?
            select
            exists
            primary-key-fn
            item-tx-fn
            collection-tx-fn]
     :or   {item-tx-fn       identity
            collection-tx-fn identity}
     :as   content-params} attempt-number]
   (when (<= attempt-number 2)
     (let [{:keys [status body]} (contentful-request
                                  contentful
                                  "entries"
                                  (merge
                                   {"content_type" (name content-type)
                                    "include"      10}
                                   (when exists
                                     (reduce
                                      (fn [m field]
                                        (assoc m (str field "[exists]") true))
                                      {}
                                      exists))
                                   (when select
                                     {"select" (string/join "," select)})
                                   (when latest?
                                     {"limit"                           1
                                      :order                            (str "-fields." env-param)
                                      (str "fields." env-param "[lte]") (date/to-iso (date/now))})))]
       (if (and status (<= 200 status 299))
         (swap! cache merge
                (cond
                  (contains? #{:mayvennMadePage :advertisedPromo} content-type)
                  (some-> body extract resolve-all walk/keywordize-keys (select-keys [content-type]))

                  (= :ugc-collection content-type)
                  (some->> body
                           resolve-all-collection
                           (mapv extract-fields)
                           walk/keywordize-keys
                           (mapv item-tx-fn)
                           (maps/index-by primary-key-fn)
                           collection-tx-fn
                           (assoc {} content-type))

                  (= :faq content-type)
                  (some->> body
                           resolve-all-collection
                           (mapv extract-fields)
                           walk/keywordize-keys
                           (mapv (juxt :faq-section :questions-answers))
                           (map (fn [[faq-section questions-answers]]
                                  {:slug             faq-section
                                   :question-answers (map
                                                      (fn [{:keys [question answer]}]
                                                        {:question {:text question}
                                                         :answer   (format-answer answer)})
                                                      questions-answers)}))
                           (maps/index-by primary-key-fn)
                           (assoc {} content-type))

                  :else
                  (some->> body
                           resolve-all-collection
                           (mapv extract-fields)
                           walk/keywordize-keys
                           (mapv item-tx-fn)
                           (maps/index-by primary-key-fn)
                           collection-tx-fn
                           (assoc {} content-type))))

         (do-fetch-entries contentful content-params (inc attempt-number)))))))

(defprotocol CMSCache
  (read-cache [_] "Returns a map representing the CMS cache") ;; GROT: deprecated in favor of separate retrieval and processing using the normalized cache
  (read-normalized-cache [_] "Returns a map representing the normalized CMS cache")
  (upsert-into-normalized-cache [_ _]))

(defrecord ContentfulContext [logger exception-handler environment cache-timeout api-key space-id endpoint scheduler]
  component/Lifecycle
  (start [c]
    (let [production?             (= environment "production")
          cache                   (atom {})
          normalized-cache        (atom {})
          env-param               (if production?
                                    "production"
                                    "acceptance")
          content-type-parameters [{:content-type   :faq
                                    :latest?        false
                                    :primary-key-fn (comp keyword :slug)
                                    :exists         ["fields.faqSection"]
                                    :select         ["fields.faqSection"
                                                     "fields.questionsAnswers"]}
                                   {:content-type :advertisedPromo
                                    :latest?      true}
                                   {:content-type   :ugc-collection
                                    :exists         ["fields.slug"]
                                    :primary-key-fn (comp keyword :slug)
                                    :select         [(if production?
                                                       "fields.looks"
                                                       "fields.acceptanceLooks")
                                                     "fields.slug"
                                                     "fields.name"
                                                     "sys.contentType"
                                                     "sys.updatedAt"
                                                     "sys.id"
                                                     "sys.type"]
                                    :item-tx-fn     (fn [u]
                                                      (let [u' (if production?
                                                                 (dissoc u :acceptance-looks)
                                                                 (set/rename-keys u {:acceptance-looks :looks}))]
                                                        (utils/?update u' :looks (partial remove :sys))))
                                    :collection-tx-fn
                                    (fn [m]
                                      (->> (vals m)
                                           (mapcat :looks)
                                           (maps/index-by (comp keyword :content/id))
                                           (assoc m :all-looks)))
                                    :latest?        false}]]
      ;; GROT: Deprecated in favor of using the normalized cache below.
      (doseq [content-params content-type-parameters]
        (scheduler/every scheduler (cache-timeout)
                         (str "poller for " (:content-type content-params))
                         #(do-fetch-entries (assoc c :cache cache :env-param env-param) content-params)))

      (scheduler/every scheduler (cache-timeout)
                       "poller for normalized CMS cache"
                       #(reset! normalized-cache (fetch-all c)))
      (assoc c
             :cache cache
             :normalized-cache normalized-cache)))
  (stop [c]
    (dissoc c :cache))
  CMSCache
  (read-cache [c] (deref (:cache c))) ;; GROT: deprecated in favor of separate retrieval and processing using the normalized cache
  (read-normalized-cache [c] (deref (:normalized-cache c)))
  (upsert-into-normalized-cache [c node]
    (swap-vals! (:normalized-cache c) #(assoc % (-> node :sys :id keyword) node))))

(defn marketing-site-redirect [req]
  (let [prefix (partial str "https://")
        url    (-> req :query-params (get "to") prefix)
        host   (-> url uri/uri :host)
        to     (if (contains? #{"looks.mayvenn.com"} host)
                 url "https://shop.mayvenn.com")]
    (util.response/redirect to)))

(defn derive-all-looks [cms-data]
  (assoc-in cms-data [:ugc-collection :all-looks]
            (->> (:ugc-collection cms-data)
                 vals
                 (mapcat :looks)
                 (maps/index-by (comp keyword :content/id)))))
