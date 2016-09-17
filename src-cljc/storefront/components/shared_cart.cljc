(ns storefront.components.shared-cart
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.accessors.promos :as promos]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [store creating-cart? advertised-promo]} owner opts]
  (component/create
   (let [{:keys [profile_picture_url store_nickname]} store]
     (ui/container
      [:div.sm-up-col-10.md-up-col-8.mx-auto.p2
       [:div.pb3
        (when profile_picture_url
          [:div.mb2.h1
           (ui/circle-picture {:class "mx-auto"} profile_picture_url)])
        [:p.center.h2.navy.medium
         store_nickname " has created a bag for you!"]]
       [:div.flex.items-center.px1.py3.border-light-gray.border-top.border-bottom
        (svg/guarantee {:height "5em" :width "100%"})
        [:div.flex-auto
         [:p.medium.navy.shout.mb2 "Free shipping & 30 day guarantee"]
         [:p.dark-gray
          "Shop with confidence: Wear it, dye it, even color it. "
          "If you do not love your Mayvenn hair we will exchange it within 30 days of purchase!"]]]
       [:div.p3.h3.center.line-height-3
        (or (:description advertised-promo) promos/bundle-discount-description)]
       [:form
        {:on-submit (utils/send-event-callback events/control-create-order-from-shared-cart)}
        (ui/submit-button "View your bag"
                          {:data-test "create-order-from-shared-cart"
                           :spinning? creating-cart?})]]))))

(defn query [data]
  {:store            (get-in data keypaths/store)
   :advertised-promo (promos/default-advertised-promotion (get-in data keypaths/promotions))
   :creating-cart?   (utils/requesting? data request-keys/create-order-from-shared-cart)})

(defn built-component [data opts]
  (component/build component (query data) opts))
