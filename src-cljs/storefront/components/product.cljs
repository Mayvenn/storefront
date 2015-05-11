(ns storefront.components.product
  (:require [storefront.components.utils :as utils]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.taxons :refer [taxon-path-for taxon-class-name]]
            [om.core :as om]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn display-price [app-state product]
  (let [variant-id (get-in app-state state/browse-variant-path)
        variant (or (->> product
                         :variants
                         (filter #(= (% :id) variant-id))
                         first)
                    (-> product :variants first))]
    (str "$" (.toFixed (js/parseFloat (variant :price))))))

(defn display-product-image [image]
  [:img {:src (:product_url image)}])

(defn display-variant [app-state variant checked?]
  [:li.keypad-item
   [:input#variant_id.keypad-input {:type "radio"
                                    :checked checked?}
    [:label.keypad-label {:id (str "variant_id_" (:id variant))
                          :on-click (utils/enqueue-event app-state
                                                         events/control-variant-select
                                                         {:variant variant})}
     (if (variant :can_supply?)
       [:div.variant-description
        (string/join "," (map :presentation (variant :option_values)))]
       [:div.variant-description.out-of-stock
        (string/join "," (map :presentation (variant :option_values)))
        [:br]
        "sold out"])]]])

(defn product-component [data owner]
  (om/component
   (html
    (let [taxon-path (get-in data state/browse-taxon-path)
          taxon (->> (get-in data state/taxons-path)
                      (filter #(= taxon-path (taxon-path-for %)))
                      first)
          products (get-in data (conj state/products-for-taxons-path taxon-path))
          product (first (filter #(= (get-in data state/browse-product-path)
                                     (:slug %))
                                 products))
          images (->> product :master :images)
          collection-name (product :collection_name)
          variants (product :variants)]
      [:div
       [:div.taxon-products-banner
        {:class (or (taxon-class-name taxon) "unknown")}]

       (utils/breadcrumbs
        ["Categories" (:href (utils/route-to data events/navigate-category {:taxon-path (taxon-path-for taxon)}))])

       [:div.product-show {:item-type "http://schema.org/Product"}
        [:div#product-images
         [:div#main-image
          (cond
            (> (count images) 1)
            [:div#slides (map display-product-image images)]
            (seq images)
            (display-product-image (first images)))]
         [:div.product-info
          [:div.product-collection
           [:img {:src (str "/images/products/squiggle-categories-" collection-name "@2x.png")
                  :class collection-name}]
           [:span collection-name]]
          [:div.product-title {:item-prop "name"}
           (product :name)]]]
        [:div.cart-form-container
         [:div#cart-form
          [:form {:action "TODO: populate_orders_path"
                  :method "POST"}
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
                                         (if-let [variant-id (get-in data state/browse-variant-path)]
                                           (= (variant :id) variant-id)
                                           (= index 0))))))
                [:div {:style {:clear "both"}}]]]
              [:input {:type "hidden"
                       :id (get-in product [:master :id])}])
            [:div.price-container
             [:div.quantity
              [:h4.quantity-label "Quantity"]
              [:div.quantity-selector
               [:div.minus [:a.pm-link
                            {:href "#"
                             :on-click (utils/enqueue-event data events/control-variant-dec-quantity)}
                            "-"]]
               [:input#quantity.quantity-selector-input
                {:min 1
                 :name "quantity"
                 :type "text"
                 :value (str (get-in data state/browse-variant-quantity-path))
                 :on-change #(utils/put-event data events/control-variant-set-quantity
                                              {:value-str (.. % -target -value)})}]
               [:div.plus [:a.pm-link
                           {:href "#"
                            :on-click (utils/enqueue-event data events/control-variant-inc-quantity)}
                           "+"]]]]
             [:div#product-price.product-price
              [:span.price-label "Price:"]
              [:span.price.selling {:item-prop "price"}
               (display-price data product)]
              ;; TODO: do we need this SEO for currency?
              [:span {:item-prop "priceCurrency" :content (:currency product)}]

              (if (get-in product [:master :can_supply?])
                [:link {:item-prop "availability" :href "http://schema.org/InStock"}]
                [:span.out-of-stock [:br] (str (:name product) " is out of stock.")])]

             [:div.add-to-cart {:style {:clear "both"}}
              [:input.large.primary#add-to-cart-button {:type "submit"
                                                        :value "Add to Bag"}]]]]]
          [:div#after-add
           [:div.added-to-bag-container]
           [:div.go-to-checkout
            [:a.cart-button {:href "TODO: cart_path"}
             "Go to Checkout >>"]
            [:figure.checkout-cart]
            [:figure.checkout-guarantee]]]]]

        [:div#product-collection-description.product-collection-description
         [:div.bar]
         [:div.product-collection-circles
          [:img {:src (str "/images/products/hairtype-circle-premier" (if (= collection-name "premier") "" "-disabled") "@2x.png")}]
          [:img {:src (str "/images/products/hairtype-circle-deluxe" (if (= collection-name "deluxe") "" "-disabled") "@2x.png")}]
          [:img {:src (str "/images/products/hairtype-circle-ultra" (if (= collection-name "ultra") "" "-disabled") "@2x.png")}]]
         [:div.product-collection-text
          [:h3.sub-header (str collection-name ":")]
          (product :collection_description)]]

        [:div#product-description.product-description
         [:h3.sub-header "Description"]
         [:div.product-description-text {:item-prop "description"}
          (if (:description product)
            (string/replace (:description product)
                            #"(.*?)\r?\n\r?\n"
                            "<p>$1</p>")
            "This product has no description")]]

        [:div.product-reviews]]

       [:div.gold-features
        [:figure.guarantee-feature]
        [:figure.free-shipping-feature]
        [:figure.triple-bundle-feature]]]))))
