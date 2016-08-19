(ns storefront.hooks.pixlee
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.platform.messages :as m]))

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

(defn attach [container-id sku]
  (when (and (widget-js-loaded?) sku)
    (js/Pixlee.addProductWidget
     (clj->js
      {:containerId       container-id
       :skuId             sku
       :addToCart         false
       :addToCartNavigate "false"
       :recipeId          476
       :displayOptionsId  14046
       :type              "horizontal"
       :accountId         (:account-id config/pixlee)})
     (.setTimeout js/window js/Pixlee.resizeWidget 0))))

(defn close-all []
  (when (widget-js-loaded?)
    (js/Pixlee.close)))

(defn track-event [event-name args]
  (when (analytics-js-loaded?)
    (.trigger (.-events (js/Pixlee_Analytics. (:api-key config/pixlee)))
              event-name
              (clj->js args))))
