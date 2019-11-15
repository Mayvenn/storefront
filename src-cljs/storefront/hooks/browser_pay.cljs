(ns storefront.hooks.browser-pay
  (:require [storefront.api :as api]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [clojure.string :as string]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.line-items :as line-items]))

(defn ^:private product-items->browser-pay-line-items [order]
  (map (fn [{:keys [quantity unit-price] :as item}]
         {:label (str (products/product-title item) " - QTY " quantity)
          :amount (int (* quantity unit-price 100))})
       (orders/product-items order)))

(defn ^:private adjustment->browser-pay-line-item [{:keys [name price]}]
  {:label name
   :amount (int (* 100 price))})

(defn ^:private adjustments->browser-pay-line-items [order]
  (map adjustment->browser-pay-line-item (:adjustments order)))

(defn ^:private shipping->browser-pay-line-item [order]
  (let [{:keys [quantity unit-price]} (orders/shipping-item order)]
    (adjustment->browser-pay-line-item {:name "Shipping"
                                        :price (int (* quantity unit-price))})))

(defn ^:private tax->browser-pay-line-item [order]
  (adjustment->browser-pay-line-item (orders/tax-adjustment order)))

(defn ^:private order->browser-pay-line-items [order]
  (concat (product-items->browser-pay-line-items order)
          (adjustments->browser-pay-line-items order)
          [(shipping->browser-pay-line-item order)
           (tax->browser-pay-line-item order)]))

(defn ^:private order->browser-pay-total [order]
  {:label "Order Total"
   :amount (int (* 100 (:total order)))})

