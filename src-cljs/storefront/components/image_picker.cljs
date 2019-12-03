(ns storefront.components.image-picker
  (:require [storefront.components.ui :as ui]
            [storefront.component :as component :refer [defdynamic-component]]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.components.svg :as svg]))

(defdynamic-component component
  (did-mount [this]
             (let [{:keys [selector widget-config on-success resizable-url]} (component/get-props this)]
               (handle-message events/image-picker-component-mounted
                               {:selector      (str "#" selector)
                                :resizable-url resizable-url
                                :on-success    on-success
                                :widget-config widget-config})))
  (will-unmount [_]
                (handle-message events/image-picker-component-will-unmount))
  (render [this]
          (let [{:keys [selector back-link]} (component/get-props this)]
            (component/html
             [:div.container.sans-serif
              [:div.p2
               [:a.inherit-color.block.mb2.flex.items-center
                (utils/route-to (:navigation-event back-link))
                (svg/left-caret {:class  "stroke-black"
                                 :width  "1.25rem"
                                 :height "1.25rem"})
                (:back-copy back-link)]
               [:h1.center "Select a source below"]]
              [:div {:id selector} ui/nbsp]]))))
