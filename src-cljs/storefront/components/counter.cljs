(ns storefront.components.counter
  (:require [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [sablono.core :refer-macros [html]]
            [om.core :as om]))

(defn counter-component [data owner {:keys [path set-event inc-event dec-event spinner-key]}]
  (let [request (when spinner-key (query/get {:request-key spinner-key}
                                             (get-in data keypaths/api-requests)))]
    (om/component
     (html
      [:div.quantity
       [:div.quantity-selector
        [:div.minus
         [:a.pm-link
          {:href "#"
           :disabled request
           :on-click (if (not request)
                       (utils/send-event-callback dec-event {:path path})
                       utils/noop-callback)}
          "-"]]
        [:input#quantity.quantity-selector-input
         {:min 1
          :name "quantity"
          :type "text"
          :disabled (or (nil? set-event) request)
          :class (when request "saving")
          :value (str (get-in data path))
          :on-change (if (not request)
                       #(handle-message set-event
                                        {:value-str (.. % -target -value) :path path})
                       utils/noop-callback)}]
        [:div.plus
         [:a.pm-link
          {:href "#"
           :disabled request
           :on-click (if (not request)
                       (utils/send-event-callback inc-event {:path path})
                       utils/noop-callback)}
          "+"]]]]))))
