(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [cemerick.url :refer [url-encode]]
            [sablono.core :refer [html]]
            [storefront.hooks.stringer :as stringer]
            [storefront.platform.messages :refer [handle-message handle-later]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.order-summary :as summary]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]
            [storefront.accessors.products :as accessors.products]
            [catalog.products :as products]
            [storefront.hooks.affirm :as affirm]
            [storefront.components.affirm :as affirm-components]
            [storefront.transitions :as transitions]
            [storefront.api :as api]
            [storefront.accessors.images :as images]))

(defn requires-additional-payment? [data]
  (let [no-stripe-payment?  (nil? (get-in data keypaths/order-cart-payments-stripe))
        no-affirm-payment?  (nil? (get-in data keypaths/order-cart-payments-affirm))
        store-credit-amount (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0)
        order-total         (get-in data keypaths/order-total)]
    (and
     ;; stripe can charge any amount
     no-stripe-payment?
     ;; affirm will be setup after this screen and will try to cover any amount
     no-affirm-payment?
     ;; is total covered by remaining store-credit?
     (> order-total store-credit-amount))))

;; TODO merge with affirm component (if possible)
(defn component
  [{:keys [available-store-credit
           checkout-steps
           payment delivery order
           placing-order?
           skus
           selected-affirm?
           order-valid-for-affirm?
           requires-additional-payment?
           saving-card?
           updating-shipping?]}
   owner]
  (om/component
   (html
    (let [affirm-selected-but-not-valid?   (and (not order-valid-for-affirm?) selected-affirm?)
          affirm-selected-and-order-valid? (and selected-affirm? order-valid-for-affirm?)]
      [:div.container.p2
       (om/build checkout-steps/component checkout-steps)

       [:.clearfix.mxn3
        [:.col-on-tb-dt.col-6-on-tb-dt.px3
         [:.h3.left-align "Order Summary"]

         [:div.my2
          {:data-test "confirmation-line-items"}
          (summary/display-line-items (orders/product-items order) skus)]]

        [:.col-on-tb-dt.col-6-on-tb-dt.px3
         (om/build checkout-delivery/component delivery)
         [:form
          {:on-submit
           (if (and order-valid-for-affirm? selected-affirm?)
             (utils/send-event-callback events/control-checkout-affirm-confirmation-submit)
             (utils/send-event-callback events/control-checkout-confirmation-submit
                                        {:place-order?             (or affirm-selected-but-not-valid?
                                                                       requires-additional-payment?)}))}
          (cond
            requires-additional-payment?
            [:div
             (ui/note-box
              {:color     "teal"
               :data-test "additional-payment-required-note"}
              [:.p2.navy
               "Please enter an additional payment method below for the remaining total on your order."])
             (om/build checkout-credit-card/component payment)]

            affirm-selected-but-not-valid?
            [:div
             (ui/note-box
              {:color     "teal"
               :data-test "alternative-payment-required-note"}
              [:.p2.navy
               "Affirm financing is not available for orders less than $50. To continue, please pay with a credit or debit card below."])
             (om/build checkout-credit-card/component payment)]

            :else
            nil)
          (summary/display-order-summary order
                                         {:read-only?             true
                                          :use-store-credit?      (not affirm-selected-and-order-valid?)
                                          :available-store-credit available-store-credit})
          (when selected-affirm?
            [:div.col-12.col-6-on-tb-dt.mx-auto (affirm-components/as-low-as-box {:amount      (:total order)
                                                                                  :middle-copy "Continue with Affirm below."})])
          [:div.col-12.col-6-on-tb-dt.mx-auto
           (if affirm-selected-and-order-valid?
             (ui/submit-button "Checkout with Affirm" {:spinning? (or saving-card? placing-order?) ;; We need a boolean for affirm request
                                                       :disabled? updating-shipping?
                                                       :data-test "confirm-affirm-form-submit"})
             (ui/submit-button "Place Order" {:spinning? (or saving-card? placing-order?)
                                              :disabled? updating-shipping?
                                              :data-test "confirm-form-submit"}))]]]]]))))

