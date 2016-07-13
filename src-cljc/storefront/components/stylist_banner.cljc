(ns storefront.components.stylist-banner
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [navigation-event store-slug stylist-banner-hidden? welcome-url]} owner opts]
  (component/create
   (when (and (= navigation-event events/navigate-home)
              (= store-slug "shop")
              (not stylist-banner-hidden?))
     [:div.bg-dark-black.white.col-12.p2.sans-serif
      [:div.right (ui/modal-close {:bg-class "fill-dark-gray" :on-close (utils/send-event-callback events/control-stylist-banner-close)})]
      [:div.col-12.mx-auto.center
       [:div.h2 "Are you a stylist?"]
       [:div.h5.py1 "Grow your business & earn extra money by joining Mayvenn!"]
       [:div.col-6.mx-auto
        (ui/button "Become a Mayvenn" {:href welcome-url
                                       :on-click (utils/send-event-callback events/external-redirect-welcome)})]]])))

(defn query [data]
  {:navigation-event       (get-in data keypaths/navigation-event)
   :store-slug             (get-in data keypaths/store-slug)
   :stylist-banner-hidden? (get-in data keypaths/stylist-banner-hidden)
   :welcome-url            (get-in data keypaths/welcome-url)})
