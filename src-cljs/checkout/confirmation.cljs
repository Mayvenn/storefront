(ns checkout.confirmation
  (:require [adventure.checkout.cart.items :as adventure-cart-items]
            [adventure.keypaths]
            [catalog.images :as catalog-images]
            [checkout.cart.items :as cart-items]
            [checkout.confirmation.summary :as confirmation-summary]
            [checkout.templates.item-card :as item-card]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.api :as api]
            [storefront.config :as config]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.component :as component]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :as mf]
            [storefront.components.order-summary :as summary]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.trackings :as trackings]
            [storefront.hooks.quadpay :as quadpay]
            [storefront.hooks.stringer :as stringer]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]))

(defn requires-additional-payment?
  [data]
  (let [no-stripe-payment?  (nil? (get-in data keypaths/order-cart-payments-stripe))
        store-credit-amount (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0)
        order-total         (get-in data keypaths/order-total)]
    (and
     ;; stripe can charge any amount
     no-stripe-payment?
     ;; is total covered by remaining store-credit?
     (> order-total store-credit-amount)
     (contains? (get-in data keypaths/checkout-selected-payment-methods) :store-credit))))

(defn checkout-button
  [selected-quadpay? {:keys [spinning? disabled?]}]
  (ui/submit-button (if selected-quadpay?
                      "Place Order with QuadPay"
                      "Place Order")
                    {:spinning? spinning?
                     :disabled? disabled?
                     :data-test "confirm-form-submit"}))

(defn checkout-button-query
  [data]
  (let [saving-card?   (checkout-credit-card/saving-card? data)
        has-order?     (get-in data keypaths/order)
        placing-order? (utils/requesting? data request-keys/place-order)
        placing-quadpay-order? (utils/requesting? data request-keys/update-cart-payments)]
    {:disabled? (or (utils/requesting? data request-keys/update-shipping-method)
                    (not has-order?))
     :spinning? (or saving-card? placing-order? placing-quadpay-order?)}))

(defmethod effects/perform-effects events/control-checkout-quadpay-confirmation-submit
  [_ _ _ app-state]
  (let [current-uri          (get-in app-state keypaths/navigation-uri)
        order-number         (get-in app-state keypaths/order-number)
        order-token          (get-in app-state keypaths/order-token)
        store-slug           (get-in app-state keypaths/store-slug)
        affiliate-stylist-id (some-> app-state
                                     (get-in keypaths/cookie)
                                     cookie-jar/retrieve-affiliate-stylist-id
                                     :affiliate-stylist-id
                                     spice/parse-int)]
    (api/update-cart-payments
     (get-in app-state keypaths/session-id)
     {:order {:number        order-number
              :token         order-token
              :cart-payments {:quadpay
                              {:status-url (str config/api-base-url "/hooks/quadpay_notifications"
                                                (when (and affiliate-stylist-id
                                                           (contains? #{"shop" "freeinstall"} store-slug))
                                                  (str "?affiliate-stylist-id=" affiliate-stylist-id)))
                               :return-url
                               (str (assoc current-uri
                                           :path (str "/orders/" order-number "/quadpay" )
                                           :query {:order-token order-token}))
                               :cancel-url (str (assoc current-uri :path "/cart?error=quadpay"))}}}}
     (fn [order]
       (messages/handle-message events/api-success-update-order-proceed-to-quadpay {:order order})))))

(defmethod trackings/perform-track events/api-success-update-order-proceed-to-quadpay
  [_ _ args app-state]
  (let [order-number (get-in app-state keypaths/order-number)
        store-slug   (get-in app-state keypaths/store-slug)
        order-total  (get-in app-state keypaths/order-total)
        redirect-url (-> (get-in app-state keypaths/order) :cart-payments :quadpay :redirect-url)]
    (stringer/track-event "customer-sent-to-quadpay"
                          {:order_number order-number
                           :store_slug   store-slug
                           :order_total  order-total}
                          events/external-redirect-quadpay-checkout
                          {:quadpay-redirect-url redirect-url})))

(defn component
  [{:keys [available-store-credit
           checkout-steps
           payment
           delivery
           order
           items
           requires-additional-payment?
           promotion-banner
           install-or-free-install-applied?
           confirmation-summary
           checkout-button-data
           selected-quadpay?
           loaded-quadpay?]}
   owner]
  (component/create
   [:div.container.p2
    (component/build promotion-banner/sticky-component promotion-banner nil)
    (component/build checkout-steps/component checkout-steps nil)

    [:form
     {:on-submit
      (if selected-quadpay?
        (utils/send-event-callback events/control-checkout-quadpay-confirmation-submit)
        (utils/send-event-callback events/control-checkout-confirmation-submit
                                   {:place-order? requires-additional-payment?}))}

     [:.clearfix.mxn3
      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       [:.h3.left-align "Order Summary"]

       [:div.my2
        {:data-test "confirmation-line-items"}
        (component/build item-card/component items nil)]]

      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       (component/build checkout-delivery/component delivery nil)
       (when requires-additional-payment?
         [:div
          (ui/note-box
           {:color     "teal"
            :data-test "additional-payment-required-note"}
           [:.p2.navy
            "Please enter an additional payment method below for the remaining total on your order."])
          (component/build checkout-credit-card/component payment nil)])
       (if confirmation-summary
         (component/build confirmation-summary/component confirmation-summary {})
         (summary/display-order-summary order
                                        {:read-only?             true
                                         :use-store-credit?      (not install-or-free-install-applied?)
                                         :available-store-credit available-store-credit}))
       (component/build quadpay/component
                        {:show?       (and selected-quadpay? loaded-quadpay?)
                         :order-total (:total order)
                         :directive   [:div.flex.justify-center.items-center
                                       "Continue with"
                                       [:div.mx1 {:style {:width "70px" :height "14px"}}
                                        svg/quadpay-logo]
                                       "below."]}
                        nil)

       [:div.col-12.col-6-on-tb-dt.mx-auto
        (checkout-button selected-quadpay? checkout-button-data)]]]]]))

