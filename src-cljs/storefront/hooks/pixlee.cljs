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

(defn fetch-album [album-id album-name]
  (api-request (str "/albums/" album-id "/photos")
               {:params  {:per_page 48}
                :handler (fn [resp]
                           (m/handle-message events/pixlee-api-success-fetch-album
                                             {:album-data (:data resp)
                                              :album-name album-name}))}))

(defn fetch-mosaic []
  (fetch-album (-> config/pixlee :albums :mosaic) :mosaic))

(defn fetch-named-search-album-ids []
  (api-request "/products"
               {:params  {:per_page 100}
                :handler (partial m/handle-message events/pixlee-api-success-fetch-named-search-album-ids)}))

(defn fetch-image [image-id]
  (api-request (str "/media/" image-id)
               {:handler (fn [resp]
                           (m/handle-message events/pixlee-api-success-fetch-image
                                             {:image-data (:data resp)}))}))
