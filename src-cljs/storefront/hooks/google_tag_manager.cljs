(ns storefront.hooks.google-tag-manager)

(defn ^:private track
  [data]
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/dataLayer (clj->js data))))

(defn ^:private line-item-skuer->gtm-cart-item
  [line-item-skuer]
  {:catalogDepartment (:catalog/department line-item-skuer)
   :productID         (:legacy/variant-id line-item-skuer)
   :hairCategory      (-> line-item-skuer :hair/family first)
   :hairColor         (-> line-item-skuer :hair/color first)
   :hairGrade         (-> line-item-skuer :hair/grade first)
   :hairLength        (-> line-item-skuer :hair/length first)
   :hairMaterial      (-> line-item-skuer :hair/material first)
   :hairOrigin        (-> line-item-skuer :hair/origin first)
   :hairStyle         (-> line-item-skuer :hair/texture first)
   :sku               (:catalog/sku-id line-item-skuer)
   :quantity          (:item/quantity line-item-skuer)
   :price             (:sku/price line-item-skuer)
   :name              (or (:legacy/product-name line-item-skuer)
                          (:sku/title line-item-skuer))})

(defn track-placed-order
  [{:keys [total number line-item-skuers buyer-type is-stylist-store
           shipping-method-name shipping-method-price shipping-method-sku
           store-slug used-promotion-codes ]}]
  (track {:event               "orderPlaced"
          :transactionTotal    total
          :transactionId       number
          :transactionShipping shipping-method-price
          :transactionProducts (map line-item-skuer->gtm-cart-item line-item-skuers)
          :buyerType           buyer-type
          :isStylistStore      is-stylist-store
          :shippingMethodName  shipping-method-name
          :shippingMethodSKU   shipping-method-sku
          :storeSlug           store-slug
          :usedPromotionCodes  used-promotion-codes}))

(defn track-checkout-initiate
  [{:keys [number]}]
  (track {:event       "checkoutInitiate"
          :orderNumber number}))

(defn track-add-to-cart
  [{:keys [number line-item-skuers store-slug store-is-stylist order-quantity]}]
  (track {:event          "addToCart"
          :orderNumber    number
          :orderQuantity  order-quantity
          :storeSlug      store-slug
          :storeIsStylist store-is-stylist
          :items          (mapv line-item-skuer->gtm-cart-item line-item-skuers)}))