(defn item-card-query
  [data]
  (let [order               (get-in data keypaths/order)
        skus                (get-in data keypaths/v2-skus)
        facets              (maps/index-by :facet/slug (get-in data keypaths/v2-facets))
        color-options->name (->> facets
                                 :hair/color
                                 :facet/options
                                 (maps/index-by :option/slug)
                                 (maps/map-values :option/name))]
    {:items (mapv (fn [{sku-id :sku :as line-item}]
                    (let [sku   (get skus sku-id)
                          price (:unit-price line-item)]
                      {:react/key                 (str (:id line-item)
                                                       "-"
                                                       (:catalog/sku-id sku)
                                                       "-"
                                                       (:quantity line-item))
                       :circle/id                 (str "line-item-length-" sku-id)
                       :circle/value              (-> sku :hair/length first (str "”"))
                       :image/id                  (str "line-item-img-" (:catalog/sku-id sku))
                       :image/value               (->> sku (catalog-images/image "cart") :ucare/id)
                       :title/id                  (str "line-item-title-" sku-id)
                       :title/value               (or (:product-title line-item)
                                                      (:product-name line-item))
                       :detail-top-left/id        (str "line-item-color-" sku-id)
                       :detail-top-left/value     (-> sku :hair/color first color-options->name str)
                       :detail-bottom-right/id    (str "line-item-price-ea-" sku-id)
                       :detail-bottom-right/value (str (mf/as-money-without-cents price) " ea")
                       :detail-bottom-left/id     (str "line-item-quantity-" sku-id)
                       :detail-bottom-left/value  (str "Qty " (:quantity line-item))}))
                  (orders/product-items order))}))

(defn adventure-component
  [{:keys [available-store-credit
           items
           checkout-steps
           payment delivery order
           requires-additional-payment?
           promotion-banner
           install-or-free-install-applied?
           confirmation-summary
           checkout-button-data
           servicing-stylist
           selected-quadpay?
           loaded-quadpay?
           order]}
   owner]
  (component/create
   [:div.container.p2
    (component/build promotion-banner/sticky-component promotion-banner nil)
    (component/build checkout-steps/component checkout-steps nil)

    (if order
      [:form
       {:on-submit
        (when-not (or (:disabled? checkout-button-data) (:spinning? checkout-button-data))
          (if selected-quadpay?
            (utils/send-event-callback events/control-checkout-quadpay-confirmation-submit)
            (utils/send-event-callback events/control-checkout-confirmation-submit
                                       {:place-order? requires-additional-payment?})))}

       [:.clearfix.mxn3
        [:.col-on-tb-dt.col-6-on-tb-dt.px3
         [:.h3.left-align "Order Summary"]

         [:div.my2
          {:data-test "confirmation-line-items"}
          (component/build item-card/component items nil)]]

        [:.col-on-tb-dt.col-6-on-tb-dt.px3
         (component/build checkout-delivery/component delivery nil)
         (when requires-additional-payment?
           [:div
            (ui/note-box
             {:color     "teal"
              :data-test "additional-payment-required-note"}
             [:.p2.navy
              "Please enter an additional payment method below for the remaining total on your order."])
            (component/build checkout-credit-card/component payment nil)])
         (if confirmation-summary
           (component/build confirmation-summary/component confirmation-summary {})
           (summary/display-order-summary order
                                          {:read-only?             true
                                           :use-store-credit?      (not install-or-free-install-applied?)
                                           :available-store-credit available-store-credit}))

         (component/build quadpay/component
                          {:show?       (and selected-quadpay? loaded-quadpay?)
                           :order-total (:total order)
                           :directive   [:div.flex.justify-center.items-center
                                         "Continue with"
                                         [:div.mx1 {:style {:width "70px" :height "14px"}}
                                          svg/quadpay-logo]
                                         "below."]}
                          nil)

         [:div.h5.my4.center.col-10.mx-auto.line-height-3
          (if-let [servicing-stylist-name (stylists/->display-name servicing-stylist)]
            (str "You’ll be connected with " servicing-stylist-name " after checkout.")
            "You’ll be able to select your Certified Mayvenn Stylist after checkout.")]
         [:div.col-12.col-6-on-tb-dt.mx-auto
          (checkout-button selected-quadpay? checkout-button-data)]]]]
      [:div.py6.h2
       [:div.py4 (ui/large-spinner {:style {:height "6em"}})]
       [:h2.center.navy "Processing your order..."]])]))

