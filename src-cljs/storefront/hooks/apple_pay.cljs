(ns storefront.hooks.apple-pay
  (:require [storefront.api :as api]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [clojure.string :as string]
            [storefront.keypaths :as keypaths]))


(defn verify-eligible [app-state]
  (when (get-in app-state keypaths/loaded-stripe-v3)
    (let [api-id {:request-key request-keys/stripe-apple-pay-availability
                  :request-id (str (random-uuid))}]
      (handle-message events/api-start api-id)
      (js/Stripe.applePay.checkAvailability (fn [available?]
                                              (handle-message events/api-end api-id)
                                              (handle-message events/apple-pay-availability {:available? available?}))))))

(defn ^:private product-items->apple-line-items [order]
  (map (fn [{:keys [quantity unit-price] :as item}]
         {:label (str (products/product-title item) " - QTY " quantity)
          :amount (* quantity unit-price)})
       (orders/product-items order)))

(defn ^:private adjustment->apple-line-item [{:keys [name price]}]
  {:label name
   :amount price})

(defn ^:private adjustments->apple-line-items [order]
  (map adjustment->apple-line-item (:adjustments order)))

(defn ^:private shipping->apple-line-item [order]
  (let [{:keys [quantity unit-price]} (orders/shipping-item order)]
    (adjustment->apple-line-item {:name "Shipping"
                                  :price (* quantity unit-price)})))

(defn ^:private tax->apple-line-item [order]
  (adjustment->apple-line-item (orders/tax-adjustment order)))

(defn ^:private waiter->apple-shipping-methods [shipping-methods]
  (for [{:keys [name price sku]} shipping-methods]
    {:label name
     :detail (as-money price)
     :amount price
     :identifier sku}))

(defn ^:private order->apple-line-items [order]
  (concat (product-items->apple-line-items order)
          (adjustments->apple-line-items order)
          [(shipping->apple-line-item order)
           (tax->apple-line-item order)]))

(defn ^:private order->apple-total [order]
  {:label "Mayvenn Hair"
   :amount (:total order)})


(defn ^:private find-state-abbr [states state-name]
  (let [lower-cased-state-name (string/lower-case (string/trim (str state-name)))]
    (->> states
         (filter #(or (= lower-cased-state-name (string/lower-case (:name %)))
                      (= lower-cased-state-name (string/lower-case (:abbr %)))))
         first
         :abbr)))


(defn ^:private apple->waiter-shipping [{:keys [locality givenName familyName phoneNumber postalCode addressLines administrativeArea]} state-name->abbr]
  {:address1 (first addressLines)
   :address2 (second addressLines)
   :city locality
   :first-name givenName
   :last-name familyName
   :phone phoneNumber
   :state (state-name->abbr administrativeArea)
   :zipcode postalCode})

(defn ^:private card->waiter-address [card shipping-contact state-name->abbr]
  (let [{:keys [address_city address_line1 address_line2 address_state address_zip]} (js->clj card :keywordize-keys true)
        {:keys [givenName familyName phoneNumber]} shipping-contact]
    {:address1 address_line1
     :address2 address_line2
     :city address_city
     :first-name givenName
     :last-name familyName
     :phone phoneNumber
     :state (state-name->abbr address_state)
     :zipcode address_zip}))

(defn ^:private charge-apple-pay [order session-id utm-params state-name->abbr result complete]
  (let [shipping-contact (js->clj (.-shippingContact result) :keywordize-keys true)
        shipping-method  (js->clj (.-shippingMethod result) :keywordize-keys true)]
    (api/checkout {:number              (:number order)
                   :token               (:token order)
                   :shipping-address    (apple->waiter-shipping shipping-contact state-name->abbr)
                   :billing-address     (card->waiter-address (.. result -token -card) shipping-contact state-name->abbr)
                   :email               (:emailAddress shipping-contact)
                   :shipping-method-sku (:identifier shipping-method)
                   :cart-payments       {:apple-pay {:source (.. result -token -id)}}
                   :session-id          session-id
                   :utm-params          utm-params}
                  (fn [response]
                    (handle-message events/apple-pay-end)
                    (complete js/ApplePaySession.STATUS_SUCCESS))
                  (fn [error]
                    (handle-message events/apple-pay-end)
                    (complete (if (some (or (some-> error :response :body :details) {}) [:email :shipping-address.phone :billing-address.phone])
                                js/ApplePaySession.STATUS_INVALID_SHIPPING_CONTACT
                                js/ApplePaySession.STATUS_FAILURE))))))

