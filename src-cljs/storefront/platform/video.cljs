(ns storefront.platform.video
  (:require [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn component [{:keys [video-id]} owner opts]
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
       [:div.wistia_responsive_padding.relative
        {:style {:padding-top "150%"}}
        [:div.wistia_responsive_wrapper.absolute.left-0.top-0.col-12 {:style {:height "100%"}}

         [:span.wistia_embed.inline-block.col-12 {:class (str "wistia_async_" video-id " popover=true popoverAnimateThumbnail=true videoFoam=true popoverOverlayOpacity=0.9")
                                                  :style {:height "100%"}}
          ui/nbsp]]]))))
