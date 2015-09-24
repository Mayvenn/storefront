(ns storefront.components.product
  (:require [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money-without-cents]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.utils.query :as query]
            [storefront.accessors.taxons :refer [taxon-path-for taxon-class-name] :as taxons]
            [storefront.accessors.products :refer [graded? all-variants]]
            [storefront.components.breadcrumbs :refer [breadcrumbs]]
            [storefront.components.counter :refer [counter-component]]
            [storefront.components.reviews :refer [reviews-component reviews-summary-component]]
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

(defn display-bagged-variant [app-state {:keys [id quantity]}]
  (let [variant (query/get {:id id}
                           (->> (get-in app-state keypaths/products)
                                vals
                                (mapcat all-variants)))
        product (first (filter #(contains? (set (all-variants %)) variant)
                               (-> (get-in app-state keypaths/products)
                                   vals)))]
    [:div.item-added
     [:strong "Added to Bag: "]
     (str (number->words quantity)
          " "
          (-> (:options_text variant)
              (string/replace #"Length: " "")
              (string/replace #"''" " inch"))
          " "
          (:name product))]))

(defn display-variant [app-state variant checked?]
  [:li.keypad-item
   [:input.keypad-input {:type "radio"
                         :id (str "variant_id_" (:id variant))
                         :checked checked?
                         :on-change (utils/send-event-callback app-state
                                                               events/control-browse-variant-select
                                                               {:variant variant})}]
   [:label.keypad-label {:for (str "variant_id_" (:id variant))}
    (if (variant :can_supply?)
      [:div.variant-description
       (string/join "," (map :presentation (variant :option_values)))]
      [:div.variant-description.out-of-stock
       (string/join "," (map :presentation (variant :option_values)))
       [:br]
       "sold out"])]])

(defn product-component [data owner]
  (om/component
   (html
    (let [taxon (taxons/current-taxon data)
          taxon-path (if taxon (taxon-path-for taxon))
          product (query/get (get-in data keypaths/browse-product-query)
                             (vals (get-in data keypaths/products)))
          images (->> product :master :images)
          collection-name (:collection_name product)
          variants (:variants product)]
      (when product
        [:div
         (when taxon-path
           [:div
            [:div.taxon-products-banner
             {:class (if taxon-path (taxon-class-name taxon) "unknown")}]

            (breadcrumbs
             data
             ["Categories" [events/navigate-category {:taxon-path taxon-path}]]
             [(:name taxon) [events/navigate-category {:taxon-path taxon-path}]])])

         [:div.product-show {:item-type "http://schema.org/Product"}
          [:div#product-images
           (when (get-in data keypaths/reviews-loaded)
             (om/build reviews-summary-component data
                       {:opts {:product-id (:id product)}}))
           [:div#main-image
            (cond
              (> (count images) 1)
              [:div#slides (map display-product-image images)]
              (seq images)
              (display-product-image (first images)))]
           [:div.product-info
            (when collection-name
              [:div.product-collection
               (when (graded? product)
                 [:div.product-collection-indicator {:class collection-name}])
               [:span
                collection-name]])
            [:div.product-title {:item-prop "name"}
             (:name product)]]]
          [:div.cart-form-container
           [:div#cart-form
            [:form
             [:div#inside-product-cart-form {:item-prop "offers"
                                             :item-scope ""
                                             :item-type "http://schema.org/Offer"}
              (if (seq variants)
                [:div#product-variants
                 [:h6.ui-descriptor "Select a hair length in inches:"]
                 [:ul.keypad
                  (->> variants
                       (filter (comp seq :option_values))
                       (map-indexed
                        (fn [index variant]
                          (display-variant data
                                           variant
                                           (if-let [variant-query (get-in data keypaths/browse-variant-query)]
                                             (query/matches? variant-query variant)
                                             (= index 0))))))
                  [:div {:style {:clear "both"}}]]]
                [:input {:type "hidden"
                         :id (get-in product [:master :id])}])
              [:div.price-container
               [:div.quantity
                [:h4.quantity-label "Quantity"]
                (om/build counter-component data {:opts {:path keypaths/browse-variant-quantity
                                                         :inc-event events/control-counter-inc
                                                         :dec-event events/control-counter-dec
                                                         :set-event events/control-counter-set}})]
               [:div#product-price.product-price
                (let [variant (selected-variant data product)]
                  (if (= (variant :price) (variant :original_price))
                    (list
                     [:span.price-label "Price:"]
                     [:span.price.selling {:item-prop "price"}
                      (as-money-without-cents (:price variant))])
                    (list
                     [:span.price-label "New Price:"]
                     [:span.price.selling {:item-prop "price"}
                      [:span.original-price
                       (as-money-without-cents (:original_price variant))]
                      [:span.current-price
                       (as-money-without-cents (:price variant))]])))
                [:span {:item-prop "priceCurrency" :content (:currency product)}]
                (if (get-in product [:master :can_supply?])
                  [:link {:item-prop "availability" :href "http://schema.org/InStock"}]
                  [:span.out-of-stock [:br] (str (:name product) " is out of stock.")])]

               (let [adding-to-cart (query/get {:request-key request-keys/add-to-bag}
                                               (get-in data keypaths/api-requests))]
                 [:div.add-to-cart {:style {:clear "both"}}
                  [:.large.primary#add-to-cart-button
                   {:on-click
                    (when-not adding-to-cart
                      (utils/send-event-callback data events/control-browse-add-to-bag))
                    :class (when adding-to-cart "saving")}
                   "Add to Bag"]])]]]

            (when-let [bagged-variants (seq (get-in data keypaths/browse-recently-added-variants))]
              [:div#after-add {:style {:display "block"}}
               [:div.added-to-bag-container
                (map (partial display-bagged-variant data) bagged-variants)]
               [:div.go-to-checkout
                [:a.cart-button
                 (utils/route-to data events/navigate-cart)
                 "Go to Checkout >>"
                 [:figure.checkout-cart]
                 [:figure.checkout-guarantee]]]])]]

          [:div
           [:div.left-of-reviews-wrapper
            [:div#product-collection-description.product-collection-description
             (when (graded? product)
               (list
                [:div.product-collection-circles-container
                 [:div.product-collection-circles
                  [:div.inner-product-collection-circles {:class (str "premier" (when-not (= collection-name "premier") " disabled"))}]
                  [:div.inner-product-collection-circles {:class (str "deluxe" (when-not (= collection-name "deluxe") " disabled"))}]
                  [:div.inner-product-collection-circles {:class (str "ultra" (when-not (= collection-name "ultra") " disabled"))}]]
                 [:div.bar]]
                [:div.product-collection-text
                 [:h3.sub-header (str collection-name ": ")]
                 (product :collection_description)]))]
            (when-let [html-description (:description product)]
              [:div#product-description.product-description
               [:h3.sub-header "Description"]
               [:div.product-description-text {:item-prop "description" :dangerouslySetInnerHTML {:__html html-description}}]])]

           (when (get-in data keypaths/reviews-loaded)
             (om/build reviews-component data
                       {:opts {:product-id (:id product)}}))]]

         [:div.gold-features
          [:figure.guarantee-feature]
          [:figure.free-shipping-feature]
          [:figure.triple-bundle-feature]]])))))
