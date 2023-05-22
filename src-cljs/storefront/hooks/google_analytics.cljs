(ns storefront.hooks.google-analytics
  (:require [clojure.string :as string]
            [spice.maps :as maps]
            [storefront.transitions :as t]
            [storefront.keypaths :as k]
            [storefront.events :as e]
            [storefront.components.formatters :as f]
            goog.crypt
            goog.crypt.Sha256))

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
  [{:keys [line-item-skuers user-ecd]}]
  ;; NOTE: We are ignoring discounts here
  ;; TODO: We should probably minus the discount out of the value.
  (let [value (reduce + 0 (map :sku/price line-item-skuers))]
    (track "add_to_cart"
           (merge
            {:items    (mapv mayvenn-line-item->ga4-item line-item-skuers)
             :currency "USD"
             :value    value}
            user-ecd))))

(defn track-login [] #_(track "login" {}))
(defn track-sign-up [] #_(track "sign_up" {}))

(defn track-placed-order
  [{:keys [number shipping-method-price line-item-skuers used-promotion-codes tax user-ecd]}]
  (track "purchase" (merge {:transaction_id number
                            :items          (mapv mayvenn-line-item->ga4-item line-item-skuers)
                            :currency       "USD"
                            :value          (reduce + 0 (map :sku/price line-item-skuers))
                            :coupon         (->> used-promotion-codes (string/join " ") not-empty)
                            :tax            tax
                            :shipping       shipping-method-price}
                           user-ecd)))

(defn track-begin-checkout
  [{:keys [line-item-skuers used-promotion-codes user-ecd]}]
  (track "begin_checkout" (merge {:items          (mapv mayvenn-line-item->ga4-item line-item-skuers)
                                  :currency       "USD"
                                  :value          (reduce + 0 (map :sku/price line-item-skuers))
                                  :coupon         (->> used-promotion-codes (string/join " ") not-empty)}
                                 user-ecd)))

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
  [user-ecd]
  (track "generate_lead" (merge {:currency "USD"
                                 :value    0}
                                user-ecd)))

(defn sha256< [message]
  (when (seq message)
    (let [sha256 (js/goog.crypt.Sha256.)]
      (->> message string/lower-case string/trim goog.crypt/stringToByteArray (.update sha256))
      (goog.crypt/byteArrayToHex (.digest sha256)))))

(defmethod t/transition-state e/set-user-ecd
  [_ _event {:keys [email phone first-name last-name address1 city state zipcode]} app-state]
  (update-in app-state
             k/user-ecd
             #(maps/deep-merge % (maps/deep-remove-nils {:email                email                ;; Meta requires, google accepts
                                                         :phone_number         (f/e164-phone phone)
                                                         
                                                         ;; google accepts these fields
                                                         :sha256_email_address (sha256< email)
                                                         :sha256_phone_number  (sha256< (f/e164-phone phone))

                                                         ;; Meta
                                                         :phone                (f/e164-phone phone) ;; Meta requires
                                                         :first_name           first-name
                                                         :last_name            last-name
                                                         :city                 city 
                                                         :state                state
                                                         :zip                  zipcode ; Meta
                                                         :country              "us" ; Meta

                                                         :address              {:sha256_first_name (sha256< first-name)
                                                                                :sha256_last_name  (sha256< last-name)
                                                                                ;:first_name        first-name
                                                                                ;:last_name         last-name
                                                                                :street            address1
                                                                                :city              city
                                                                                :region            state
                                                                                :postal_code       zipcode
                                                                                :country           "us"}}))))