(defn ->affirm-address [{:keys [address1 address2 city state zipcode]}]
  {:line1   address1
   :line2   address2
   :city    city
   :state   state
   :zipcode zipcode
   :country "USA"})

(defn- absolute-url [& path]
  (apply str (.-protocol js/location) "//" (.-host js/location) path))

(defn ->affirm-line-item
  "Convert order line items to affirm schema with help of in-mem product/sku dbs

  Currently experiencing hard to track down bug around product-db lookup, throwing
  to get more information."
  [products skus {:as item :keys [product-name unit-price quantity] sku-id :sku}]
  (if-let [{:as product :keys [page/slug catalog/product-id]}
           (accessors.products/find-product-by-sku-id products sku-id)]
    {:display_name   product-name
     :sku            sku-id
     :unit_price     (* 100 unit-price)
     :qty            quantity
     :item_image_url (str "https:" (:src (images/cart-image (get skus sku-id))))
     :item_url       (absolute-url (products/path-for-sku product-id slug sku-id))}
    (throw (ex-info "Affirm line item building missing product" {:item item
                                                                 :product-keys (keys products)
                                                                 :sku-keys (keys skus)}))))

(defn promotion->affirm-discount [{:keys [amount promotion] :as promo}]
  (when (seq promo)
    {(:name promotion) {:discount_amount       (Math/abs (js/Math.round (* amount 100)))
                        :discount_display_name (:name promotion)}}))

(defn order->affirm [products skus order]
  (let [email              (-> order :user :email)
        product-line-items (orders/product-items order)
        line-items         (mapv (partial ->affirm-line-item products skus) product-line-items)
        promotions         (distinct (mapcat :applied-promotions product-line-items))]
    {:merchant {:user_confirmation_url        (absolute-url "/orders/" (:number order) "/affirm/" (url-encode (:token order)))
                :user_cancel_url              (absolute-url "/checkout/payment?error=affirm-incomplete")
                :user_confirmation_url_action "POST"
                :name                         "Mayvenn"}

     ;; You can include the full name instead
     ;; "full"  "John Doe"

     :shipping {:name         {:first (-> order :shipping-address :first-name)
                               :last  (-> order :shipping-address :last-name)}
                :address      (->affirm-address (:shipping-address order))
                :phone_number (-> order :shipping-address :phone)
                :email        email}

     :billing {:name         {:first (-> order :billing-address :first-name)
                              :last  (-> order :billing-address :last-name)}
               :address      (->affirm-address (:billing-address order))
               :phone_number (-> order :billing-address :phone)
               :email        email}

     :items line-items

     :discounts (->> promotions
                     (map promotion->affirm-discount)
                     (into {}))

     ;;user defined key/value pairs
     :metadata {}

     :order_id        (:number order)
     :shipping_amount (-> order orders/shipping-item :unit-price (* 100) js/Math.round)
     :tax_amount      (-> order :tax-total (* 100) js/Math.round)
     :total           (-> order :total (* 100) js/Math.round)}))

(defmethod effects/perform-effects events/control-checkout-affirm-confirmation-submit
  [_ _ _ _ app-state]
  (handle-message events/api-start {:xhr nil
                                    :request-key request-keys/affirm-place-order
                                    :request-id "affirm-checkout-request-id"}))

(defmethod trackings/perform-track events/control-checkout-affirm-confirmation-submit
  [_ event args app-state]
  (let [order (get-in app-state keypaths/order)]
    (stringer/track-event "customer-sent-to-affirm" {:order_number (:number order)
                                                     :order_total (:total order)}
                          events/stringer-tracked-sent-to-affirm)))

(defmethod effects/perform-effects events/stringer-tracked-sent-to-affirm [_ _ _ _ app-state]
  (affirm/checkout (order->affirm (get-in app-state keypaths/v2-products)
                                  (get-in app-state keypaths/v2-skus)
                                  (get-in app-state keypaths/order))))

