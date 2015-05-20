(ns storefront.components.counter
  (:require [storefront.events :as events]
            [storefront.components.utils :as utils]
            [sablono.core :refer-macros [html]]
            [om.core :as om]))

(defn counter-component [data owner {:keys [path]}]
  (om/component
   (html
    [:div.quantity
     [:div.quantity-selector
      [:div.minus [:a.pm-link
                   {:href "#"
                    :on-click (utils/enqueue-event data
                                                   events/control-counter-dec
                                                   {:path path})}
                   "-"]]
      [:input#quantity.quantity-selector-input
       {:min 1
         :name "quantity"
         :type "text"
         :value (str (get-in data path))
         :on-change #(utils/put-event data
                                      events/control-counter-set
                                      {:value-str (.. % -target -value)
                                       :path path})}]
      [:div.plus [:a.pm-link
                  {:href "#"
                   :on-click (utils/enqueue-event data
                                                  events/control-counter-inc
                                                  {:path path})}
                  "+"]]]])))
