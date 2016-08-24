(ns storefront.platform.video
  (:require [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn component [{:keys [video-id]} owner {:keys [on-close]}]
  (reify
    om/IDidMount
    (did-mount [this]
      (handle-message events/video-component-mounted {:video-id video-id}))

    om/IWillUnmount
    (will-unmount [this]
      (handle-message events/video-component-unmounted {:video-id video-id}))

    om/IRender
    (render [this]
      (html
       (ui/modal
        {:on-close on-close :bg-class "bg-darken-4"}
        [:div.flex.items-center
         {:style {:height "100vh"}}
         [:div.wistia_responsive_padding.relative.col-12
          {:style {:padding-top "56.25%"}}
          [:div.wistia_responsive_wrapper.absolute.left-0.top-0.col-12
           {:style {:height "100%"}}
           [:span.wistia_embed.col-12
            {:class (str "wistia_async_" video-id " videoFoam=true autoPlay=true volume=0.33")
             :style {:height "100%"}}
            ui/nbsp]]]])))))
