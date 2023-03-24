(ns storefront.hooks.google-analytics
  (:require [clojure.string :as string]
            [storefront.components.formatters :as f]
            [goog.crypt :as crypt]
            goog.crypt.Sha256))

(defn sha256< [message]
  (let [sha256 (js/goog.crypt.Sha256.)]
    (.update sha256 (crypt/stringToByteArray message))
    (crypt/byteArrayToHex (.digest sha256))))

(defn ^:private track
  [event-name data]
  (when (.hasOwnProperty js/window "dataLayer")
    (.push js/dataLayer (clj->js {:ecommerce nil}))
    (.push js/dataLayer (clj->js {:event event-name
                                  :ecommerce data}))))

(defn ^:private mayvenn-line-item->ga4-item 
  ([item quantity] (mayvenn-line-item->ga4-item (assoc item :item/quantity quantity)))
  ([item]
   {:item_id   (:catalog/sku-id item)
    :item_name (or (:legacy/product-name item)
                   (:sku/title item))
    :quantity  (or (:item/quantity item) 1)
    :price     (:sku/price item)}))

(defn track-add-to-cart
  "Track an add-to-cart event in GA4 schema."
  ;; When things settle down we might want to consider the following or more:
  ;; order/number, order/quantity, store/slug, stylist?
  [{:keys [line-item-skuers]}]
  ;; NOTE: We are ignoring discounts here
  ;; TODO: We should probably minus the discount out of the value.
  (let [value (reduce + 0 (map :sku/price line-item-skuers))]
    (track "add_to_cart"
           {:items    (mapv mayvenn-line-item->ga4-item line-item-skuers)
            :currency "USD"
            :value    value})))

(defn track-login [] #_(track "login" {}))
(defn track-sign-up [] #_(track "sign_up" {}))

(defn track-placed-order
  [{:keys [number shipping-method-price line-item-skuers used-promotion-codes tax email address]}]
  (let [{:keys [phone first-name last-name address1 city state zipcode]} address]
    (track "purchase" 
           {:transaction_id           number
            :items                    (mapv mayvenn-line-item->ga4-item line-item-skuers)
            :currency                 "USD"
            :value                    (reduce + 0 (map :sku/price line-item-skuers))
            :coupon                   (->> used-promotion-codes (string/join " ") not-empty)
            :tax                      tax
            :shipping                 shipping-method-price
            :enhanced_conversion_data {:sha256_email_address (sha256< email)
                                       :sha256_phone_number  (sha256< (f/e164-phone phone))
                                       :address              {:sha256_first_name (sha256< first-name)
                                                              :sha256_last_name  (sha256< last-name)
                                                              :street            address1
                                                              :city              city
                                                              :region            state
                                                              :postal_code       zipcode
                                                              :country           "us"}}})))

(defn track-begin-checkout
  [{:keys [line-item-skuers used-promotion-codes]}]
  (track "begin_checkout" {:items          (mapv mayvenn-line-item->ga4-item line-item-skuers)
                           :currency       "USD"
                           :value          (reduce + 0 (map :sku/price line-item-skuers))
                           :coupon         (->> used-promotion-codes (string/join " ") not-empty)}))

(defn track-view-item
  [sku]
  (track "view_item" {:currency "USD"
                      :value    (:sku/price sku)
                      :items    [(mayvenn-line-item->ga4-item sku)]}))

(defn track-select-promotion
  [{:keys [promotion]}]
  (track "select_promotion" {:promotion_id   (:code promotion)
                             :promotion_name (:description promotion)}))

(defn track-remove-from-cart
  [{:keys [sku quantity]}]
  (track "remove_from_cart"
         {:currency "USD"
          :items    [(mayvenn-line-item->ga4-item sku quantity)]}))

(defn track-generate-lead
  ;; TODO: We should probably track the trigger/template ids
  []
  (track "generate_lead" {:currency "USD" 
                          :value    0}))