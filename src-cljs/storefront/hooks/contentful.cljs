(ns storefront.hooks.contentful
  (:require [ajax.core :refer [GET json-response-format]]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.platform.messages :as m]))

(def default-error-handler (partial m/handle-message events/api-failure-bad-server-response))

(defn ^:private api-request [path {:keys [params handler error-handler]}]
  (GET (str "https://cdn.contentful.com/spaces/" (:content-model config/contentful) "/entries" path)
       {:params          (merge {:access-token (:api-key config/contentful)}
                                params)
        :response-format (json-response-format {:keywords? true})
        :handler         handler
        :error-handler   (or error-handler default-error-handler)}))

(defn fetch-homepage []
  (api-request (str "/" (:homepage-id config/contentful))
               {:handler (fn [resp]
                           (let [fields (:fields resp)]
                             (m/handle-message events/contentful-api-success-fetch-homepage
                                               {:homepage {:hero {:desktop-uuid (:heroImageDesktopUuid fields)
                                                                  :mobile-uuid  (:heroImageMobileUuid fields)
                                                                  :file-name    (:heroImageFileName fields)
                                                                  :alt-text     (:heroImageAltText fields)}}})))}))
