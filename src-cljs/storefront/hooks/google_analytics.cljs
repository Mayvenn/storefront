(ns storefront.hooks.google-analytics
  (:require [clojure.string :as string]))

(defn ^:private track
  [event-name data]
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/dataLayer (clj->js {:ecommerce nil}))
    (.push js/dataLayer (clj->js {:event event-name
                                  :ecommerce data}))))
(defn track-login
  []
  (track "login" {}))

(defn track-sign-up
  []
  (track "sign_up" {}))

(defn ^:private line-item-skuer->ga-cart-item
  [line-item-skuer]
  {:item_id        (:legacy/variant-id line-item-skuer)
   :item_variant   (:catalog/sku-id line-item-skuer)
   :item_name      (or (:legacy/product-name line-item-skuer)
                       (:sku/title line-item-skuer))
   :item_category  (-> line-item-skuer :catalog/department first)
   :item_category2 (-> line-item-skuer :hair/family first)
   :item_category3 (-> line-item-skuer :hair/material first)
   :item_category4 (-> line-item-skuer :hair/origin first)
   :item_category5 (-> line-item-skuer :hair/texture first)
   :quantity       (:item/quantity line-item-skuer)
   :price          (:sku/price line-item-skuer)
   ;; Custom:
   :hairCategory   (-> line-item-skuer :hair/family first)
   :hairMaterial   (-> line-item-skuer :hair/material first)
   :hairOrigin     (-> line-item-skuer :hair/origin first)
   :hairStyle      (-> line-item-skuer :hair/texture first)
   :hairGrade      (-> line-item-skuer :hair/grade first)
   :hairLength     (-> line-item-skuer :hair/length first)
   :hairColor      (-> line-item-skuer :hair/color first)})

(defn order<
  [{:keys [total number line-item-skuers buyer-type is-stylist-store
           shipping-method-name shipping-method-sku
           store-slug used-promotion-codes]}]
  {:currency           "USD"
   :value              total
   :items              (map line-item-skuer->ga-cart-item line-item-skuers)
   :coupon             (not-empty (string/join " " used-promotion-codes)) ; HACK

   ;; Custom:
   :orderNuber         number
   :buyerType          buyer-type
   :isStylistStore     is-stylist-store
   :shippingMethodName shipping-method-name
   :shippingMethodSKU  shipping-method-sku
   :storeSlug          store-slug
   :usedPromotionCodes used-promotion-codes})

(defn track-placed-order
  [{:keys [number shipping-method-price]
    :as   data}]
  (track "purchase" (merge (order< data)
                           {:transaction_id number
                            ;; :tax TODO
                            :shipping       shipping-method-price})))

(defn track-checkout-initiate
  [data]
  (track "begin_checkout" (order< data)))

(defn track-add-to-cart
  [{:keys [number line-item-skuers store-slug store-is-stylist order-quantity]}]
  (track "add_to_cart"
         {:items          (mapv line-item-skuer->ga-cart-item line-item-skuers)
          ;; Custom:
          :orderNumber    number
          :orderQuantity  order-quantity
          :storeSlug      store-slug
          :storeIsStylist store-is-stylist}))
