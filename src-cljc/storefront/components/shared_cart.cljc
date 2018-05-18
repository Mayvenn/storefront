(ns storefront.components.shared-cart
  (:require [storefront.accessors.promos :as promos]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [shared-cart-id store fetching-products? creating-cart? advertised-promo]} owner opts]
  (component/create
   (let [{:keys [portrait store-nickname]} store]
     [:div.container.p4
      [:div.pb3
       (when (:resizable-url portrait)
         [:div.mb2.h2
          (ui/circle-picture {:class "mx-auto"} (ui/square-image portrait 96))])
       [:p.center.h3.navy.medium
        store-nickname " has created a bag for you!"]]
      [:div.flex.items-center.px1.py3.border-dark-gray.border-top.border-bottom
       svg/guarantee
       [:div.ml2.flex-auto
        [:p.medium.navy.shout.mb2 "Free shipping & 30 day guarantee"]
        [:p.dark-gray
         "Shop with confidence: Wear it, dye it, even color it. "
         "If you do not love your Mayvenn hair we will exchange it within 30 days of purchase!"]]]
      [:div.p3.h4.center
       (or (:description advertised-promo) promos/bundle-discount-description)]
      [:form
       {:on-submit (utils/send-event-callback events/control-create-order-from-shared-cart {:shared-cart-id shared-cart-id})}
       (ui/submit-button "View your bag"
                         {:data-test "create-order-from-shared-cart"
                          :spinning? (or fetching-products?
                                         creating-cart?)
                          :disabled? (or fetching-products?
                                         creating-cart?)})]])))

(defn query [data]
  {:shared-cart-id     (get-in data keypaths/shared-cart-id)
   :store              (get-in data keypaths/store)
   :advertised-promo   (promos/default-advertised-promotion (get-in data keypaths/promotions))
   :fetching-products? (utils/requesting? data (conj request-keys/search-v2-products {}))
   :creating-cart?     (utils/requesting? data request-keys/create-order-from-shared-cart)})

(defn built-component [data opts]
  (component/build component (query data) opts))
