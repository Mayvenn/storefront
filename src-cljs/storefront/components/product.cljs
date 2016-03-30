(ns storefront.components.product
  (:require [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money-without-cents]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.utils.query :as query]
            [storefront.accessors.taxons :refer [taxon-path-for] :as taxons]
            [storefront.accessors.products :refer [all-variants]]
            [storefront.components.counter :refer [counter-component]]
            [om.core :as om]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]))

(defn selected-variant [app-state product]
  (let [variant-query (get-in app-state keypaths/browse-variant-query)]
    (->> product
         all-variants
         (query/get variant-query))))

(defn display-product-image [image]
  [:img {:src (:product_url image)}])

(defn number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-variant [{:keys [quantity product variant]}]
  [:div.item-added
   [:strong "Added to Cart: "]
   (str (number->words quantity)
        " "
        (some-> variant
                :variant_attrs
                :length)
        " "
        (:name product))])

(defn product-component [data owner]
  (om/component
   (html
    (let [taxon (taxons/current-taxon data)
          taxon-path (if taxon (taxon-path-for taxon))
          product (query/get (get-in data keypaths/browse-product-query)
                             (vals (get-in data keypaths/products)))
          images (->> product :master :images)
          variants (:variants product)]
      (when product
        [:div
         [:div.product-show {:item-type "http://schema.org/Product"}
          [:div#product-images
           [:div#main-image
            (cond
              (> (count images) 1)
              [:div#slides (map display-product-image images)]
              (seq images)
              (display-product-image (first images)))]
           [:div.product-title {:item-prop "name"}
            (:name product)]]
          [:.guarantee-banner
           [:figure.guarantee-banner-image]]
          [:div.cart-form-container
           [:div#cart-form
            [:form
             [:div#inside-product-cart-form {:item-prop "offers"
                                             :item-scope ""
                                             :item-type "http://schema.org/Offer"}
              [:input {:type "hidden"
                       :id (get-in product [:master :id])}]
              [:div.price-container
               [:div.quantity
                [:h4.quantity-label "Quantity"]
                (om/build counter-component data {:opts {:path keypaths/browse-variant-quantity
                                                         :inc-event events/control-counter-inc
                                                         :dec-event events/control-counter-dec
                                                         :set-event events/control-counter-set}})]
               [:div#product-price.product-price
                [:span.price-label "Price:"]
                (let [variant (selected-variant data product)]
                  [:span.price.selling {:item-prop "price"}
                   (as-money-without-cents (:price variant))])
                [:span {:item-prop "priceCurrency" :content (:currency product)}]
                (if (get-in product [:master :can_supply?])
                  [:link {:item-prop "availability" :href "http://schema.org/InStock"}]
                  [:span.out-of-stock [:br] (str (:name product) " is out of stock.")])]

               (let [adding-to-cart (query/get {:request-key request-keys/add-to-bag}
                                               (get-in data keypaths/api-requests))]
                 ;; TODO: disable add to bag button until there is a browse-variant-query
                 [:a.large.primary.alternate#add-to-cart-button
                  {:on-click
                   (when-not adding-to-cart
                     (utils/send-event-callback events/control-browse-add-to-bag))
                   :class (when adding-to-cart "saving")}
                  "Add to Cart"])]]]

            (when-let [bagged-variants (seq (get-in data keypaths/browse-recently-added-variants))]
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

         [:div.clear-fix]])))))
