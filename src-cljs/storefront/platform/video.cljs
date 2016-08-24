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
       (letfn [(cancel-close [e] (.stopPropagation e) false)
               (on-click-or-touch [f] {:on-click       f
                                       :on-touch-start f})]
         ;; TODO: if ui/modal is ever refactored, copy changes here
         [:div
          [:div.fixed.overlay.bg-darken-4.z3
           (on-click-or-touch on-close)
           [:div.fixed.overlay.bg-darken-4]]
          [:div.fixed.z3.left-0.right-0.mx-auto.overflow-auto.col-11
           {:style        {:max-height "100%"}
            :data-snap-to "top"}
           [:div.flex.flex-column.items-center.justify-center
            (merge (on-click-or-touch on-close)
                   {:style {:height "100vh"}})
            [:div.wistia_responsive_padding.relative.col-12
             {:style {:padding-top "56.25%"}}
             [:div.wistia_responsive_wrapper.absolute.left-0.top-0.col-12
              (merge (on-click-or-touch cancel-close)
                     {:style {:height "100%"}})
              [:span.wistia_embed.col-12
               {:class (str "wistia_async_" video-id " videoFoam=true autoPlay=true volume=0.33")
                :style {:height "100%"}}
               ui/nbsp]]]
            [:div.white.p1.mt3
             (on-click-or-touch on-close)
             "close video"]]]])))))
