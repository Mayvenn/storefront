(ns storefront.hooks.pixlee
  (:require [ajax.core :refer [GET json-response-format]]
            [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.platform.messages :as m]
            [clojure.string :as str])
  (:import goog.json.Serializer))

(defn analytics-js-loaded? [] (.hasOwnProperty js/window "Pixlee_Analytics"))

(defn insert []
  (when-not (analytics-js-loaded?)
    (tags/insert-tag-with-src
     "//assets.pixlee.com/assets/pixlee_events.js"
     "ugc-analytics")))

(defn write-json [data]
  (.serialize (goog.json.Serializer.) (clj->js data)))

(defn ^:private api-request [path {:keys [params handler]}]
  (GET (str "https://distillery.pixlee.com/api/v2" path)
      {:params          (merge {:api_key (:api-key config/pixlee)}
                               params)
       :response-format (json-response-format {:keywords? true})
       :handler         handler}))

(defn fetch-album-photos [album-id {:keys [params handler]}]
  (api-request (str "/albums/" album-id "/photos")
               ;; Only fetch images until we handle videos https://www.pivotaltracker.com/story/show/131462137
               {:params  (merge {:filters (write-json {:content_type ["image"]})}
                                params)
                :handler handler}))

(defn fetch-mosaic []
  (fetch-album-photos (-> config/pixlee :mosaic :albumId)
                      {:params  {:per_page 48}
                       :handler (partial m/handle-message events/pixlee-api-success-fetch-mosaic)}))

(defn fetch-named-search-album [named-search-slug album-id]
  (fetch-album-photos album-id
                      {:params  {:per_page 24}
                       :handler (fn [resp]
                                  (m/handle-message events/pixlee-api-success-fetch-named-search-album
                                                    {:album-data (:data resp)
                                                     :named-search-slug named-search-slug}))}))

(defn fetch-named-search-album-ids []
  (api-request "/products"
               {:params  {:per_page 100}
                :handler (partial m/handle-message events/pixlee-api-success-fetch-named-search-album-ids)}))

(defn track-event [event-name args]
  (when (analytics-js-loaded?)
    (.trigger (.-events (js/Pixlee_Analytics. (:api-key config/pixlee)))
              event-name
              (clj->js args))))
