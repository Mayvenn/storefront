(ns storefront.system.contentful
  (:require [clojure.walk :as walk]
            [com.stuartsierra.component :as component]
            [lambdaisland.uri :as uri]
            [overtone.at-at :as at-at]
            [storefront.utils :as utils]
            [ring.util.response :as util.response]
            [spice.date :as date]
            [spice.maps :as maps]
            [tugboat.core :as tugboat]
            [clojure.string :as string]
            [clojure.set :as set]))

(defn contentful-request
  "Wrapper for the Content Delivery endpoint"
  [{:keys [endpoint api-key space-id]} params]
  (try
    (tugboat/request {:endpoint endpoint}
                     :get (str "/spaces/" space-id "/entries")
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
         (if-let [resolved-entry (get-in resolved-includes [:Entry (-> form :sys :id)])]
           (extract-fields resolved-entry)
           form)
         form))
     items)))

;; Used for homepage

(defn ^:private condense
  [data]
  (->> data
       (mapv (fn [asset] [(-> asset :sys :id) (-> asset :fields)]))
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
   (try
     (when (<= attempt-number 2)
       (let [{:keys [status body]} (contentful-request
                                    contentful
                                    (merge
                                     {"content_type" (name content-type)}
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

                    (= :homepage content-type)
                    (some->> body
                             condense-items-with-includes
                             walk/keywordize-keys
                             (maps/index-by primary-key-fn)
                             (assoc {} content-type))

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

           (do-fetch-entries contentful content-params (inc attempt-number)))))
     (catch Throwable t
       ;; Ideally, we should never get here, but at-at halts all polls that throw exceptions silently.
       ;; This simply reports it and lets the polling continue
       (exception-handler t)
       (logger :error t)))))

(defn- date-time-for-every [[start end] {:keys [seconds]}]
  (range (date/to-millis start) (date/to-millis end) (* 1000 seconds)))

(defn increased-polling-intervals [current-time]
  (let [black-friday            (date/date-time 2020 11 27 04 55 0)
        increased-interval-stop (date/add-delta black-friday {:minutes 15})]
    (when (date/after? increased-interval-stop current-time)
      (let [start-time (if (date/after? current-time black-friday)
                         current-time black-friday)]
        (date-time-for-every [start-time increased-interval-stop] {:seconds 10})))))

(defprotocol CMSCache
  (read-cache [_] "Returns a map representing the CMS cache"))

(defrecord ContentfulContext [logger exception-handler environment cache-timeout api-key space-id endpoint]
  component/Lifecycle
  (start [c]
    (let [pool                    (at-at/mk-pool)
          production?             (= environment "production")
          cache                   (atom {})
          env-param               (if production?
                                    "production"
                                    "acceptance")
          content-type-parameters [{:content-type     :homepage
                                    :latest?          false
                                    :primary-key-fn   (comp keyword :experience)
                                    :item-tx-fn       identity
                                    :collection-tx-fn identity}
                                   {:content-type   :faq
                                    :latest?        false
                                    :primary-key-fn (comp keyword :slug)
                                    :exists         ["fields.faqSection"]
                                    :select         ["fields.faqSection"
                                                     "fields.questionsAnswers"]}
                                   {:content-type :advertisedPromo
                                    :latest?      true}
                                   {:content-type     :ugc-collection
                                    :exists           ["fields.slug"]
                                    :primary-key-fn   (comp keyword :slug)
                                    :select           [(if production?
                                                         "fields.looks"
                                                         "fields.acceptanceLooks")
                                                       "fields.slug"
                                                       "fields.name"
                                                       "sys.contentType"
                                                       "sys.updatedAt"
                                                       "sys.id"
                                                       "sys.type"]
                                    :item-tx-fn       (fn [u]
                                                        (let [u' (if production?
                                                                   (dissoc u :acceptance-looks)
                                                                   (set/rename-keys u {:acceptance-looks :looks}))]
                                                          (utils/?update u' :looks (partial remove :sys))))
                                    :collection-tx-fn (fn [m]
                                                        (->> (vals m)
                                                             (mapcat :looks)
                                                             (maps/index-by (comp keyword :content/id))
                                                             (assoc m :all-looks)))
                                    :latest?          false}]
          timestamps              (increased-polling-intervals (date/now))]
      (doseq [content-params content-type-parameters
              :let           [num-checks (count timestamps)]
              [i ts-millis]  (map-indexed vector timestamps)]
        (at-at/at ts-millis #(do-fetch-entries (assoc c :cache cache :env-param env-param) content-params)
                  pool
                  :desc (str "setting up " i " of " num-checks " for " (:content-type content-params))))
      (doseq [content-params content-type-parameters]
        (at-at/interspaced cache-timeout
                           #(do-fetch-entries (assoc c :cache cache :env-param env-param) content-params)
                           pool
                           :desc (str "poller for " (:content-type content-params))))
      (println "Pool polling status at start: " (at-at/show-schedule pool))
      (assoc c
             :pool  pool
             :cache cache)))
  (stop [c]
    (when (:pool c) (at-at/stop-and-reset-pool! (:pool c)))
    (dissoc c :cache :pool))
  CMSCache
  (read-cache [c] (deref (:cache c))))

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
