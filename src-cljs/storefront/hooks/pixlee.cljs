(ns storefront.hooks.pixlee
  (:require [ajax.core :refer [GET json-response-format]]
            [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.platform.messages :as m]
            [clojure.string :as str]))

(defn ^:private api-request [path {:keys [params handler]}]
  (GET (str "https://distillery.pixlee.com/api/v2" path)
      {:params          (merge {:api_key (:api-key config/pixlee)}
                               params)
       :response-format (json-response-format {:keywords? true})
       :handler         handler}))

(defn fetch-album-photos [album-id {:keys [params handler]}]
  (api-request (str "/albums/" album-id "/photos")
               {:params  params
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
