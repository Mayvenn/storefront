(ns storefront.system.contentful
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [overtone.at-at :as at-at]
            [spice.core :as spice]
            [spice.date :as date]
            [spice.maps :as maps]
            [tugboat.core :as tugboat]))

(defn contentful-request
  "Wrapper for the Content Delivery endpoint"
  [{:keys [endpoint env-param api-key space-id]} params]
  (let [base-params {:access_token                     api-key
                     :order                            (str "-fields." env-param)
                     (str "fields." env-param "[lte]") (date/to-iso (date/now)) }]
    (try
      (tugboat/request {:endpoint endpoint}
                       :get (str "/spaces/" space-id "/entries")
                       {:socket-timeout 30000
                        :conn-timeout   30000
                        :as             :json
                        :query-params   (merge base-params params)})
      (catch java.io.IOException ioe
        nil))))

(defn extract-fields
  "Contentful resources are boxed.

  We extract the fields, merging useful meta-data."
  [{:keys [fields sys]}]
  (merge (cske/transform-keys csk/->kebab-case fields)
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

(defn resolve-link
  "Resolves contentful links in field values by stitching included
  resources in situ."
  [includes [key value]]
  [key (if (map? value)
         (let [id        (some-> value :sys :id)
               link-type (some-> value :sys :link-type keyword)]
           (get-in includes [link-type id] value))
         value)])

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

(defn do-fetch-entries
  ([contentful content-type]
   (do-fetch-entries contentful content-type 1))
  ([{:keys [logger exception-handler cache space-id] :as contentful} content-type attempt-number]
   (try
     (when (<= attempt-number 2)
       (let [{:keys [status body]} (contentful-request contentful
                                                       {"content_type" (name content-type)
                                                        "limit"        1})]
         (if (and status (<= 200 status 299))
           (swap! cache merge (some-> body extract resolve-all clojure.walk/keywordize-keys (select-keys [content-type])))
           (do-fetch-entries contentful content-type (inc attempt-number)))))
     (catch Throwable t
       ;; Ideally, we should never get here, but at-at halts all polls that throw exceptions silently.
       ;; This simply reports it and lets the polling continue
       (exception-handler t)
       (logger :error t)))))

(defprotocol CMSCache
  (read-cache [_] "Returns a map representing the CMS cache"))

(defrecord ContentfulContext [logger exception-handler environment cache-timeout api-key space-id endpoint]
  component/Lifecycle
  (start [c]
    (let [pool          (at-at/mk-pool)
          cache         (atom {})
          config        {:space-id  space-id
                         :endpoint  endpoint
                         :env-param (if (= environment "production")
                                      "production"
                                      "acceptance")
                         :cache     cache
                         :api-key   api-key}]
      (doseq [content-type [:homepage :mayvennMadePage :advertisedPromo]]
        (at-at/every cache-timeout
                     #(do-fetch-entries config content-type)
                     pool))
      (assoc c
             :pool  pool
             :cache cache)))
  (stop [c]
    (when (:pool c) (at-at/stop-and-reset-pool! (:pool c)))
    (dissoc c :cache :pool))
  CMSCache
  (read-cache [c] (deref (:cache c))))
