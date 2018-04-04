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
   ;; TODO: this was fill-dark-gray, but got lost
   (ui/modal-close {:close-attrs (utils/fake-href events/control-stylist-banner-close)})))

(def banner-copy
  (component/html [:p.h5.light.letter-spacing-1 [:span.medium "Are you a stylist?"] " Grow your business & earn extra money by joining Mayvenn!"]))

(defn component [{:keys [welcome-url]} owner opts]
  (component/create
   (let [btn-behavior {:href     welcome-url
                       :on-click (utils/send-event-callback events/external-redirect-welcome)}
         btn-copy "Become a Mayvenn"]
     [:div.bg-dark-gray.light-gray.p2
      ;; Mobile layout
      [:div.hide-on-tb-dt
       [:div.right close-button]
       [:div.center
        [:div.col-9.mx-auto.mb2 banner-copy]
        [:div.col-9.mx-auto (ui/teal-button btn-behavior btn-copy)]]]
      ;; Desktop / Tablet layout
      [:div.flex.items-center.hide-on-mb
       [:div.flex-auto
        [:div.col-8.mx-auto.center.flex.items-center
         [:div.col-7.mr2 banner-copy]
         [:div.col-7 (ui/teal-button btn-behavior btn-copy)]]]
       close-button]])))

(defn query [data]
  {:welcome-url (get-in data keypaths/welcome-url)})

(defn built-component [data opts]
  (when (and (= (get-in data keypaths/navigation-event) events/navigate-home)
             (= (get-in data keypaths/store-slug) "shop")
             (not (get-in data keypaths/stylist-banner-hidden))
             (not (experiments/the-ville? data)))
    (component/build component (query data) opts)))
