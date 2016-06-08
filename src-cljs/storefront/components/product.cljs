(ns storefront.components.product
  (:require [storefront.components.ui :as ui]
            [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money-without-cents]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [sablono.core :refer-macros [html]]))

(defn page [wide-left wide-right-and-narrow]
  [:.clearfix.mxn2 {:item-type "http://schema.org/Product"}
   [:.md-up-col.md-up-col-7.px2 [:.to-md-hide wide-left]]
   [:.md-up-col.md-up-col-5.px2 wide-right-and-narrow]])

(defn title [name]
  [:h1.medium.titleize.navy.h2.line-height-2 {:item-prop "name"} name])

(defn full-bleed-narrow [body]
  ;; The mxn2 pairs with the p2 of the ui/container, to make the body full width
  ;; on mobile.
  [:.md-up-hide.mxn2.my2 body])

(def schema-org-offer-props
  {:item-prop "offers"
   :item-scope ""
   :item-type "http://schema.org/Offer"})

(defn quantity-and-price-structure [quantity price]
  [:div
   [:.right-align.light-gray.h5 "PRICE"]
   [:.flex.h1 {:style {:min-height "1.5em"}} ; prevent slight changes to size depending on content of counter
    [:.flex-auto quantity]
    [:.navy price]]])

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
                                              {:product product
                                               :variant variant
                                               :quantity quantity})
    :show-spinner? adding-to-bag?
    :color         "bg-navy"}))

(defn ^:private number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-variant [{:keys [quantity product variant]}]
  [:div.item-added
   [:strong "Added to Cart: "]
   (number->words quantity)
   " "
   (some-> variant :variant_attrs :length)
   " "
   (:name product)])

(defn redesigned-display-bagged-variant [idx {:keys [quantity product variant]}]
  [:.h6.line-height-3.my1.p1.caps.gray.bg-dark-white.medium.center
   {:key idx}
   "Added to bag: "
   (number->words quantity)
   " "
   (some-> variant :variant_attrs :length)
   " "
   (:name product)])

(defn redesigned-bagged-variants [bagged-variants]
  (map-indexed redesigned-display-bagged-variant bagged-variants))

(def checkout-button
  (html
   [:.cart-button ; for scrolling
    (ui/button "Check out" (utils/route-to events/navigate-cart))]))

(defn bagged-variants-and-checkout [bagged-variants]
  (when (seq bagged-variants)
    [:div
     (redesigned-bagged-variants bagged-variants)
     checkout-button]))

(defn description-structure [body]
  [:.border.border-light-gray.p2.rounded
   [:.h3.medium.navy.shout "Description"]
   [:div {:item-prop "description"} body]])
