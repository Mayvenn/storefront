(ns checkout.cart
  (:require
   #?@(:cljs [[cemerick.url :refer [url-encode]]
              [storefront.api :as api]
              [storefront.accessors.orders :as orders]
              [storefront.config :as config]
              [storefront.accessors.stylist-urls :as stylist-urls]
              [storefront.platform.messages :as messages]
              [storefront.history :as history]
              [goog.labs.userAgent.device :as device]])
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.transitions :as transitions]))

(defmethod effects/perform-effects events/control-pick-stylist-button
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate events/navigate-adventure-match-stylist)))

(defmethod effects/perform-effects events/control-change-stylist
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate events/navigate-adventure-find-your-stylist)))

(defmethod transitions/transition-state events/control-toggle-promo-code-entry
  [_ _ _ app-state]
  (update-in app-state keypaths/promo-code-entry-open? not))

(defmethod effects/perform-effects events/control-cart-update-coupon
  [_ _ _ _ app-state]
  #?(:cljs
     (let [coupon-code (get-in app-state keypaths/cart-coupon-code)]
       (when-not (empty? coupon-code)
         (api/add-promotion-code {:session-id     (get-in app-state keypaths/session-id)
                                  :number         (get-in app-state keypaths/order-number)
                                  :token          (get-in app-state keypaths/order-token)
                                  :promo-code     coupon-code
                                  :allow-dormant? false})))))

(defmethod effects/perform-effects events/control-cart-share-show
  [_ _ _ _ app-state]
  #?(:cljs
     (api/create-shared-cart (get-in app-state keypaths/session-id)
                             (get-in app-state keypaths/order-number)
                             (get-in app-state keypaths/order-token))))

(defmethod effects/perform-effects events/control-cart-remove
  [_ event variant-id _ app-state]
  #?(:cljs
     (api/delete-line-item (get-in app-state keypaths/session-id) (get-in app-state keypaths/order) variant-id)))

(defmethod effects/perform-effects events/control-cart-line-item-inc
  [_ event {:keys [variant]} _ app-state]
  #?(:cljs
     (let [sku      (get (get-in app-state keypaths/v2-skus) (:sku variant))
           order    (get-in app-state keypaths/order)
           quantity 1]
       (api/add-sku-to-bag (get-in app-state keypaths/session-id)
                           {:sku                sku
                            :token              (:token order)
                            :number             (:number order)
                            :quantity           quantity
                            :heat-feature-flags (get-in app-state keypaths/features)}
                           #(messages/handle-message events/api-success-add-sku-to-bag
                                                     {:order    %
                                                      :quantity quantity
                                                      :sku      sku})))))

(defmethod effects/perform-effects events/control-cart-line-item-dec
  [_ event {:keys [variant]} _ app-state]
  #?(:cljs
     (let [order (get-in app-state keypaths/order)]
       (api/remove-line-item (get-in app-state keypaths/session-id)
                             {:number     (:number order)
                              :token      (:token order)
                              :variant-id (:id variant)
                              :sku-code   (:sku variant)}
                             #(messages/handle-message events/api-success-decrease-quantity {:order %})))))

#?(:cljs
   (defn- order-has-inapplicable-freeinstall-promo?
     "A small hack to prevent classic orders from being placed with the freeinstall
        promo.  A full solution would be implemented in waiter."
     [app-state]
     (let [order            (get-in app-state keypaths/order)
           store-experience (get-in app-state keypaths/store-experience)]
       (and (orders/discountable-services-on-order? order)
            (= "mayvenn-classic" store-experience)))))

#?(:cljs
   (defn- reject-inapplicable-freeinstall-promo [session-id order]
     (api/remove-freeinstall-line-item session-id order
                                       (fn [order]
                                         (messages/handle-message events/api-success-remove-from-bag {:order order})
                                         (history/enqueue-redirect events/navigate-cart {:query-params {:error "ineligible-for-free-install"}})))))

(defmethod effects/perform-effects events/control-checkout-cart-submit
  [dispatch event args _ app-state]
  #?(:cljs
     (if (order-has-inapplicable-freeinstall-promo? app-state)
       (reject-inapplicable-freeinstall-promo (get-in app-state keypaths/session-id)
                                              (get-in app-state keypaths/order))
       ;; If logged in, this will send user to checkout-address. If not, this sets
       ;; things up so that if the user chooses sign-in from the returning-or-guest
       ;; page, then signs-in, they end up on the address page. Convoluted.
       (history/enqueue-navigate events/navigate-checkout-address))))

(defmethod effects/perform-effects events/control-checkout-cart-paypal-setup
  [dispatch event args _ app-state]
  #?(:cljs
     (let [order (get-in app-state keypaths/order)]
       (if (order-has-inapplicable-freeinstall-promo? app-state)
         (reject-inapplicable-freeinstall-promo (get-in app-state keypaths/session-id)
                                                (get-in app-state keypaths/order))
         ;; If logged in, this will send user to checkout-address. If not, this sets
         ;; things up so that if the user chooses sign-in from the returning-or-guest
         ;; page, then signs-in, they end up on the address page. Convoluted.
         (api/update-cart-payments
          (get-in app-state keypaths/session-id)
          {:order (-> app-state
                      (get-in keypaths/order)
                      (select-keys [:token :number])
                 ;;; Get ready for some nonsense!
                      ;;
                      ;; Paypal requires that urls are *double* url-encoded, such as
                      ;; the token part of the return url, but that *query
                      ;; parameters* are only singley encoded.
                      ;;
                      ;; Thanks for the /totally sane/ API, PayPal.
                      (assoc-in [:cart-payments]
                                {:paypal {:amount (get-in app-state keypaths/order-total)
                                          :mobile-checkout? (not (device/isDesktop))
                                          :return-url (str stylist-urls/store-url "/orders/" (:number order) "/paypal/"
                                                           (url-encode (url-encode (:token order)))
                                                           "?sid="
                                                           (url-encode (get-in app-state keypaths/session-id)))
                                          :callback-url (str config/api-base-url "/v2/paypal-callback?number=" (:number order)
                                                             "&order-token=" (url-encode (:token order)))
                                          :cancel-url (str stylist-urls/store-url "/cart?error=paypal-cancel")}}))
           :event events/external-redirect-paypal-setup})))))