(def ^:private affirm->error-path
  {"billing.address"                    ["billing-address" "address1"]
   "billing.phone_number.phone_number"  ["billing-address" "phone"]
   "billing.phone_number"               ["billing-address" "phone"]
   "billing.name.first"                 ["billing-address" "first-name"]
   "billing.name.last"                  ["billing-address" "last-name"]
   "billing.email.email"                ["billing-address" "email"]
   "billing.email"                      ["billing-address" "email"]
   "shipping.address"                   ["shipping-address" "address1"]
   "shipping.phone_number.phone_number" ["shipping-address" "phone"]
   "shipping.phone_number"              ["shipping-address" "phone"]
   "shipping.name.first"                ["shipping-address" "first-name"]
   "shipping.name.last"                 ["shipping-address" "last-name"]
   "shipping.email.email"               ["shipping-address" "email"]
   "shipping.email"                     ["shipping-address" "email"]})

(defn- affirm-field-error->std-error [{:keys [field message]} billing-is-shipping?]
  (let [path (affirm->error-path field)
        billing-error? (= "billing-address" (first path))
        billing->shipping-path #(assoc % 0 "shipping-address")]
    {:error-code    "affirm-input-error"
     :field-errors  (remove nil?
                            [(when (and billing-is-shipping? billing-error?)
                               {:path         (billing->shipping-path path)
                                :long-message message})
                             {:path         path
                              :long-message message}])
     :error-message "Affirm reported an error with your address, please correct them below"}))

(def non-input-affirm-std-error
  {:error-code "affirm-open-failure"
   :error-message "There was an issue authorizing your Affirm loan. Please check out again or use a different payment method."})

(def affirm-ui-std-error
  {:error-code "affirm-ui-error"
   :error-message "There was an issue authorizing your Affirm loan. Please check out again or use a different payment method."})

(defmethod effects/perform-effects events/affirm-checkout-error
  [_ _ {:keys [code] :as affirm-error} _ app-state]
  (handle-message events/api-end {:xhr nil
                                  :request-key request-keys/affirm-place-order
                                  :request-id "affirm-checkout-request-id"})
  (let [field-errors? (= "invalid_field" code)]
    (effects/redirect (if field-errors?
                        events/navigate-checkout-address
                        events/navigate-checkout-payment))
    (handle-later events/api-failure-errors
                  (if field-errors?
                    (affirm-field-error->std-error
                     affirm-error
                     (get-in app-state keypaths/checkout-bill-to-shipping-address))
                    non-input-affirm-std-error)
                  0)))

(defmethod effects/perform-effects events/affirm-ui-error-closed
  [_ _ affirm-error _ app-state]
  (handle-message events/api-end {:xhr nil
                                  :request-key request-keys/affirm-place-order
                                  :request-id "affirm-checkout-request-id"})
  (effects/redirect events/navigate-checkout-address)
  (handle-message events/api-failure-errors affirm-ui-std-error))

(defn query [data]
  (let [order (get-in data keypaths/order)]
    {:updating-shipping?           (utils/requesting? data request-keys/update-shipping-method)
     :saving-card?                 (checkout-credit-card/saving-card? data)
     :placing-order?               (or (utils/requesting? data request-keys/place-order)
                                       (utils/requesting? data request-keys/affirm-place-order))
     :selected-affirm?             (get-in data keypaths/order-cart-payments-affirm)
     :order-valid-for-affirm?      (affirm-components/valid-order-total? (:total order))
     :requires-additional-payment? (requires-additional-payment? data)
     :checkout-steps               (checkout-steps/query data)
     :products                     (get-in data keypaths/v2-products)
     :skus                         (get-in data keypaths/v2-skus)
     :order                        order
     :payment                      (checkout-credit-card/query data)
     :delivery                     (checkout-delivery/query data)
     :available-store-credit       (get-in data keypaths/user-total-available-store-credit)}))

(defn built-component [data opts]
  (let [query-data (query data)]
    (om/build component query-data opts)))
