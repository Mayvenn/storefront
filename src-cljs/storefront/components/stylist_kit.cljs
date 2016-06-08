(ns storefront.components.stylist-kit
  (:require [storefront.components.product :as product]
            [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money-without-cents]]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.utils.query :as query]
            [storefront.accessors.taxons :as taxons]
            [om.core :as om]
            [sablono.core :refer-macros [html]] ))

(defn css-url [url] (str "url(" url ")"))

(defn carousel-image [image]
  [:.bg-cover.bg-no-repeat.bg-center.col-12
   {:style {:background-image (css-url image)
            :height "31rem"}}])

(defn component [{:keys [product variant-quantity selected-variant adding-to-bag? bagged-variants]} owner]
  (om/component
   (html
    (ui/narrow-container
     (when product
       ;; TODO: put schema.org back on bundle builder
       [:div {:item-type "http://schema.org/Product"}
        [:.center
         [:h1.medium.titleize.navy.h2.line-height-2 {:item-prop "name"} (:name product)]
         [:.my2.mxn2.md-m0 (carousel-image (->> product :images first :product_url))]]
        [:div {:item-prop "offers"
               :item-scope ""
               :item-type "http://schema.org/Offer"}
         [:.h2.my2
          [:.right-align.light-gray.h5 "PRICE"]
          [:.flex.h1 {:style {:min-height "1.5em"}} ; prevent slight changes to size depending on content of counter
           (if (:can_supply? selected-variant)
             [:.flex-auto
              [:link {:item-prop "availability" :href "http://schema.org/InStock"}]
              (ui/counter variant-quantity
                          false
                          (utils/send-event-callback events/control-counter-dec
                                                     {:path keypaths/browse-variant-quantity})
                          (utils/send-event-callback events/control-counter-inc
                                                     {:path keypaths/browse-variant-quantity}))]
             [:span.flex-auto "Currently out of stock"])
           [:.navy {:item-prop "price"}
            (as-money-without-cents (:price selected-variant))]]]

         (ui/button
          "Add to bag"
          {:on-click      (utils/send-event-callback events/control-add-to-bag
                                                     {:product product
                                                      :variant selected-variant
                                                      :quantity variant-quantity})
           :show-spinner? adding-to-bag?
           :color         "bg-navy"})]

        [:.border-top.border-bottom.border-light-silver.p2.my2.center.navy.shout.medium.h5
         "Free shipping & 30 day guarantee"]

        (when (seq bagged-variants)
          [:div
           (map-indexed product/redesigned-display-bagged-variant bagged-variants)
           [:.cart-button ; for scrolling
            (ui/button "Check out" (utils/route-to events/navigate-cart))]])

        (when-let [html-description (:description product)]
          [:.border.border-light-gray.p2.rounded.mt2
           [:.h3.medium.navy.shout "Description"]
           [:.h5.dark-gray.line-height-2.mt2
            {:item-prop "description" :dangerouslySetInnerHTML {:__html html-description}}]])])))))

(defn- selected-variant [app-state product]
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
