(ns storefront.components.stylist-kit
  (:require [storefront.components.product :as product]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.taxons :as taxons]
            [om.core :as om]
            [sablono.core :refer-macros [html]] ))

(defn kit-image [product]
  [:img.col-12 {:src (->> product :images first :large_url)
                :item-prop "image"
                :alt "Contents of stylist kit, including sample bundle rings, and other Mayvenn stylist resources"}])

(def shipping-and-guarantee
  (html
   [:.border-top.border-bottom.border-light-silver.p2.my2.center.navy.shout.medium.h5
    "Free shipping & 30 day guarantee"]))

(defn kit-description [{:keys [description]}]
  (when description
    (product/description-structure
     [:.mt1 {:dangerouslySetInnerHTML {:__html description}}])))

(defn component [{:keys [product variant-quantity selected-variant adding-to-bag? bagged-variants]} owner]
  (om/component
   (html
    (when product
      (ui/container
       (product/page
        (kit-image product)
        [:div
         [:.center
          (product/title (:name product))
          (product/full-bleed-narrow (kit-image product))]

         [:div product/schema-org-offer-props
          [:.my2 (product/counter-and-price selected-variant variant-quantity)]
          (product/add-to-bag-button adding-to-bag? product selected-variant variant-quantity)]

         (product/bagged-variants-and-checkout bagged-variants)
         shipping-and-guarantee
         (kit-description product)]))))))

(defn query [data]
  ;; Assume the kits taxon has only one product, which has only one variant
  (let [product-id       (first (:product-ids (taxons/current-taxon data)))
        product          (get-in data (conj keypaths/products product-id))
        selected-variant (first (:variants product))]
    {:product          product
     :selected-variant selected-variant
     :variant-quantity (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?   (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants  (get-in data keypaths/browse-recently-added-variants)}))

(defn built-component [data]
  (om/build component (query data)))
