(ns checkout.call-out
  (:require [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]))

(defn seventy-five-off-install-cart-promo
  [qualified?]
  [:div.mb3
   (if qualified?
     [:div.bg-teal.bg-celebrate.p2.white.center {:data-test "seventy-five-off-install-cart-promo"}
      [:div.flex.justify-center.mb1 (ui/ucare-img {:width 46} "014c70a0-0d57-495d-add0-f2f46248d224")]
      [:h3 "This order qualifies for"]
      [:h1.shout.bold "$100 off your install"]
      [:h4.pb6 "You’ll receive a voucher via email after purchase"]]

     [:div.p2.bg-orange.white.center {:data-test "ineligible-seventy-five-off-install-cart-promo"}
      [:h4 "You're almost there..."]
      [:h4 "Buy 3 bundles or more and get"]
      [:h1.shout.bold "$100 off"]
      [:h6
       [:div "your install from your Mayvenn stylist."]
       [:div "Use code " [:span.bold "INSTALL"] " to get your discounted install."]]])])

(defn free-install-cart-promo
  [qualified?]
  [:div.mb3
   (if qualified?
     [:div.bg-teal.bg-celebrate.p2.white.center {:data-test "free-install-cart-promo"}
      [:img {:src    "//ucarecdn.com/db055165-7085-4af5-b265-8aba681e6275/successwhite.png"
             :height "63px"
             :width  "68px"}]
      [:h4 "This order qualifies for a"]
      [:h1.shout.bold "Free Install"]
      [:h6
       [:div "from a Mayvenn Certified Stylist in Fayetteville, NC."]]]

     [:div.p2.bg-orange.white.center {:data-test "ineligible-free-install-cart-promo"}
      [:h4 "You're almost there..."]
      [:h4 "Buy 3 bundles or more and get a"]
      [:h1.shout.bold "Free Install"]
      [:h6
       [:div "from a Mayvenn Certified Stylist in Fayetteville, NC."]
       [:div "Use code " [:span.bold "FREEINSTALL"] " to get your free install."]]])])

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
  [{:keys [the-ville? v2-experience? seventy-five-off-install? show-green-banner?]}]
  (component/create
   [:div
    (cond
      v2-experience?
      (v2-cart-promo show-green-banner?)

      seventy-five-off-install?
      (seventy-five-off-install-cart-promo show-green-banner?)

      the-ville?
      (free-install-cart-promo show-green-banner?)

      :else nil)]))

(defn query
  [data]
  (let [ order (get-in data keypaths/order)]
       {:the-ville?                (experiments/the-ville? data)
        :v2-experience?            (experiments/v2-experience? data)
        :seventy-five-off-install? (experiments/seventy-five-off-install? data)
        :show-green-banner?        (or (orders/install-applied? order)
                                       (orders/freeinstall-applied? order))}))

(defn built-component
  [data opts]
  (component/build component (query data) opts))
