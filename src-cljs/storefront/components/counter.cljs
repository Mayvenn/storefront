(ns storefront.components.counter
  (:require [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.messages :refer [send]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [sablono.core :refer-macros [html]]
            [om.core :as om]))

(defn counter-component [data owner {:keys [path set-event inc-event dec-event spinner-path]}]
  (om/component
   (html
    [:div.quantity
     [:div.quantity-selector
      [:div.minus [:a.pm-link
                   {:href "#"
                    :on-click (utils/send-event-callback data
                                                         dec-event
                                                         {:path path})}
                   "-"]]
      (let [updating-cart (get-in data spinner-path)]
        [:input#quantity.quantity-selector-input
         {:min 1
          :name "quantity"
          :type "text"
          :value (str (get-in data path))
          :class (when updating-cart
                   "saving")
          :on-change #(send data
                            set-event
                            {:value-str (.. % -target -value)
                             :path path})}])
      [:div.plus [:a.pm-link
                  {:href "#"
                   :on-click (utils/send-event-callback data
                                                        inc-event
                                                        {:path path})}
                  "+"]]]])))
 
