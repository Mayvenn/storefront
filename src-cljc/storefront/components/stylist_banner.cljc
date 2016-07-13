(ns storefront.components.stylist-banner

(:require #?(:clj [storefront.component-shim :as component]
             :cljs [storefront.component :as component])
          [storefront.components.ui :as ui]
          [storefront.platform.component-utils :as utils]
          [storefront.events :as events]
          [storefront.keypaths :as keypaths]))

(defn stylist-banner-component [data owner opts]
  (component/create
   (when (and (= (get-in data keypaths/navigation-event) events/navigate-home)
              (= (get-in data keypaths/store-slug) "shop")
              (not (get-in data keypaths/stylist-banner-hidden)))
     [:div.bg-dark-black.white.col-12.p2.sans-serif
      [:div.right (ui/modal-close {:bg-class "fill-dark-gray" :on-close (utils/send-event-callback events/control-stylist-banner-close)})]
      [:div.col-12.mx-auto.center
       [:div.h2 "Are you a stylist?"]
       [:div.h5.py1 "Grow your business & earn extra money by joining Mayvenn!"]
       [:div.col-6.mx-auto
        (ui/button "Become a Mayvenn" {:href (get-in data keypaths/welcome-url)
                                       :on-click (utils/send-event-callback events/external-redirect-welcome)})]]])))
