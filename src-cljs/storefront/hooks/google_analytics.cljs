(ns storefront.hooks.google-analytics
  (:require [clojure.string :as string]))

(defn ^:private track
  [event-name data]
  #_(prn "track" event-name data)
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/dataLayer (clj->js {:ecommerce nil}))
    (.push js/dataLayer (clj->js {:event event-name
                                  :ecommerce data}))))

(defn track-add-to-cart
  "Track an add-to-cart event in GA4 schema.
   
   When things settle down we might want to consider the following or more:
   order/number, order/quantity, store/slug, stylist?"
  [{:keys [line-item-skuers]}]
  ;; NOTE: We are ignoring discounts here
  ;; TODO: We should probably minus the discount out of the value.
  (let [value (reduce + 0 (map :sku/price line-item-skuers))]
    (track "add_to_cart"
           {:items    (mapv (fn mayvenn->ga4 [item]
                              {:item_id   (:catalog/sku-id item)
                               :item_name (or (:legacy/product-name item)
                                              (:sku/title item))
                               :quantity  (:item/quantity item)
                               :price     (:sku/price item)}) line-item-skuers)
            :currency "USD"
            :value    value})))

(defn track-login [] #_  (track "login" {}))
(defn track-sign-up [] #_(track "sign_up" {}))

#_
(defn ^:private line-item-skuer->ga-cart-item
  [line-item-skuer]
  {:item_id        (:catalog/sku-id line-item-skuer) 
   :item_name      (or (:legacy/product-name line-item-skuer)
                       (:sku/title line-item-skuer))
   :quantity       (:item/quantity line-item-skuer)
   :price          (:sku/price line-item-skuer)})

#_
(defn order<
  [{:keys [total number line-item-skuers buyer-type is-stylist-store
           shipping-method-name shipping-method-sku
           store-slug used-promotion-codes]}]
  {:currency           "USD"
   :value              total
   :items              (map line-item-skuer->ga-cart-item line-item-skuers)
   :coupon             (not-empty (string/join " " used-promotion-codes)) ; HACK

   ;; Custom:
   :orderNumber        number
   :buyerType          buyer-type
   :isStylistStore     is-stylist-store
   :shippingMethodName shipping-method-name
   :shippingMethodSKU  shipping-method-sku
   :storeSlug          store-slug
   :usedPromotionCodes used-promotion-codes})

(defn track-placed-order
  [{:keys [number shipping-method-price line-item-skuers used-promotion-codes tax]}]
  (track "purchase" {:transaction_id number
                     :items          (mapv (fn mayvenn->ga4 [item]
                                             {:item_id   (:catalog/sku-id item)
                                              :item_name (or (:legacy/product-name item)
                                                             (:sku/title item))
                                              :quantity  (:item/quantity item)
                                              :price     (:sku/price item)}) line-item-skuers)
                     :currency       "USD"
                     :value          (reduce + 0 (map :sku/price line-item-skuers))
                     :coupon         (not-empty (string/join " " used-promotion-codes))
                     :tax            tax
                     :shipping       shipping-method-price}))

(defn track-checkout-initiate
  [data]
#_
  (track "begin_checkout" (order< data)))

(defn track-view-item
  [sku]
#_
  (track "view_item"
         {:items [(line-item-skuer->ga-cart-item sku)]}))
(defn track-select-promotion
  [{:keys [promo-code]}]
#_
  (track "select_promotion" {:promotion_id promo-code}))

(defn track-remove-from-cart
  [{:keys [sku number]}]
#_
  (track "remove_from_cart"
         {:items       [(line-item-skuer->ga-cart-item sku)]
          :orderNumber number}))

(defn track-generate-lead
  "TODO: We should probably track the trigger/template ids"
  []
  (track "generate_lead" {:currency "USD" :value 0}))