(ns storefront.components.stylist-kit
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.product :as product]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.taxons :as taxons]))

(defn kit-image [product]
  [:img.col-12 {:src (->> product :images first :large_url)
                :item-prop "image"
                :alt "Contents of stylist kit, including sample bundle rings, and other Mayvenn stylist resources"}])

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-light-silver.p2.my2.center.navy.shout.medium.h5
    "Free shipping & 30 day guarantee"]))

(defn kit-description [{:keys [description]}]
  (product/description-structure
   [:div.mt1.dark-gray
    [:div.mbp3.h6 "Includes:"]
    [:ul.list-reset.m0.mb2.navy.h5.medium
     (for [[idx item] (map-indexed vector (:summary description))]
       [:li.mbp3 {:key idx} item])]

    [:div.line-height-2
     (for [[idx item] (map-indexed vector (:commentary description))]
       [:p.mt2 {:key idx} item])]]))

(defn component [{:keys [taxon product variant-quantity selected-variant adding-to-bag? bagged-variants]} owner opts]
  (component/create
   (when product
     (ui/container
      (product/page
       (kit-image product)
       [:div
        [:div.center
         (product/title (:name product))
         (product/full-bleed-narrow (kit-image product))]

        [:div product/schema-org-offer-props
         [:div.my2 (product/counter-and-price selected-variant variant-quantity)]
         (product/add-to-bag-button adding-to-bag? product selected-variant variant-quantity)]

        (product/bagged-variants-and-checkout bagged-variants)
        shipping-and-guarantee
        (kit-description taxon)])))))

(defn query [data]
  ;; Assume the kits taxon has only one product, which has only one variant
  (let [taxon            (taxons/current-taxon data)
        product-id       (first (:product-ids taxon))
        product          (get-in data (conj keypaths/products product-id))
        selected-variant (first (:variants product))]
    {:taxon            taxon
     :product          product
     :selected-variant selected-variant
     :variant-quantity (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?   (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants  (get-in data keypaths/browse-recently-added-variants)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
