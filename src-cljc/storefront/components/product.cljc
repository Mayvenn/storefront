(ns storefront.components.product
  (:require [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.components.money-formatters :refer [as-money-without-cents]]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2 {:item-type "http://schema.org/Product"}
   [:div.md-up-col.md-up-col-7.px2 [:div.to-md-hide wide-left]]
   [:div.md-up-col.md-up-col-5.px2 wide-right-and-narrow]])

(defn title [name]
  [:h1.medium.titleize.navy.h2.line-height-2 {:item-prop "name"} name])

(defn full-bleed-narrow [body]
  ;; The mxn2 pairs with the p2 of the ui/container, to make the body full width
  ;; on mobile.
  [:div.md-up-hide.mxn2.my2 body])

(def schema-org-offer-props
  {:item-prop "offers"
   :item-scope ""
   :item-type "http://schema.org/Offer"})

(defn quantity-and-price-structure [quantity price]
  [:div
   [:div.right-align.light-gray.h5 "PRICE"]
   [:div.flex.h1 {:style {:min-height "1.5em"}} ; prevent slight changes to size depending on content of counter
    [:div.flex-auto quantity]
    [:div.navy price]]])

(defn counter-or-out-of-stock [can-supply? quantity]
  (if can-supply?
    [:div
     [:link {:item-prop "availability" :href "http://schema.org/InStock"}]
     (ui/counter quantity
                 false
                 (utils/send-event-callback events/control-counter-dec
                                            {:path keypaths/browse-variant-quantity})
                 (utils/send-event-callback events/control-counter-inc
                                            {:path keypaths/browse-variant-quantity}))]
    [:span.h3 "Currently out of stock"]) )

(defn counter-and-price [{:keys [can_supply? price]} quantity]
  (quantity-and-price-structure
   (counter-or-out-of-stock can_supply? quantity)
   [:span {:item-prop "price"} (as-money-without-cents price)]))

(defn add-to-bag-button [adding-to-bag? product variant quantity]
  (ui/button
   "Add to bag"
   {:on-click      (utils/send-event-callback events/control-add-to-bag
                                              {:product  product
                                               :variant  variant
                                               :quantity quantity})
    :data-test     "add-to-bag"
    :show-spinner? adding-to-bag?
    :color         "bg-navy"}))

(defn ^:private number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-variant [idx {:keys [quantity product variant]}]
  [:div.h6.line-height-3.my1.p1.caps.gray.bg-dark-white.medium.center
   {:key idx
    :data-test "items-added"}
   "Added to bag: "
   (number->words quantity)
   " "
   (some-> variant :variant_attrs :length)
   " "
   ;; TODO: could this be products/summary? (it only needs a variant)
   (:name product)])

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-scroll "cart-button"}
    (ui/button "Check out" (utils/route-to events/navigate-cart))]))

(defn bagged-variants-and-checkout [bagged-variants]
  (when (seq bagged-variants)
    [:div
     (map-indexed display-bagged-variant bagged-variants)
     checkout-button]))

(defn description-structure [body]
  [:div.border.border-light-gray.p2.rounded
   [:div.h3.medium.navy.shout "Description"]
   [:div {:item-prop "description"} body]])
