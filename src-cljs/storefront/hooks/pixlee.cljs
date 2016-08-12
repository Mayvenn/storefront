(ns storefront.hooks.pixlee
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.platform.messages :as m]))

(defn js-loaded? [] (.hasOwnProperty js/window "Pixlee"))

(defn insert []
  (when-not (js-loaded?)
    (tags/insert-tag-with-callback
     (tags/src-tag "//assets.pixlee.com/assets/pixlee_widget_1_0_0.js"
                   "ugc")
     (fn []
       (js/Pixlee.init (clj->js {:apiKey "PUTXr6XBGuAhWqoIP4ir"}))
       (m/handle-message events/inserted-pixlee)))))

(defn attach [container-id sku]
  (when (and (js-loaded?) sku)
    (js/Pixlee.addProductWidget
     (clj->js
      {:containerId       container-id
       :skuId             sku
       :addToCart         false
       :addToCartNavigate "false"
       :recipeId          476
       :displayOptionsId  14046
       :type              "horizontal"
       :accountId         1009}))))
