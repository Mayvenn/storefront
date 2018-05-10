(ns storefront.hooks.pixlee
  (:require [ajax.core :refer [GET json-response-format]]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.platform.messages :as m]))

(def default-error-handler (partial m/handle-message events/api-failure-bad-server-response))

(defn ^:private api-request [path {:keys [params handler error-handler]}]
  (GET (str "https://distillery.pixlee.com/api/v2" path)
      {:params          (merge {:api_key (:api-key config/pixlee)}
                               params)
       :response-format (json-response-format {:keywords? true})
       :handler         handler
       :error-handler   (or error-handler default-error-handler)}))

(defn fetch-album [album-id album-keyword]
  (api-request (str "/albums/" album-id "/photos")
               {:params  {:per_page 48}
                :handler (fn [resp]
                           (m/handle-message events/pixlee-api-success-fetch-album
                                             {:album-data (:data resp)
                                              :album-keyword album-keyword}))}))

(defn fetch-album-by-keyword [album-keyword]
  (fetch-album (-> config/pixlee :albums album-keyword) album-keyword))

(defn fetch-image [album-keyword image-id]
  (api-request (str "/media/" image-id)
               {:handler (fn [resp]
                           (m/handle-message events/pixlee-api-success-fetch-image
                                             {:image-data (:data resp)
                                              :album-keyword album-keyword}))
                :error-handler (fn [resp]
                                 (m/handle-message events/pixlee-api-failure-fetch-album resp))}))