(defn error->shipping-contact-status [error]
  (let [bad-shipping? (and error
                           (some (or (some-> error :response :body :details) {})
                                 [:shipping-address.city :shipping-address.state :shipping-address.zipcode]))]
    (cond
      bad-shipping? js/ApplePaySession.STATUS_INVALID_SHIPPING_POSTAL_ADDRESS
      error         js/ApplePaySession.STATUS_FAILURE
      :else         js/ApplePaySession.STATUS_SUCCESS)))

(defn error->shipping-method-status [error]
  (cond
    error js/ApplePaySession.STATUS_FAILURE
    :else js/ApplePaySession.STATUS_SUCCESS))

(defn complete-shipping-contact-selection [session order error]
  (.completeShippingContactSelection session
                                     (error->shipping-contact-status error)
                                     (clj->js []) ;; no new shipping methods
                                     (-> order order->apple-total clj->js)
                                     (-> order order->apple-line-items clj->js)))

(defn complete-shipping-method-selection [session order error]
  (.completeShippingMethodSelection session
                                    (error->shipping-method-status error)
                                    (-> order order->apple-total clj->js)
                                    (-> order order->apple-line-items clj->js)))

(defn estimate-params [order states event]
  (let [shipping-contact (js->clj (.-shippingContact event) :keywordize-keys true)
        shipping-method  (js->clj (.-shippingMethod event) :keywordize-keys true)]
    {:number              (:number order)
     :token               (:token order)
     :shipping-address    (if shipping-contact
                            ;; At this stage apple pay only provides us with these
                            ;; keys: administrativeArea country locality and postalCode
                            (apple->waiter-shipping shipping-contact (partial find-state-abbr states))
                            (:shipping-address order))
     :shipping-method-sku (:identifier shipping-method (:sku (orders/shipping-item order)))}))

(defn on-apple-pay-updated [session order-atom states completion-fn event]
  (let [order  @order-atom
        params (estimate-params order states event)]
    (api/apple-pay-estimate params
                            (fn [updated-order]
                              (reset! order-atom updated-order)
                              (completion-fn session updated-order nil))
                            (fn [error]
                              (completion-fn session order error)))))

(defn begin [order session-id utm-params shipping-methods states]
  (let [modified-order   (atom order) shipping-methods (waiter->apple-shipping-methods shipping-methods)
        payment-request  {:countryCode                   "US"
                          :currencyCode                  "USD"
                          :requiredBillingContactFields  ["name" "postalAddress"]
                          :requiredShippingContactFields (if (:user order)
                                                           ["name" "phone" "postalAddress"]
                                                           ["name" "phone" "email" "postalAddress"])
                          :shippingMethods               shipping-methods
                          :lineItems                     (order->apple-line-items order)
                          :total                         (order->apple-total order)}
        session          (js/Stripe.applePay.buildSession (clj->js payment-request)
                                                          (partial charge-apple-pay order session-id utm-params (partial find-state-abbr states)))]
    (set! (.-oncancel session) (fn [_] (handle-message events/apple-pay-end)))
    (set! (.-onshippingcontactselected session) (partial on-apple-pay-updated session modified-order states complete-shipping-contact-selection))
    (set! (.-onshippingmethodselected session) (partial on-apple-pay-updated session modified-order states complete-shipping-method-selection))
    (.begin session)))

