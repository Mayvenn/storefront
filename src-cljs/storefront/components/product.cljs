(ns storefront.components.product
  (:require [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money-without-cents]]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.utils.query :as query]
            [storefront.accessors.taxons :as taxons]
            [om.core :as om]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]))

(defn number->words [n]
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

(defn component [{:keys [product variant-quantity selected-variant adding-to-bag? bagged-variants]} owner]
  (om/component
   (html
    (when product
      (let [image (->> product :images first :product_url)
            variants (:variants product)]
        [:div
         [:div.product-show {:item-type "http://schema.org/Product"}
          [:div#product-images
           [:div#main-image
            [:img {:src image}]]
           [:div.product-title {:item-prop "name"}
            (:name product)]]
          [:.guarantee-banner
           [:figure.guarantee-banner-image]]
          [:div.cart-form-container
           [:div#cart-form
            [:form
             ;; TODO: port this to redesigned page, and put it back on bundle builder
             [:div#inside-product-cart-form {:item-prop "offers"
                                             :item-scope ""
                                             :item-type "http://schema.org/Offer"}
              [:div.price-container
               [:div.quantity
                [:h4.gray "Quantity"]
                [:.h2.mt2
                 (ui/counter variant-quantity
                             false
                             (utils/send-event-callback events/control-counter-dec
                                                        {:path keypaths/browse-variant-quantity})
                             (utils/send-event-callback events/control-counter-inc
                                                        {:path keypaths/browse-variant-quantity}))]]
               [:div#product-price.product-price
                [:span.price-label "Price:"]
                [:span.price.selling {:item-prop "price"}
                 (as-money-without-cents (:price selected-variant))]
                [:span {:item-prop "priceCurrency" :content (:currency product)}]
                (if (some :can_supply? variants)
                  [:link {:item-prop "availability" :href "http://schema.org/InStock"}]
                  [:span.out-of-stock [:br] (str (:name product) " is out of stock.")])]

               [:a.large.primary.alternate#add-to-cart-button
                {:on-click
                 (when-not adding-to-bag?
                   (utils/send-event-callback events/control-browse-add-to-bag {:product  product
                                                                                :variant  selected-variant
                                                                                :quantity variant-quantity}))
                 :class (when adding-to-bag? "saving")}
                "Add to Cart"]]]]

            (when (seq bagged-variants)
              [:div#after-add {:style {:display "block"}}
               [:div.added-to-bag-container
                (map display-bagged-variant bagged-variants)]
               [:div.go-to-checkout
                [:a.cart-button
                 (utils/route-to events/navigate-cart)
                 "Go to Checkout >>"
                 [:figure.checkout-cart]
                 [:figure.checkout-guarantee]]]])]]

          [:div
           [:div.left-of-reviews-wrapper
            (when-let [html-description (:description product)]
              [:div#product-description.product-description
               [:h3.sub-header "Description"]
               [:div.product-description-text {:item-prop "description" :dangerouslySetInnerHTML {:__html html-description}}]])]]]

         [:div.clearfix]])))))

(defn selected-variant [app-state product]
  (query/get (get-in app-state keypaths/browse-variant-query)
             (:variants product)))

(defn query [data]
  (let [product-id (first (:product-ids (taxons/current-taxon data)))
        product (query/get {:id product-id}
                           (vals (get-in data keypaths/products)))]
    {:product          product
     :selected-variant (selected-variant data product)
     :variant-quantity (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?   (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants  (get-in data keypaths/browse-recently-added-variants)}))

(defn built-component [data]
  (om/build component (query data)))
