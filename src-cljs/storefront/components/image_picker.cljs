(ns storefront.components.image-picker
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]))

(defn component [{:keys [selector widget-config on-success resizable-url back-link]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_]
      (handle-message events/image-picker-component-mounted
                      {:selector      (str "#" selector)
                       :resizable-url resizable-url
                       :on-success    on-success
                       :widget-config widget-config}))
    om/IWillUnmount
    (will-unmount [_]
      (handle-message events/image-picker-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div.container.sans-serif
        [:div.p2 back-link
         [:h1.center "Select a source below"]]
        [:div {:id selector} ui/nbsp]]))))
