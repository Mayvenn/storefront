(ns checkout.behavior
  (:require #?@(:cljs [[goog.labs.userAgent.device :as device]
                       [cemerick.url :refer [url-encode]]
                       [storefront.accessors.stylist-urls :as stylist-urls]
                       [storefront.api :as api]
                       [storefront.config :as config]
                       [storefront.history :as history]])
            [api.orders :as api.orders]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.orders :as orders]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.experiments :as experiments]
            [storefront.transitions :as transitions]))

(defn ^:private current-order-has-addons
  [app-state]
  (some->> app-state
           api.orders/current
           :order.items/services
           (mapcat :addons)
           not-empty))

;; == /checkout/add page ==

(defmethod effects/perform-effects events/api-success-bulk-add-to-bag-checkout-add
  [_ event args previous-app-state app-state]
  #?(:cljs
     (if (-> app-state
             auth/signed-in
             ::auth/at-all)
       (history/enqueue-navigate events/navigate-checkout-address {})
       (history/enqueue-navigate events/navigate-checkout-returning-or-guest {}))))

(defmethod effects/perform-effects events/navigate-checkout-add
  [_ event {:keys [navigate/caused-by]} previous-app-state app-state]
  (when (and (#{:module-load :first-nav} caused-by)
             (current-order-has-addons app-state))
    (effects/redirect events/navigate-cart)))

;; == Cart controls ==

(defmethod effects/perform-effects events/control-checkout-cart-submit
  [dispatch event args _ app-state]
  (messages/handle-message events/checkout-initiated-mayvenn-checkout))

(defmethod effects/perform-effects events/control-checkout-cart-paypal-setup
  [dispatch event args _ app-state]
  (messages/handle-message events/checkout-initiated-paypal-checkout))

;; == checkout initiation sanity checks ==

(defn- order-has-inapplicable-freeinstall-promo?
  "A small hack to prevent classic orders from being placed with the freeinstall
        promo.  A full solution would be implemented in waiter."
  [app-state]
  (let [order            (get-in app-state keypaths/order)
        store-experience (get-in app-state keypaths/store-experience)]
    (and (orders/discountable-services-on-order? order)
         (= "mayvenn-classic" store-experience))))

(defn- reject-inapplicable-freeinstall-promo [session-id order]
  #?(:cljs
     (api/remove-freeinstall-line-item session-id order
                                       (fn [order]
                                         (messages/handle-message events/api-success-remove-from-bag {:order order})
                                         (messages/handle-message events/checkout-order-rejected)))))

(defmethod effects/perform-effects events/checkout-order-rejected
  [dispatch event args _ app-state]
  (messages/handle-message events/navigate-cart {:query-params {:error "ineligible-for-free-install"}}))

(defmethod effects/perform-effects events/checkout-initiated-mayvenn-checkout ; read: stripe
  [_ _ _ _ app-state]
  (if (order-has-inapplicable-freeinstall-promo? app-state)
    (reject-inapplicable-freeinstall-promo (get-in app-state keypaths/session-id)
                                           (get-in app-state keypaths/order))
    (messages/handle-message events/checkout-order-cleared-for-mayvenn-checkout)))

(defmethod effects/perform-effects events/checkout-initiated-paypal-checkout
  [_ _ _ _ app-state]
  (if (order-has-inapplicable-freeinstall-promo? app-state)
    (reject-inapplicable-freeinstall-promo (get-in app-state keypaths/session-id)
                                           (get-in app-state keypaths/order))
    (messages/handle-message events/checkout-order-cleared-for-paypal-checkout)))

;; == dispatch checkout flows ==

(defmethod effects/perform-effects events/checkout-order-cleared-for-mayvenn-checkout
  [_ _ _ _ app-state]
  #?(:cljs
     (if (and (experiments/interrupt-checkout? app-state)
              (not (current-order-has-addons app-state)))
       (history/enqueue-navigate events/navigate-checkout-add)
       (history/enqueue-navigate events/navigate-checkout-address))))

;; TODO: consider moving paypal query-building logic into its own namespace
(defmethod effects/perform-effects events/checkout-order-cleared-for-paypal-checkout
  [_ _ _ _ app-state]
  #?(:cljs
     (let [order (get-in app-state keypaths/order)]
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
                              {:paypal {:amount           (get-in app-state keypaths/order-total)
                                        :mobile-checkout? (not (device/isDesktop))
                                        :return-url       (str stylist-urls/store-url "/orders/" (:number order) "/paypal/"
                                                               (url-encode (url-encode (:token order)))
                                                               "?sid="
                                                               (url-encode (get-in app-state keypaths/session-id)))
                                        :callback-url     (str config/api-base-url "/v2/paypal-callback?number=" (:number order)
                                                               "&order-token=" (url-encode (:token order)))
                                        :cancel-url       (str stylist-urls/store-url "/cart?error=paypal-cancel")}}))
         :event events/external-redirect-paypal-setup}))))