(defn ^:private freeinstall-line-item-data->item-card
  [{:keys [id title detail thumbnail-image price]}]
  {:react/key              (str "freeinstall-line-item-" id)
   :title/value            title
   :title/id               "line-item-title-freeinstall"
   :detail-top-left/id     "freeinstall-details"
   :detail-top-left/value  detail
   :image/id               "freeinstall-needle-thread"
   :image/value            thumbnail-image
   :detail-top-right/id    (str "line-item-price-freeinstall")
   :detail-top-right/opts  {:class "flex items-end justify-end"}
   :detail-top-right/value (mf/as-money-without-cents price)})

(defn query
  [data]
  (let [order                      (get-in data keypaths/order)
        freeinstall-line-item-data (cart-items/freeinstall-line-item-query data)
        freeinstall-applied?       (orders/freeinstall-applied? order)
        selected-quadpay?          (-> (get-in data keypaths/order) :cart-payments :quadpay)]
    {:requires-additional-payment?     (requires-additional-payment? data)
     :promotion-banner                 (promotion-banner/query data)
     :checkout-steps                   (checkout-steps/query data)
     :products                         (get-in data keypaths/v2-products)
     :items                            (cond-> (item-card-query data)
                                         (and freeinstall-applied? freeinstall-line-item-data)
                                         (update
                                          :items conj
                                          (freeinstall-line-item-data->item-card freeinstall-line-item-data)))
     :order                            order
     :payment                          (checkout-credit-card/query data)
     :delivery                         (checkout-delivery/query data)
     :install-or-free-install-applied? freeinstall-applied?
     :available-store-credit           (when-not selected-quadpay?
                                         (get-in data keypaths/user-total-available-store-credit))
     :selected-quadpay?                selected-quadpay?
     :checkout-button-data             (checkout-button-query data)
     :confirmation-summary             (confirmation-summary/query data)
     :store-slug                       (get-in data keypaths/store-slug)
     :loaded-quadpay?                  (get-in data keypaths/loaded-quadpay)}))

(defn adventure-query
  [data]
  (let [order                      (get-in data keypaths/order)
        freeinstall-line-item-data (adventure-cart-items/freeinstall-line-item-query data)]
    {:requires-additional-payment?     (requires-additional-payment? data)
     :promotion-banner                 (promotion-banner/query data)
     :checkout-steps                   (checkout-steps/query data)
     :products                         (get-in data keypaths/v2-products)
     :items                            (update (item-card-query data)
                                               :items conj
                                               (freeinstall-line-item-data->item-card freeinstall-line-item-data))
     :order                            order
     :payment                          (checkout-credit-card/query data)
     :delivery                         (checkout-delivery/query data)
     :install-or-free-install-applied? (orders/freeinstall-applied? order)
     :available-store-credit           (get-in data keypaths/user-total-available-store-credit)
     :checkout-button-data             (checkout-button-query data)
     :confirmation-summary             (confirmation-summary/query data)
     :store-slug                       (get-in data keypaths/store-slug)
     :servicing-stylist                (get-in data adventure.keypaths/adventure-servicing-stylist)
     :selected-quadpay?                (-> (get-in data keypaths/order) :cart-payments :quadpay)
     :loaded-quadpay?                  (get-in data keypaths/loaded-quadpay)}))


(defn ^:private built-non-auth-component [data opts]
  (if (= "freeinstall" (get-in data keypaths/store-slug))
    (component/build adventure-component
                     (adventure-query data)
                     opts)
    (component/build component
                     (query data)
                     opts)))

(defn ^:export built-component
  [data opts]
  (checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout
   built-non-auth-component
   data
   opts))
