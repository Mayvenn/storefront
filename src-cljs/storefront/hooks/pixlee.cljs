(ns storefront.hooks.pixlee
  (:require [ajax.core :refer [GET json-response-format]]
            [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.platform.messages :as m])
  (:import goog.json.Serializer))

(defn widget-js-loaded? [] (.hasOwnProperty js/window "Pixlee"))
(defn analytics-js-loaded? [] (.hasOwnProperty js/window "Pixlee_Analytics"))

(defn insert []
  (when-not (widget-js-loaded?)
    (tags/insert-tag-with-callback
     (tags/src-tag "//assets.pixlee.com/assets/pixlee_widget_1_0_0.js"
                   "ugc")
     (fn []
       (js/Pixlee.init (clj->js {:apiKey (:api-key config/pixlee)}))
       (m/handle-message events/inserted-pixlee))))
  (when-not (analytics-js-loaded?)
    (tags/insert-tag-with-src
     "//assets.pixlee.com/assets/pixlee_events.js"
     "ugc-analytics")))

(defn write-json [data]
  (.serialize (goog.json.Serializer.) (clj->js data)))

(defn fetch-mosaic []
  (GET (str "http://distillery.pixlee.com/api/v2/albums/" (-> config/pixlee :mosaic :albumId) "/photos")
      {:params          {:api_key  (:api-key config/pixlee)
                         :per_page 48
                         :filters  (write-json {:content_type ["image"]})}
       :response-format (json-response-format {:keywords? true})
       :handler         (partial m/handle-message events/pixlee-api-success-fetch-mosaic)}))

(defn attach-product-widget [container-id sku]
  (when (and (widget-js-loaded?) sku)
    (js/Pixlee.addProductWidget
     (clj->js (merge
               (:product config/pixlee)
               {:containerId       container-id
                :skuId             sku
                :type              "horizontal"
                :addToCart         false
                :addToCartNavigate "false"
                :recipeId          476
                :accountId         (:account-id config/pixlee)})))
    (.setTimeout js/window js/Pixlee.resizeWidget 0)))

(defn close-all []
  (when (widget-js-loaded?)
    (js/Pixlee.close true)))

(defn track-event [event-name args]
  (when (analytics-js-loaded?)
    (.trigger (.-events (js/Pixlee_Analytics. (:api-key config/pixlee)))
              event-name
              (clj->js args))))
