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
            [adventure.checkout.cart.items :as adventure-cart-items]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]
            [storefront.accessors.products :as accessors.products]
            [catalog.products :as products]
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
        store-credit-amount (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0)
        order-total         (get-in data keypaths/order-total)]
    (and
     ;; stripe can charge any amount
     no-stripe-payment?
     ;; is total covered by remaining store-credit?
     (> order-total store-credit-amount))))

(defn checkout-button [{:keys [spinning? disabled?]}]
  (ui/submit-button "Place Order" {:spinning? spinning?
                                   :disabled? disabled?
                                   :data-test "confirm-form-submit"}))

(defn checkout-button-query [data]
  (let [order                   (get-in data keypaths/order)

        saving-card?   (checkout-credit-card/saving-card? data)
        placing-order? (utils/requesting? data request-keys/place-order)]
    {:disabled?              (utils/requesting? data request-keys/update-shipping-method)
     :spinning?              (or saving-card? placing-order?)}))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn ^:private display-freeinstall-line-item
  [{:keys [id price title thumbnail-image-fn detail]}]
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
      (if (empty? detail)
        [:div.mb1.mt0 (str "w/ " "a Certified Mayvenn Stylist")
         [:ul.h6.list-img-purple-checkmark.pl4.mt1
          (mapv (fn [%] [:li %])
                ["Licensed Salon Stylist" "Near you" "Experienced"])]]
        detail)]]]])

(defn component
  [{:keys [available-store-credit
           checkout-steps
           payment delivery order
           skus
           requires-additional-payment?
           promotion-banner
           install-or-free-install-applied?
           freeinstall-line-item-data
           confirmation-summary
           checkout-button-data
           store-slug]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (component/build promotion-banner/sticky-component promotion-banner nil)
     (om/build checkout-steps/component checkout-steps)

     [:form
      {:on-submit
       (utils/send-event-callback events/control-checkout-confirmation-submit
                                  {:place-order? requires-additional-payment?})}

      [:.clearfix.mxn3
       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        [:.h3.left-align "Order Summary"]

        [:div.my2
         {:data-test "confirmation-line-items"}
         (summary/display-line-items (orders/product-items order) skus)
         (when freeinstall-line-item-data
           (display-freeinstall-line-item freeinstall-line-item-data))]]

       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        (om/build checkout-delivery/component delivery)
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (if confirmation-summary
          (component/build confirmation-summary/component confirmation-summary {})
          (summary/display-order-summary order
                                         {:read-only?             true
                                          :use-store-credit?      (not install-or-free-install-applied?)
                                          :available-store-credit available-store-credit}))
        (when (= store-slug "freeinstall")
          [:p.h6.my4.center.col-10.mx-auto.line-height-3
           "A text message will be sent to connect you and your stylist after your order is placed."])
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (checkout-button checkout-button-data)]]]]])))

(defn adventure-component
  [{:keys [available-store-credit
           checkout-steps
           payment delivery order
           skus
           requires-additional-payment?
           promotion-banner
           install-or-free-install-applied?
           freeinstall-line-item-data
           confirmation-summary
           checkout-button-data
           store-slug]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (component/build promotion-banner/sticky-component promotion-banner nil)
     (om/build checkout-steps/component checkout-steps)

     [:form
      {:on-submit
       (utils/send-event-callback events/control-checkout-confirmation-submit
                                  {:place-order? requires-additional-payment?})}

      [:.clearfix.mxn3
       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        [:.h3.left-align "Order Summary"]

        [:div.my2
         {:data-test "confirmation-line-items"}
         (summary/display-line-items (orders/product-items order) skus)
         (when freeinstall-line-item-data
           (display-freeinstall-line-item freeinstall-line-item-data))]]

       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        (om/build checkout-delivery/component delivery)
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (if confirmation-summary
          (component/build confirmation-summary/component confirmation-summary {})
          (summary/display-order-summary order
                                         {:read-only?             true
                                          :use-store-credit?      (not install-or-free-install-applied?)
                                          :available-store-credit available-store-credit}))
        (when (= store-slug "freeinstall")
          [:p.h6.my4.center.col-10.mx-auto.line-height-3
           "A text message will be sent to connect you and your stylist after your order is placed."])
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (checkout-button checkout-button-data)]]]]])))

(defn- absolute-url [& path]
  (apply str (.-protocol js/location) "//" (.-host js/location) path))

(defn query [data]
  (let [order (get-in data keypaths/order)]
    {:requires-additional-payment?     (requires-additional-payment? data)
     :promotion-banner                 (promotion-banner/query data)
     :checkout-steps                   (checkout-steps/query data)
     :products                         (get-in data keypaths/v2-products)
     :skus                             (get-in data keypaths/v2-skus)
     :order                            order
     :payment                          (checkout-credit-card/query data)
     :delivery                         (checkout-delivery/query data)
     :install-or-free-install-applied? (orders/freeinstall-applied? order)
     :available-store-credit           (get-in data keypaths/user-total-available-store-credit)
     :checkout-button-data             (checkout-button-query data)
     :confirmation-summary             (confirmation-summary/query data)
     :freeinstall-line-item-data       (cart-items/freeinstall-line-item-query data)
     :store-slug                       (get-in data keypaths/store-slug)
     :freeinstall?                     (= "freeinstall" (get-in data keypaths/store-slug))}))

(defn adventure-query [data]
  (let [order (get-in data keypaths/order)]
    (adventure-cart-items/freeinstall-line-item-query data)
    {:requires-additional-payment?     (requires-additional-payment? data)
     :promotion-banner                 (promotion-banner/query data)
     :checkout-steps                   (checkout-steps/query data)
     :products                         (get-in data keypaths/v2-products)
     :skus                             (get-in data keypaths/v2-skus)
     :order                            order
     :payment                          (checkout-credit-card/query data)
     :delivery                         (checkout-delivery/query data)
     :install-or-free-install-applied? (orders/freeinstall-applied? order)
     :available-store-credit           (get-in data keypaths/user-total-available-store-credit)
     :checkout-button-data             (checkout-button-query data)
     :confirmation-summary             (confirmation-summary/query data)
     :freeinstall-line-item-data       (adventure-cart-items/freeinstall-line-item-query data)
     :store-slug                       (get-in data keypaths/store-slug)
     :freeinstall?                     (= "freeinstall" (get-in data keypaths/store-slug))}))

(defn built-component [data opts]
  (let [query-data           (query data)
        query-adventure-data (adventure-query data)
        freeinstall?         (:freeinstall? query-data)]
    (if freeinstall?
      (om/build component query-adventure-data opts)
      (om/build component query-data opts))))
