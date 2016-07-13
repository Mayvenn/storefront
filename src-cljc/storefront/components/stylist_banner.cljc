(ns storefront.components.stylist-banner
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]))

(defn component [{:keys [stylist-banner? navigation-event store-slug stylist-banner-hidden? welcome-url]} owner opts]
  (component/create
   (when (and stylist-banner?
              (= navigation-event events/navigate-home)
              (= store-slug "shop")
              (not stylist-banner-hidden?))
     [:div.bg-dark-black.white.col-12.p2.sans-serif
      ;; Mobile layout
      [:div.right.md-up-hide (ui/modal-close {:bg-class "fill-dark-gray" :on-close (utils/send-event-callback events/control-stylist-banner-close)})]
      [:div.center.md-up-hide
       [:div.col-12
        [:div.col-9.mx-auto.f4.mb2.light.letter-spacing-1 [:span.medium "Are you a stylist?"] " Grow your business & earn extra money by joining Mayvenn!"]]
       [:div.col-12
        [:div.col-7.mx-auto
         (ui/green-button {:href     welcome-url
                           :on-click (utils/send-event-callback events/external-redirect-welcome)} "Become a Mayvenn")]]]
      ;; Desktop / Tablet layout
      [:div.flex.items-center.col-12.to-md-hide
       [:div.flex-auto
        [:div.col-7.mx-auto
         [:div.flex.items-center.justify-center
          [:div.col.col-7.center
           [:div.col-11.mx-auto.f4.light.letter-spacing-1 [:span.medium "Are you a stylist?"] " Grow your business & earn extra money by joining Mayvenn!"]]
          [:div.col.col-5
           (ui/banner-green-button {:href     welcome-url
                                    :on-click (utils/send-event-callback events/external-redirect-welcome)}
                                   [:div.flex.items-center.justify-center
                                    "Become a Mayvenn"])]
          [:div.clearfix]]]]
       [:div.to-md-hide (ui/modal-close {:bg-class "fill-dark-gray" :on-close (utils/send-event-callback events/control-stylist-banner-close)})]]])))

(defn query [data]
  {:stylist-banner?        (experiments/stylist-banner? data)
   :navigation-event       (get-in data keypaths/navigation-event)
   :store-slug             (get-in data keypaths/store-slug)
   :stylist-banner-hidden? (get-in data keypaths/stylist-banner-hidden)
   :welcome-url            (get-in data keypaths/welcome-url)})
