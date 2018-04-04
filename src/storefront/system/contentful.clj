(ns storefront.system.contentful
  (:require [overtone.at-at :as at-at]
            [tugboat.core :as tugboat]
            [com.stuartsierra.component :as component]))

(defn contentful-request [{:keys [endpoint api-key] :as contentful} method path]
  (tugboat/request {:endpoint endpoint}
                   method path
                   (assoc {:socket-timeout 30000
                           :conn-timeout   30000
                           :as             :json}
                          :query-params
                          {:access_token api-key})))

(def entry->content-type-id (comp :id :sys :contentType :sys))

(defn entry->fields [entry]
  (let [fields (:fields entry)]
    [(entry->content-type-id entry)
     (merge fields
            {:updated-at (-> entry :sys :updatedAt)
             :entry-id   (-> entry :sys :id)})]))

;; Rule is to use the most recently updated 'published' entry of a specific type
(defn tx-contentful-body [body]
  (let [contentful-key->data (->> (:items body)
                                  (sort-by (comp :updatedAt :sys))
                                  reverse
                                  (partition-by entry->content-type-id)
                                  (map first)
                                  (map entry->fields)
                                  (into {}))
        homepage-hero-data   (get contentful-key->data "homepage")]
    {:hero {:desktop-uuid (:heroImageDesktopUuid homepage-hero-data)
            :mobile-uuid  (:heroImageMobileUuid homepage-hero-data)
            :file-name    (:heroImageFileName homepage-hero-data)
            :alt-text     (:heroImageAltText homepage-hero-data)}}))

(defn fetch-entries
  ([contentful]
   (fetch-entries contentful 1))
  ([{:keys [cache space-id] :as contentful} attempt-number]
   (when (<= attempt-number 2)
     (let [{:keys [status body]}
           (contentful-request contentful :get (str "/spaces/" space-id "/entries"))]
       (if (<= 200 status 299)
         (reset! cache {:transformed-response (tx-contentful-body body)})
         (fetch-entries contentful (inc attempt-number)))))))

(defrecord ContentfulContext
    [logger exception-handler cache-timeout api-key space-id endpoint]
  component/Lifecycle
  (start [c]
    (let [pool  (at-at/mk-pool)
          cache (atom {:transformed-response nil})]
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
