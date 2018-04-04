(ns storefront.system.contentful
  (:require [overtone.at-at :as at-at]
            [tugboat.core :as tugboat]
            [com.stuartsierra.component :as component]))

(defn contentful-request [{:keys [endpoint space-id api-key] :as contentful}]
  (tugboat/request {:endpoint endpoint }
                   :get (str "/spaces/" space-id "/entries")
                   (merge
                    {:socket-timeout 30000
                     :conn-timeout   30000
                     :as             :json
                     :query-params   {:access_token api-key}})) )

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

(defn contentful-fetch [{:keys [cache] :as contentful}]
  (let [{:keys [status body]} (contentful-request contentful)]
    (when (<= 200 status 299) ;; What if this fails?
      (reset! cache {:transformed-response (tx-contentful-body body)}))))

(defrecord ContentfulContext
    [logger exception-handler cache-timeout api-key space-id endpoint]
  component/Lifecycle
  (start [c]
    (let [pool  (at-at/mk-pool)
          cache (atom {:transformed-response nil})]
      (at-at/every cache-timeout
                   (partial contentful-fetch {:space-id space-id
                                              :endpoint endpoint
                                              :cache    cache
                                              :api-key  api-key})
                   pool)
      (assoc c :pool  pool
               :cache cache)))
  (stop [c]
    (when (:pool c) (at-at/stop-and-reset-pool! (:pool c)))
    (dissoc c :cache :pool)))
