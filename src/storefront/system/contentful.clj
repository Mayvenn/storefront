(ns storefront.system.contentful
  (:require [overtone.at-at :as at-at]
            [tugboat.core :as tugboat]
            [com.stuartsierra.component :as component]
            [spice.core :as spice]
            [spice.maps :as maps]
            [clojure.set :as set]))

(defn contentful-request
  [{:keys [endpoint api-key] :as contentful} method path]
  (tugboat/request {:endpoint endpoint}
                   method path
                   (assoc {:socket-timeout 30000
                           :conn-timeout   30000
                           :as             :json}
                          :query-params
                          {:access_token api-key})))

(defn extract-fields
  "Contentful resources are boxed.

  We extract the fields, merging useful meta-data."
  [{:keys [fields sys]}]
  (merge (maps/kebabify fields)
         {:content/updated-at (spice.date/to-millis (:updatedAt sys 0))
          :content/type       (or
                               (-> sys :contentType :sys :id)
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
      (update-in [:includes :Asset]
                 extract-latest-by :content/id)))

(defn resolve-link
  "Resolves contentful links in field values by stitching included
  resources in situ."
  [includes [key value]]
  [key (if (map? value)
         (get (:Asset includes) (some-> value :sys :id) value)
         value)])

(defn resolve-items
  [{:keys [items includes]}]
  (maps/map-values (fn [item]
                     (into {}
                           (map (partial resolve-link includes))
                           item))
                   items))

(defn fetch-entries
  ([contentful]
   (fetch-entries contentful 1))
  ([{:keys [cache space-id] :as contentful} attempt-number]
   (when (<= attempt-number 2)
     (let [{:keys [status body]}
           (contentful-request contentful :get (str "/spaces/" space-id "/entries"))]
       (if (<= 200 status 299)
         (reset! cache (some-> body extract resolve-items))
         (fetch-entries contentful (inc attempt-number)))))))

(defrecord ContentfulContext
    [logger exception-handler cache-timeout api-key space-id endpoint]
  component/Lifecycle
  (start [c]
    (let [pool  (at-at/mk-pool)
          cache (atom {})]
      (at-at/every cache-timeout
                   #(fetch-entries {:space-id space-id
                                    :endpoint endpoint
                                    :cache    cache
                                    :api-key  api-key})
                  pool)
      (assoc c :pool  pool
               :cache cache)))
  (stop [c]
    (when (:pool c) (at-at/stop-and-reset-pool! (:pool c)))
    (dissoc c :cache :pool)))

;; TODO filter by space
