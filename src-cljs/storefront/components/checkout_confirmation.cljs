(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [cemerick.url :refer [url-encode]]
            [sablono.core :refer-macros [html]]
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
            [storefront.routes :as routes]
            [storefront.accessors.products :refer [medium-img]]
            [catalog.products :as products]
            [storefront.hooks.affirm :as affirm]
            [storefront.components.affirm :as affirm-components]
            [storefront.component :as component]))

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

(defn old-component
  [{:keys [available-store-credit
           checkout-steps
           payment delivery order
           placing-order?
           products
           requires-additional-payment?
           saving-card?
           updating-shipping?]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (om/build checkout-steps/component checkout-steps)

     [:.clearfix.mxn3
      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       [:.h3.left-align "Order Summary"]

       [:div.my2
        {:data-test "confirmation-line-items"}
        (summary/display-line-items (orders/product-items order) products)]]

      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       (om/build checkout-delivery/component delivery)
       [:form
        {:on-submit
         (utils/send-event-callback events/control-checkout-confirmation-submit
                                    {:place-order? requires-additional-payment?})}
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (summary/display-order-summary order
                                       {:read-only?             true
                                        :use-store-credit?      true
                                        :available-store-credit available-store-credit})
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (ui/submit-button "Place Order" {:spinning? (or saving-card? placing-order?)
                                          :disabled? updating-shipping?
                                          :data-test "confirm-form-submit"})]]]]])))

(defn component
  [{:keys [affirm-payment?
           available-store-credit
           checkout-steps
           payment delivery order
           placing-order?
           products
           requires-additional-payment?
           saving-card?
           updating-shipping?]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (om/build checkout-steps/component checkout-steps)

     [:.clearfix.mxn3
      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       [:.h3.left-align "Order Summary"]

       [:div.my2
        {:data-test "confirmation-line-items"}
        (summary/display-line-items (orders/product-items order) products)]]

      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       (om/build checkout-delivery/component delivery)
       [:form
        {:on-submit
         (utils/send-event-callback events/control-checkout-affirm-confirmation-submit)}
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (summary/display-order-summary order
                                       {:read-only?             true
                                        :use-store-credit?      false
                                        :available-store-credit available-store-credit})
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (affirm-components/as-low-as-box {:amount (:total order)})]
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (ui/submit-button "Checkout with Affirm" {:spinning? (or saving-card? placing-order?) ;; We need a boolean for affirm request
                                                   :disabled? updating-shipping?
                                                   :data-test "confirm-affirm-form-submit"})]]]]])))

(defn ->affirm-address [{:keys [address1 address2 city state zipcode]}]
  {:line1   address1
   :line2   address2
   :city    city
   :state   state
   :zipcode zipcode
   :country "USA"})

(defn- absolute-url [& path]
  (apply str (.-protocol js/location) "//" (.-host js/location) path))

(defn ->affirm-line-item [products {:keys [product-id product-name sku unit-price quantity]}]
  (let [{:keys [images slug]} (get products product-id)]
    {:display_name   product-name
     :sku            sku
     :unit_price     (* 100 unit-price)
     :qty            quantity
     :item_image_url (str "https:" (:src (medium-img products product-id)))
     :item_url       (absolute-url (products/path-for-sku product-id slug sku))}))

(defn promotion->affirm-discount [{:keys [amount promotion] :as promo}]
  (when (seq promo)
    {(:name promotion) {:discount_amount       (Math/abs amount)
                        :discount_display_name (:name promotion)}}))

(defn order->affirm [products order]
  (let [email         (-> order :user :email)
        product-items (orders/product-items order)
        line-items    (mapv (partial ->affirm-line-item products) product-items)
        promotions    (distinct (mapcat :applied-promotions product-items))]
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
     :shipping_amount (-> order orders/shipping-item :unit-price (* 100) int)
     :tax_amount      (-> order :tax-total (* 100) int)
     :total           (-> order :total (* 100) int)}))

(defmethod effects/perform-effects events/control-checkout-affirm-confirmation-submit
  [_ _ _ _ app-state]
  (affirm/checkout (order->affirm (get-in app-state keypaths/products)
                                  (get-in app-state keypaths/order))))

(defn query [data]
  (let [order (get-in data keypaths/order)]
    {:updating-shipping?           (utils/requesting? data request-keys/update-shipping-method)
     :saving-card?                 (checkout-credit-card/saving-card? data)
     :placing-order?               (utils/requesting? data request-keys/place-order)
     :requires-additional-payment? (requires-additional-payment? data)
     :affirm-payment?              (get-in data keypaths/order-cart-payments-affirm)
     :checkout-steps               (checkout-steps/query data)
     :products                     (get-in data keypaths/products)
     :order                        order
     :payment                      (checkout-credit-card/query data)
     :delivery                     (checkout-delivery/query data)
     :available-store-credit       (get-in data keypaths/user-total-available-store-credit)}))

(defn built-component [data opts]
  (om/build (if (and (experiments/affirm? data)
                     (get-in data keypaths/order-cart-payments-affirm))
              component
              old-component)
            (query data)
            opts))
