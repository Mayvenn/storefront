(ns storefront.components.stylist-banner
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]))

(def close-button
  (component/html
   (ui/modal-close {:bg-class "fill-dark-gray" :on-close (utils/send-event-callback events/control-stylist-banner-close)})))

(def banner-copy
  (component/html [:p.f4.light.letter-spacing-1 [:span.medium "Are you a stylist?"] " Grow your business & earn extra money by joining Mayvenn!"]))

(defn component [{:keys [show? welcome-url]} owner opts]
  (component/create
   (when show?
     (let [btn-behavior {:href     welcome-url
                         :on-click (utils/send-event-callback events/external-redirect-welcome)}
           btn-copy "Become a Mayvenn"]
       [:div.bg-dark-black.white.p2
        ;; Mobile layout
        [:div.md-up-hide
         [:div.right close-button]
         [:div.center
          [:div.col-9.mx-auto.mb2 banner-copy]
          [:div.col-7.mx-auto (ui/green-button btn-behavior btn-copy)]]]
        ;; Desktop / Tablet layout
        [:div.flex.items-center.to-md-hide
         [:div.flex-auto
          [:div.col-7.mx-auto.center.flex.items-center
           [:div.col-7.mr2 banner-copy]
           [:div.col-5 (ui/banner-green-button btn-behavior btn-copy)]]]
         close-button]]))))

(defn query [data]
  {:show?       (and (experiments/stylist-banner? data)
                     (= (get-in data keypaths/navigation-event) events/navigate-home)
                     (= (get-in data keypaths/store-slug) "shop")
                     (not (get-in data keypaths/stylist-banner-hidden)))
   :welcome-url (get-in data keypaths/welcome-url)})
