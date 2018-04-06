(ns storefront.system.contentful
  (:require [overtone.at-at :as at-at]
            [tugboat.core :as tugboat]
            [com.stuartsierra.component :as component]
            [spice.core :as spice]
            [spice.maps :as maps]
            [clojure.set :as set]))

(defn contentful-request [{:keys [endpoint api-key] :as contentful} method path]
  (tugboat/request {:endpoint endpoint}
                   method path
                   (assoc {:socket-timeout 30000
                           :conn-timeout   30000
                           :as             :json}
                          :query-params
                          {:access_token api-key})))

(defn find-referenced-asset [assets value]
  (if (-> value :sys :linkType (= "Asset"))
    (:fields (get assets (-> value :sys :id)))
    value))

(defn unbox [{:keys [fields sys]}]
  (merge (maps/kebabify fields)
         {:content/updated-at (spice.date/to-millis (:updatedAt sys 0))
          :content/type       (or
                               (-> sys :contentType :sys :id)
                               "Asset")
          :content/id         (-> sys :id)}))

(defn unbox-all [index-key boxed-resources]
  (->> (map unbox boxed-resources)
       (group-by index-key)
       (maps/map-values (partial apply max-key :content/updated-at))))


(defn resolve-link [resources [key value]]
  [key (if (map? value)
         (let [{:as image :keys [link-type id]} (:sys value)]
           (if-let [resolved-value (get (get resources "Asset") (:id image))]
             resolved-value
             value))
         value)])

(defn ->content [body]
  (let [resources {:items (unbox-all :content/type
                                     (:items body))
                   :Asset (unbox-all :content/id
                                     (some-> body :includes :Asset))}]
    (maps/map-values (fn [v] (into {} (map (partial resolve-link resources)) v))
                     (:items resources))))

(defn fetch-entries
  ([contentful]
   (fetch-entries contentful 1))
  ([{:keys [cache space-id] :as contentful} attempt-number]
   (when (<= attempt-number 2)
     (let [{:keys [status body]}
           (contentful-request contentful :get (str "/spaces/" space-id "/entries"))]
       (if (<= 200 status 299)
         (reset! cache (->content body))
         (fetch-entries contentful (inc attempt-number)))))))

#_(let [{:keys [cache space-id] :as contentful} (:contentful dev-system/the-system)]
  (->content
   (:body
    (contentful-request contentful :get (str "/spaces/" space-id "/entries")))))

#_ (fetch-entries (:contentful dev-system/the-system))


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
