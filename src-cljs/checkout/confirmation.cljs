(ns checkout.confirmation
  (:require [om.core :as om]
            [cemerick.url :refer [url-encode]]
            [sablono.core :refer [html]]
            [goog.string :as google-string]
            [storefront.components.money-formatters :as mf]
            [storefront.hooks.stringer :as stringer]
            [storefront.platform.messages :refer [handle-message handle-later]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.order-summary :as summary]
            [storefront.components.promotion-banner :as promotion-banner]
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
            [storefront.accessors.images :as images]
            [spice.core :as spice]
            [clojure.string :as string]
            [checkout.control-cart :as cart]
            [checkout.cart.items :as cart-items]
            [checkout.confirmation.summary :as confirmation-summary]))

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

(defn checkout-button [{:keys [affirm-order? spinning? disabled?]}]
  (if affirm-order?
    (ui/submit-button "Checkout with Affirm" {:spinning? spinning?
                                              :disabled? disabled?
                                              :data-test "confirm-affirm-form-submit"})
    (ui/submit-button "Place Order" {:spinning? spinning?
                                     :disabled? disabled?
                                     :data-test "confirm-form-submit"})))

(defn checkout-button-query [data]
  (let [order                   (get-in data keypaths/order)
        selected-affirm?        (get-in data keypaths/order-cart-payments-affirm)
        order-valid-for-affirm? (affirm-components/valid-order-total? (:total order))

        saving-card?   (checkout-credit-card/saving-card? data)
        placing-order? (or (utils/requesting? data request-keys/place-order)
                           (utils/requesting? data request-keys/affirm-place-order))]
    {:affirm-order?          (and selected-affirm? order-valid-for-affirm?)
     :second-checkout-button {:show?       (experiments/second-checkout-button? data)
                              :show-total? (experiments/second-checkout-button-total? data)
                              :order-total            (mf/as-money (:total order))
                              :total-quantity         (google-string/format
                                                       "Total (%s): "
                                                       (ui/pluralize (orders/product-quantity order) "item" "items"))}
     :disabled?              (utils/requesting? data request-keys/update-shipping-method)
     :spinning?              (or saving-card? placing-order?)}))

(defn second-checkout-button-component [{:keys [second-checkout-button affirm-order? disabled? spinning?]}]
  (let [{:keys [show? show-total? order-total total-quantity]} second-checkout-button]
    (when show?
     [:div.border-bottom.border-gray.pt2.pb5.mb4.hide-on-tb-dt
      (when show-total?
        [:div.h3.light.pb2
         total-quantity
         [:span.h2.medium order-total]])
      (checkout-button {:spinning?     spinning?
                        :disabled?     disabled?
                        :affirm-order? affirm-order?})])))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn ^:private display-freeinstall-line-item [{:keys [id price title thumbnail-image-fn] :as freeinstall-line-item-data}]
  [:div.clearfix.border-bottom.border-gray.py3
   [:a.left.mr1
    [:div.block.border.border-gray.rounded.hide-on-mb
     (thumbnail-image-fn 117)]
    [:div.block.border.border-gray.rounded.hide-on-tb-dt
     (thumbnail-image-fn 132)]]
   [:div.overflow-hidden
    [:div.ml1
     [:a.medium.titleize.h5 {:data-test (str "line-item-title-" id)}
      title]
     [:div.h6.mt1.line-height-1
      [:div.pyp2 {:data-test (str "line-item-price-ea-" id)}
       "Price Each: " (mf/as-money-without-cents price)]]]]])

(defn component
  [{:keys [available-store-credit
           checkout-steps
           payment delivery order
           skus
           selected-affirm?
           order-valid-for-affirm?
           requires-additional-payment?
           promotion-banner
           install-or-free-install-applied?
           freeinstall-line-item-data
           confirmation-summary
           checkout-button-data]}
   owner]
  (om/component
   (html
    (let [affirm-selected-but-not-valid?   (and (not order-valid-for-affirm?) selected-affirm?)
          affirm-selected-and-order-valid? (and selected-affirm? order-valid-for-affirm?)]
      [:div.container.p2
       (component/build promotion-banner/sticky-component promotion-banner nil)
       (om/build checkout-steps/component checkout-steps)

       [:form
        {:on-submit
         (if (and order-valid-for-affirm? selected-affirm?)
           (utils/send-event-callback events/control-checkout-affirm-confirmation-submit)
           (utils/send-event-callback events/control-checkout-confirmation-submit
                                      {:place-order? (or affirm-selected-but-not-valid?
                                                         requires-additional-payment?)}))}

        [:.clearfix.mxn3
         [:.col-on-tb-dt.col-6-on-tb-dt.px3
          (second-checkout-button-component checkout-button-data)

          [:.h3.left-align "Order Summary"]

          [:div.my2
           {:data-test "confirmation-line-items"}
           (summary/display-line-items (orders/product-items order) skus)
           (when freeinstall-line-item-data
             (display-freeinstall-line-item freeinstall-line-item-data))]]

         [:.col-on-tb-dt.col-6-on-tb-dt.px3
          (om/build checkout-delivery/component delivery)
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
          (if confirmation-summary
            (component/build confirmation-summary/component confirmation-summary {})
            (summary/display-order-summary order
                                           {:read-only?             true
                                            :use-store-credit?      (and (not affirm-selected-and-order-valid?)
                                                                         (not install-or-free-install-applied?))
                                            :available-store-credit available-store-credit}))
          (when selected-affirm?
            [:div.col-12.col-6-on-tb-dt.mx-auto (affirm-components/as-low-as-box {:amount      (:total order)
                                                                                  :middle-copy "Continue with Affirm below."})])
          [:div.col-12.col-6-on-tb-dt.mx-auto
           (checkout-button checkout-button-data)]]]]]))))

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
    (throw (ex-info "Affirm line item building missing product"
                    {:item item
                     :product-keys (if-let [product-keys (keys products)]
                                     product-keys
                                     :no-products-in-app-state)
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

(def ^:private std-affirm-error-message
  "There was an issue authorizing your Affirm loan. Please check out again or use a different payment method.")

(def non-input-affirm-std-error
  {:error-code "affirm-open-failure"
   :error-message std-affirm-error-message})

(def affirm-ui-std-error
  {:error-code "affirm-ui-error"
   :error-message std-affirm-error-message})

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
    {:selected-affirm?                 (get-in data keypaths/order-cart-payments-affirm)
     :order-valid-for-affirm?          (affirm-components/valid-order-total? (:total order))
     :requires-additional-payment?     (requires-additional-payment? data)
     :promotion-banner                 (promotion-banner/query data)
     :checkout-steps                   (checkout-steps/query data)
     :products                         (get-in data keypaths/v2-products)
     :skus                             (get-in data keypaths/v2-skus)
     :order                            order
     :payment                          (checkout-credit-card/query data)
     :delivery                         (checkout-delivery/query data)
     :install-or-free-install-applied? (or (orders/freeinstall-applied? order)
                                           (orders/install-applied? order))
     :available-store-credit           (get-in data keypaths/user-total-available-store-credit)
     :checkout-button-data             (checkout-button-query data)
     :confirmation-summary             (confirmation-summary/query data)
     :freeinstall-line-item-data       (cart-items/freeinstall-line-item-query data)}))

(defn built-component [data opts]
  (let [query-data (query data)]
    (om/build component query-data opts)))
