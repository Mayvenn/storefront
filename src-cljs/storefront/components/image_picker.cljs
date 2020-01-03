(ns storefront.components.image-picker
  (:require [storefront.components.ui :as ui]
            [storefront.component :as component :refer [defdynamic-component]]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            ui.molecules))

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
               (ui.molecules/return-link
                {:return-link/id            "back to gallery"
                 :return-link/copy          "Back to Gallery"
                 :return-link/event-message [(:navigation-event back-link)]})]
              [:div.bg-warm-gray.border-top.border-gray
               [:div.mt8.mb1.title-1.canela.center "Select a source"]
               [:div.mb3.col-8.mx-auto.content-2.proxima.center "Click an icon below to select your photo source"]
               [:div {:id selector} ui/nbsp]]]))))
