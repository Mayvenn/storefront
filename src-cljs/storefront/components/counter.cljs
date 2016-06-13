(ns storefront.components.counter
  (:require [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [sablono.core :refer-macros [html]]
            [om.core :as om]))

;; TODO CLEANUP Remove this after product-page-redesign? experiment variant wins
(defn counter-component [data owner {:keys [path set-event inc-event dec-event spinner-key]}]
  (let [saving? (when spinner-key (utils/requesting? data spinner-key))]
    (om/component
     (html
      [:div.quantity
       [:div.quantity-selector
        [:div.minus
         [:a.pm-link
          {:href "#"
           :disabled saving?
           :on-click (if saving?
                       utils/noop-callback
                       (utils/send-event-callback dec-event {:path path}))}
          "-"]]
        [:input#quantity.quantity-selector-input
         {:min 1
          :name "quantity"
          :type "text"
          :disabled (or (nil? set-event) saving?)
          :class (when saving? "saving")
          :value (str (get-in data path))
          :on-change (if saving?
                       utils/noop-callback
                       #(handle-message set-event
                                        {:value-str (.. % -target -value) :path path}))}]
        [:div.plus
         [:a.pm-link
          {:href "#"
           :disabled saving?
           :on-click (if saving?
                       utils/noop-callback
                       (utils/send-event-callback inc-event {:path path}))}
          "+"]]]]))))
