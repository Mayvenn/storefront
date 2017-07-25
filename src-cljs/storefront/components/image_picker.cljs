(ns storefront.components.image-picker
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
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
          [:img.px1.mbnp4 {:style {:height "1.25rem"}
                           :src   (assets/path "/images/icons/caret-left.png")}]
          (:back-copy back-link)]
         [:h1.center "Select a source below"]]
        [:div {:id selector} ui/nbsp]]))))
