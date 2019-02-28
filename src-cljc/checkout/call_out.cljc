(ns checkout.call-out
  (:require [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]))

;; TODO this should be a generalized call-out for the cart
;; The template should be made to be presentation focused
;;   and just focused on the call out aspect of the cart (probably no ifs)
;; The query should build data for consumption by the template

(defn v2-cart-promo
  [qualified?]
  [:div.mb3
   (if qualified?
     [:div.bg-teal.bg-celebrate.p2.white.center {:data-test "v2-cart-promo"}
      [:div.flex.justify-center.mb1 (ui/ucare-img {:width 46} "014c70a0-0d57-495d-add0-f2f46248d224")]
      [:h4 "This order qualifies for a"]
      [:h1.shout.bold "Free Install"]
      [:h4.pb6 "You'll receive a voucher via email after purchase"]]

     [:div.p2.bg-orange.white.center {:data-test "ineligible-v2-cart-promo"}
      [:h4.medium "You're almost there..."]
      [:h4.medium "Buy 3 bundles or more and get a"]
      [:h1.shout.bold "Free Install"]
      [:h6.medium
       [:div "from your Mayvenn Certified Stylist"]
       [:div "Use code " [:span.bold "FREEINSTALL"] " to get your free install."]]])])

(defn component
  [{:keys [v2-experience? show-green-banner?]}]
  (component/create
   [:div
    (when v2-experience? (v2-cart-promo show-green-banner?))]))

(defn query
  [data]
  (let [order (get-in data keypaths/order)]
    {:v2-experience?     (experiments/v2-experience? data)
     :show-green-banner? (orders/freeinstall-applied? order)}))

(defn built-component
  [data opts]
  (component/build component (query data) opts))
