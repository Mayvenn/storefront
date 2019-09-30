(ns storefront.components.image-picker
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.assets :as assets]
            [storefront.platform.component-utils :as utils]
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
        [:div.p2
         [:a.dark-gray.block.mb2
          (utils/route-to (:navigation-event back-link))
          (ui/ucare-img {:class "px1 mbnp4"
                         :style {:height  "1.25rem"
                                 :display "inline"}} "942d4023-ae6b-4228-8394-00293229e895")
          (:back-copy back-link)]
         [:h1.center "Select a source below"]]
        [:div {:id selector} ui/nbsp]]))))