(defn ^:private split-name [name]
  (string/split name #" " 2))

(defn ^:private find-state-abbr [states state-name]
  (let [lower-cased-state-name (string/lower-case (string/trim (str state-name)))]
    (->> states
         (filter #(or (= lower-cased-state-name (string/lower-case (:name %)))
                      (= lower-cased-state-name (string/lower-case (:abbr %)))))
         first
         :abbr)))

(defn ^:private browser-pay->waiter-shipping-address [{:keys [city recipient phone postalCode addressLine region]} state-name->abbr]
  (let [[first-name last-name] (split-name recipient)]
    {:address1 (first addressLine)
     :address2 (second addressLine)
     :city city
     :first-name first-name
     :last-name last-name
     :phone phone
     :state (state-name->abbr region)
     :zipcode postalCode}))

(defn ^:private card->waiter-billing-address [card shipping-contact state-name->abbr]
  (let [{:keys [address_city address_line1 address_line2 address_state address_zip name]} (js->clj card :keywordize-keys true)
        {:keys [phone]} shipping-contact
        [first-name last-name] (split-name name)]
    {:address1 address_line1
     :address2 address_line2
     :city address_city
     :first-name first-name
     :last-name last-name
     :phone phone
     :state (state-name->abbr address_state)
     :zipcode address_zip}))

(defn ^:private waiter->browser-pay-error-status [error]
  (cond
    (some (or (some-> error :response :body :details) {}) [:shipping-address.phone
                                                           :shipping-address.first-name
                                                           :shipping-address.last-name
                                                           :shipping-address.address1
                                                           :shipping-address.address2
                                                           :shipping-address.city
                                                           :shipping-address.state
                                                           :shipping-address.zipcode])
    "invalid_shipping_address"

    (some (or (some-> error :response :body :details) {}) [:billing-address.first-name
                                                           :billing-address.last-name])
    "invalid_payer_name"

    (some (or (some-> error :response :body :details) {}) [:billing-address.phone])
    "invalid_payer_phone"

    (some (or (some-> error :response :body :details) {}) [:user.email])
    "invalid_payer_email"

    :else
    "fail"))

(defn ^:private waiter->browser-pay-shipping-method [{:keys [name price sku]}]
  {:label name
   :detail (as-money price)
   :amount (int (* 100 price))
   :id sku})

(defn ^:private waiter->browser-pay-shipping-methods [shipping-methods]
  (map waiter->browser-pay-shipping-method shipping-methods))

(defn ^:private sku->shipping-method [shipping-methods sku]
  (first (filter (comp #{sku} :sku) shipping-methods)))

(defn ^:private waiter->browser-pay-updated-shipping-option [order shipping-methods]
  {:displayItems      (order->browser-pay-line-items order)
   :total             {:label  "Order Total"
                       :amount (int (* 100 (:total order)))}})

(defn browser-pay->waiter-order [order states event]
  (let [shipping-contact (js->clj (.-shippingAddress event) :keywordize-keys true)
        shipping-method  (js->clj (.-shippingOption event) :keywordize-keys true)
        shipping-method-sku (:sku (orders/shipping-item order))]
    (merge {:number (:number order)
            :token  (:token order)}
           (cond
             shipping-contact
             {:shipping-address (browser-pay->waiter-shipping-address shipping-contact (partial find-state-abbr states))}

             (:shipping-address order)
             {:shipping-address (:shipping-address order)}

             :else
             nil)

           (cond
             shipping-method
             {:shipping-method-sku (:id shipping-method)}

             shipping-method-sku
             {:shipping-method-sku shipping-method-sku}

             :else
             nil))))

(defn locally-switch-shipment-line [shipment selected-shipping-method]
  (assoc shipment :line-items
         (conj (orders/product-items-for-shipment shipment)
               (merge (->> shipment :line-items (filter line-items/shipping-method?) first)
                      {:product-name (:name selected-shipping-method)
                       :sku          (:sku selected-shipping-method)
                       :unit-price   (:price selected-shipping-method)
                       :variant-name (:name selected-shipping-method)}))))

(defn locally-update-shipping-methods [order shipping-methods selected-shipping-option]
  (let [original-shipping-method (sku->shipping-method shipping-methods (:sku (orders/shipping-item order)))
        selected-shipping-method (sku->shipping-method shipping-methods (:id selected-shipping-option))]
    (-> order
        (update :total + (:price selected-shipping-method) (- (:price original-shipping-method)))
        (update-in [:shipments 0] locally-switch-shipment-line selected-shipping-method))))

(defn on-shipping-address-updated [ev order-atom shipping-methods states]
  (let [order  @order-atom
        params (browser-pay->waiter-order order states ev)]
    (api/browser-pay-estimate params
                              (fn [updated-order]
                                (reset! order-atom updated-order)
                                (let [order @order-atom]
                                  (.updateWith ev (clj->js {:status "success"
                                                            :displayItems      (order->browser-pay-line-items order)
                                                            :shippingOptions   (waiter->browser-pay-shipping-methods shipping-methods)
                                                            :total             {:label  "Order Total"
                                                                                :amount (int (* 100 (:total order)))}}))))
                              (fn [error]
                                (.updateWith ev (clj->js {:status (waiter->browser-pay-error-status error)}))))))

(defn on-shipping-option-updated [ev order-atom shipping-methods states]
  (let [order          @order-atom
        params         (browser-pay->waiter-order order states ev)
        success-params (fn [order]
                         (clj->js
                          {:status "success"
                           :displayItems (order->browser-pay-line-items order)
                           :total        {:label  "Order Total"
                                          :amount (int (* 100 (:total order)))}}))]
    (if (or (.-shippingAddress ev) (:shipping-address order))
      (api/browser-pay-estimate params
                                (fn [updated-order]
                                  (reset! order-atom updated-order)
                                  (.updateWith ev (success-params @order-atom)))
                                (fn [error]
                                  (.updateWith ev (clj->js {:status (waiter->browser-pay-error-status error)}))))
      (do
        (swap! order-atom locally-update-shipping-methods shipping-methods (js->clj (.-shippingOption ev) :keywordize-keys true))
        (.updateWith ev (success-params @order-atom))))))

(defn ^:private charge [order session-id utm-params ev states]
  (let [shipping-contact (js->clj (.-shippingAddress ev) :keywordize-keys true)
        shipping-method  (js->clj (.-shippingOption ev) :keywordize-keys true)
        state-name->abbr (partial find-state-abbr states)]
    (api/checkout (merge (browser-pay->waiter-order order states ev)
                         {:billing-address     (card->waiter-billing-address (.. ev -token -card) shipping-contact state-name->abbr)
                          :email               (.-payerEmail ev)
                          :cart-payments       {:apple-pay {:source (.. ev -token -id)}}
                          :session-id          session-id
                          :utm-params          utm-params})
                  (fn [response] (.complete ev "success"))
                  (fn [error] (.complete ev (waiter->browser-pay-error-status error))))))

(defn payment-request-button [order shipping-methods payment-request]
  (.create (js/stripe.elements)
           "paymentRequestButton"
           (clj->js {:paymentRequest payment-request})))

(defn payment-request [order session-id utm-params states shipping-methods]
  (let [order-with-default-shipping (locally-update-shipping-methods order shipping-methods {:id (:sku (first shipping-methods))})
        modified-order              (atom order-with-default-shipping)
        request                     (js/stripe.paymentRequest
                                     (clj->js {:country           "US"
                                               :currency          "usd"
                                               :displayItems      (order->browser-pay-line-items order-with-default-shipping)
                                               :requestPayerName  true
                                               :requestPayerEmail true
                                               :requestPayerPhone true
                                               :requestShipping   true
                                               :shippingOptions   (waiter->browser-pay-shipping-methods shipping-methods)
                                               :total             {:label  "Order Total"
                                                                   :amount (int (* 100 (:total order-with-default-shipping)))}}))]
    (-> request
        (.canMakePayment)
        (.then (fn [ev]
                 (when ev
                   (.mount (payment-request-button order-with-default-shipping shipping-methods request)
                           "#request-payment-button-container")))))
    (.on request "cancel" (fn [ev]
                            (.update request (clj->js {:displayItems    (order->browser-pay-line-items order-with-default-shipping)
                                                       :shippingOptions (waiter->browser-pay-shipping-methods shipping-methods)
                                                       :total           {:label  "Order Total"
                                                                         :amount (int (* 100 (:total order-with-default-shipping)))}}))
                            (reset! modified-order order-with-default-shipping)))
    (.on request "shippingaddresschange" (fn [ev] (js/console.log ev) (on-shipping-address-updated ev modified-order shipping-methods states)))
    (.on request "shippingoptionchange" (fn [ev] (js/console.log ev) (on-shipping-option-updated ev modified-order shipping-methods states)))
    (.on request "token" (fn [ev] (charge order-with-default-shipping session-id utm-params ev states)))))
