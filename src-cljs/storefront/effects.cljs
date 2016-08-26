(ns storefront.effects
  (:require [ajax.core :refer [-abort]]
            [cemerick.url :refer [url-encode]]
            [clojure.string :as str]
            [goog.labs.userAgent.device :as device]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.accessors.credit-cards :refer [parse-expiration]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.taxons :as taxons]
            [storefront.accessors.pixlee :as accessors.pixlee]
            [storefront.accessors.products :as products]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.analytics :as analytics]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.hooks.convert :as convert]
            [storefront.hooks.optimizely :as optimizely]
            [storefront.hooks.facebook :as facebook]
            [storefront.hooks.fastpass :as fastpass]
            [storefront.hooks.seo :as seo]
            [storefront.hooks.pixlee :as pixlee]
            [storefront.hooks.places-autocomplete :as places-autocomplete]
            [storefront.hooks.reviews :as reviews]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.stripe :as stripe]
            [storefront.hooks.talkable :as talkable]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.hooks.wistia :as wistia]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message handle-later]]
            [storefront.routes :as routes]
            [storefront.utils.query :as query]))

(defn refresh-account [app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (when (and user-id user-token stylist-id)
      (api/get-account user-id user-token stylist-id))))

(defn refresh-current-order [app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)
        order-number (get-in app-state keypaths/order-number)]
    (when (and user-id user-token stylist-id (not order-number))
      (api/get-current-order user-id
                             user-token
                             stylist-id))))

(defn refresh-products [app-state product-ids]
  (when (seq product-ids)
    (api/get-products-by-ids product-ids
                             (get-in app-state keypaths/user-token))))

(defn ensure-products [app-state product-ids]
  (let [not-cached (remove (products/loaded-ids app-state) (set product-ids))]
    (refresh-products app-state not-cached)))

(defn refresh-taxon-products
  "Intentionally bypass ensure-products whenever we navigate to a category"
  [app-state]
  (refresh-products app-state (:product-ids (taxons/current-taxon app-state))))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/app-start [dispatch event args app-state]
  (google-analytics/insert-tracking)
  (convert/insert-tracking)
  (optimizely/insert-tracking)
  (riskified/insert-beacon (get-in app-state keypaths/session-id))
  (facebook-analytics/insert-tracking)
  (analytics/track dispatch event args app-state)
  (talkable/insert)
  (refresh-account app-state)
  (refresh-current-order app-state))

(defmethod perform-effects events/app-stop [_ event args app-state]
  (convert/remove-tracking)
  (optimizely/remove-tracking)
  (riskified/remove-beacon)
  (google-analytics/remove-tracking)
  (facebook-analytics/remove-tracking))

(defmethod perform-effects events/external-redirect-welcome [_ event args app-state]
  (set! (.-location js/window) (get-in app-state keypaths/welcome-url)))

(defmethod perform-effects events/external-redirect-community [_ event args app-state]
  (set! (.-location js/window) (fastpass/community-url)))

(defmethod perform-effects events/external-redirect-paypal-setup [_ event args app-state]
  (set! (.-location js/window) (get-in app-state keypaths/order-cart-payments-paypal-redirect-url)))

(defmethod perform-effects events/navigate [dispatch event args app-state]
  (let [[nav-event nav-args] (get-in app-state keypaths/navigation-message)]
    (refresh-account app-state)
    (api/get-sms-number)
    (api/get-promotions (get-in app-state keypaths/api-cache)
                        (or
                         (first (get-in app-state keypaths/order-promotion-codes))
                         (get-in app-state keypaths/pending-promo-code)))

    (when-let [order-number (get-in app-state keypaths/order-number)]
      (api/get-order order-number
                     (get-in app-state keypaths/order-token)))
    (seo/set-tags app-state)
    (scroll/snap-to-top)

    (when-let [pending-promo-code (-> nav-args :query-params :sha)]
      (cookie-jar/save-pending-promo-code
       (get-in app-state keypaths/cookie)
       pending-promo-code)
      (routes/enqueue-redirect nav-event (update-in nav-args [:query-params] dissoc :sha)))

    (let [[flash-event flash-args :as flash-message] (get-in app-state keypaths/flash-success-nav)]
      (when-not (or
                 (empty? flash-message)
                 (= [nav-event (seq nav-args)] [flash-event (seq flash-args)]))
        (handle-message events/flash-dismiss)))
    (let [[flash-event flash-args :as flash-message] (get-in app-state keypaths/flash-failure-nav)]
      (when-not (or
                 (empty? flash-message)
                 (= [nav-event (seq nav-args)] [flash-event (seq flash-args)]))
        (handle-message events/flash-dismiss)))

    (analytics/track dispatch event args app-state)
    (handle-message events/control-popup-hide)

    (when-not (= [nav-event nav-args] (get-in app-state keypaths/previous-navigation-message))
      (let [path (routes/current-path app-state)]
        (exception-handler/refresh)))))

(defmethod perform-effects events/navigate-shop-by-look [_ event _ app-state]
  (pixlee/insert))

(defmethod perform-effects events/navigate-categories [dispatch event args app-state]
  (analytics/track dispatch event args app-state))

(defmethod perform-effects events/navigate-category [dispatch event {:keys [taxon-slug] :as args} app-state]
  (analytics/track dispatch event args app-state)
  (reviews/insert-reviews)
  (when (and (experiments/pixlee-product? app-state)
             (accessors.pixlee/content-available? (taxons/current-taxon app-state)))
    (pixlee/insert))
  (refresh-taxon-products app-state))

(defmethod perform-effects events/navigate-account [_ event args app-state]
  (when-not (get-in app-state keypaths/user-token)
    (routes/enqueue-redirect events/navigate-sign-in)))

(defmethod perform-effects events/navigate-stylist [_ event args app-state]
  (cond
    (not (get-in app-state keypaths/user-token))
    (routes/enqueue-redirect events/navigate-sign-in)

    (not (stylists/own-store? app-state))
    (do
      (routes/enqueue-redirect events/navigate-home)
      (handle-message events/flash-show-failure
                      {:message    "Page not found"
                       :navigation [events/navigate-home {}]}))

    :else nil))

(defmethod perform-effects events/navigate-stylist-account [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-states (get-in app-state keypaths/api-cache))
    (api/get-stylist-account user-token)))

(defmethod perform-effects events/navigate-stylist-dashboard [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-stylist-stats user-token)))

(defmethod perform-effects events/navigate-stylist-dashboard-commissions [_ event args app-state]
  (api/get-shipping-methods)
  (when (zero? (get-in app-state keypaths/stylist-commissions-page 0))
    (handle-message events/control-stylist-commissions-fetch)))

(defmethod perform-effects events/control-stylist-commissions-fetch [_ _ args app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        page (inc (get-in app-state keypaths/stylist-commissions-page 0))]
    (when (and user-id user-token)
      (api/get-stylist-commissions user-id
                                   user-token
                                   {:page page}))))

(defmethod perform-effects events/app-restart [_ _ _ _]
  (.reload js/window.location))

(defmethod perform-effects events/api-end [_ event args app-state]
  (let [app-version (get-in app-state keypaths/app-version)
        remote-version (:app-version args)]
    (when (and app-version remote-version
               (< config/allowed-version-drift (- remote-version app-version)))
      (handle-later events/app-restart))))


(defmethod perform-effects events/api-success-stylist-commissions [_ event args app-state]
  (ensure-products app-state
                   (->> (get-in app-state keypaths/stylist-commissions-history)
                        (map :order)
                        (mapcat orders/product-items)
                        (map :product-id)
                        set)))

(defmethod perform-effects events/navigate-stylist-dashboard-bonus-credit [_ event args app-state]
  (when (zero? (get-in app-state keypaths/stylist-bonuses-page 0))
    (handle-message events/control-stylist-bonuses-fetch)))

(defmethod perform-effects events/control-stylist-bonuses-fetch [_ event args app-state]
  (let [user-token (get-in app-state keypaths/user-token)
        page (inc (get-in app-state keypaths/stylist-bonuses-page 0))]
    (when user-token
      (api/get-stylist-bonus-credits user-token
                                     {:page page}))))

(defmethod perform-effects events/navigate-stylist-dashboard-referrals [_ event args app-state]
  (when (zero? (get-in app-state keypaths/stylist-referral-program-page 0))
    (handle-message events/control-stylist-referrals-fetch)))

(defmethod perform-effects events/control-stylist-referrals-fetch [_ event args app-state]
  (let [user-token (get-in app-state keypaths/user-token)
        page (inc (get-in app-state keypaths/stylist-referral-program-page 0))]
    (when user-token
      (api/get-stylist-referral-program user-token
                                        {:page page}))))

(defmethod perform-effects events/control-stylist-referral-submit [_ event args app-state]
  (api/send-referrals
   {:referring-stylist-id (get-in app-state keypaths/store-stylist-id)
    :referrals (map #(select-keys % [:fullname :email :phone]) (get-in app-state keypaths/stylist-referrals))}))

(def cart-error-codes
  {"paypal-incomplete"      "We were unable to complete your order with PayPal. Please try again."
   "paypal-invalid-address" "Unfortunately, Mayvenn products cannot be delivered to this address at this time. Please choose a new shipping destination."
   "share-cart-failed"      "The bag that has been shared with you has items that are no longer available.  Please ask your stylist for a new link or 'Shop Now' below"})

(defmethod perform-effects events/navigate-cart [_ event args app-state]
  (refresh-current-order app-state)
  (api/get-shipping-methods)
  (when-let [error-msg (-> args :query-params :error cart-error-codes)]
    (handle-message events/flash-show-failure
                    {:message error-msg
                     :navigation (get-in app-state keypaths/navigation-message)})))

(defmethod perform-effects events/navigate-checkout [_ event args app-state]
  (cond
    (not (get-in app-state keypaths/order-number))
    (routes/enqueue-redirect events/navigate-cart)

    (not (or (= event events/navigate-checkout-sign-in)
             (get-in app-state keypaths/user-id)
             (get-in app-state keypaths/checkout-as-guest)))
    (routes/enqueue-redirect events/navigate-checkout-sign-in)))

(defmethod perform-effects events/navigate-checkout-sign-in [_ event args app-state]
  (facebook/insert))

(defn- fetch-saved-cards [app-state]
  (when-let [user-id (get-in app-state keypaths/user-id)]
    (api/get-saved-cards user-id (get-in app-state keypaths/user-token))))

(defmethod perform-effects events/navigate-checkout-address [_ event args app-state]
  (places-autocomplete/insert-places-autocomplete)
  (api/get-states (get-in app-state keypaths/api-cache))
  (fetch-saved-cards app-state))

(defmethod perform-effects events/navigate-checkout-payment [dispatch event args app-state]
  (fetch-saved-cards app-state)
  (stripe/insert))

(defmethod perform-effects events/navigate-checkout-confirmation [_ event args app-state]
  (stripe/insert)
  (api/get-shipping-methods))

(defmethod perform-effects events/navigate-order-complete [_ event {{:keys [paypal order-token]} :query-params number :number} app-state]
  (when paypal
    (routes/enqueue-redirect events/navigate-order-complete {:number number}))
  (when (and number order-token)
    (api/get-completed-order number order-token)))

(defmethod perform-effects events/navigate-friend-referrals [_ event args app-state]
  (talkable/show-referrals app-state))

(defmethod perform-effects events/navigate-account-referrals [_ event args app-state]
  (talkable/show-referrals app-state))

(defmethod perform-effects events/api-success-get-completed-order [_ event order app-state]
  (handle-message events/order-completed order))

(defmethod perform-effects events/api-success-get-saved-cards [dispatch event args app-state]
  (analytics/track dispatch event args app-state))

(defn redirect-to-return-navigation [app-state]
  (apply routes/enqueue-redirect
         (get-in app-state keypaths/return-navigation-message)))

(defn redirect-when-signed-in [app-state]
  (when (get-in app-state keypaths/user-email)
    (handle-message events/flash-show-success
                    {:message "You are already signed in."
                     :navigation (get-in app-state keypaths/return-navigation-message)})
    (redirect-to-return-navigation app-state)))

(defmethod perform-effects events/navigate-sign-in [_ event args app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-getsat-sign-in [_ event args app-state]
  (when-not (get-in app-state keypaths/user-token)
    (routes/enqueue-redirect events/navigate-sign-in)))
(defmethod perform-effects events/navigate-sign-up [_ event args app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-forgot-password [_ event args app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-reset-password [_ event args app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))

(defmethod perform-effects events/navigate-not-found [_ event args app-state]
  (handle-message events/flash-show-failure
                  {:message "The page you were looking for could not be found."
                   :navigation [event args]}))

(defmethod perform-effects events/control-menu-expand [_ event {keypath :keypath} app-state]
  (when (#{keypaths/menu-expanded} keypath)
    (set! (.. js/document -body -style -overflow) "hidden")))

(defmethod perform-effects events/control-menu-collapse-all [_ _ _ _]
  (set! (.. js/document -body -style -overflow) "auto"))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-number)
               (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/control-sign-up-submit [_ event args app-state]
  (api/sign-up (get-in app-state keypaths/sign-up-email)
               (get-in app-state keypaths/sign-up-password)
               (get-in app-state keypaths/sign-up-password-confirmation)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-number)
               (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/control-facebook-sign-in [_ event args app-state]
  (facebook/start-log-in app-state))

(defmethod perform-effects events/control-facebook-reset [_ event args app-state]
  (facebook/start-reset app-state))

(defmethod perform-effects events/facebook-success-sign-in [_ _ facebook-response app-state]
  (api/facebook-sign-in (-> facebook-response :authResponse :userID)
                        (-> facebook-response :authResponse :accessToken)
                        (get-in app-state keypaths/store-stylist-id)
                        (get-in app-state keypaths/order-number)
                        (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/facebook-failure-sign-in [_ _ args app-state]
  (handle-message events/flash-show-failure
                  {:message "Could not sign in with Facebook.  Please try again, or sign in with email and password."
                   :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/facebook-email-denied [_ _ args app-state]
  (handle-message events/flash-show-failure
                  {:message "We need your Facebook email address to communicate with you about your orders. Please try again."
                   :navigation (get-in app-state keypaths/navigation-message)}))

(defn- abort-pending-requests [requests]
  (doseq [{xhr :xhr} requests] (when xhr (-abort xhr))))

(defmethod perform-effects events/control-sign-out [_ event args app-state]
  (cookie-jar/clear (get-in app-state keypaths/cookie))
  (handle-message events/control-menu-collapse-all)
  (abort-pending-requests (get-in app-state keypaths/api-requests))
  (routes/enqueue-navigate events/navigate-home)
  (handle-message events/flash-show-success
                  {:message "Logged out successfully"
                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/control-bundle-option-select [dispatch event args app-state]
  (analytics/track dispatch event args app-state))

(defmethod perform-effects events/control-add-to-bag [dispatch event {:keys [variant quantity] :as args} app-state]
  (analytics/track dispatch event args app-state)
  (api/add-to-bag
   {:variant variant
    :quantity quantity
    :stylist-id (get-in app-state keypaths/store-stylist-id)
    :token (get-in app-state keypaths/order-token)
    :number (get-in app-state keypaths/order-number)
    :user-id (get-in app-state keypaths/user-id)
    :user-token (get-in app-state keypaths/user-token)}))

(defmethod perform-effects events/control-forgot-password-submit [_ event args app-state]
  (api/forgot-password (get-in app-state keypaths/forgot-password-email)))

(defmethod perform-effects events/control-reset-password-submit [_ event args app-state]
  (if (empty? (get-in app-state keypaths/reset-password-password))
    (handle-message events/flash-show-failure
                    {:message "Your password cannot be blank."
                     :navigation (get-in app-state keypaths/navigation-message)})
    (api/reset-password (get-in app-state keypaths/reset-password-password)
                        (get-in app-state keypaths/reset-password-password-confirmation)
                        (get-in app-state keypaths/reset-password-token)
                        (get-in app-state keypaths/order-number)
                        (get-in app-state keypaths/order-token))))

(defmethod perform-effects events/facebook-success-reset [_ event facebook-response app-state]
  (api/facebook-reset-password (-> facebook-response :authResponse :userID)
                               (-> facebook-response :authResponse :accessToken)
                               (get-in app-state keypaths/reset-password-token)
                               (get-in app-state keypaths/order-number)
                               (get-in app-state keypaths/order-token)))

(defn save-cookie [app-state]
  ;; bug: previously, we always set this to true. Needs verification with Ryan before we fix this.
  (let [remember? true #_(get-in app-state keypaths/sign-in-remember)]
    (cookie-jar/save-order (get-in app-state keypaths/cookie)
                           (get-in app-state keypaths/order)
                           remember?)
    (cookie-jar/save-user (get-in app-state keypaths/cookie)
                          (get-in app-state keypaths/user)
                          remember?)))

(defmethod perform-effects events/control-account-profile-submit [_ event args app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (api/update-account (get-in app-state keypaths/user-id)
                        (get-in app-state keypaths/manage-account-email)
                        (get-in app-state keypaths/manage-account-password)
                        (get-in app-state keypaths/manage-account-password-confirmation)
                        (get-in app-state keypaths/user-token))))

(defmethod perform-effects events/control-cart-update-coupon [_ event args app-state]
  (let [coupon-code (get-in app-state keypaths/cart-coupon-code)]
    (when-not (empty? coupon-code)
      (api/add-promotion-code (get-in app-state keypaths/order-number)
                              (get-in app-state keypaths/order-token)
                              coupon-code
                              false))))

(defmethod perform-effects events/control-cart-share-show [dispatch event args app-state]
  (analytics/track dispatch event args app-state)
  (api/create-shared-cart (get-in app-state keypaths/order-number)
                          (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/control-cart-line-item-inc [_ event {:keys [variant]} app-state]
  (api/inc-line-item (get-in app-state keypaths/order) {:variant variant}))

(defmethod perform-effects events/control-cart-line-item-dec [_ event {:keys [variant]} app-state]
  (api/dec-line-item (get-in app-state keypaths/order) {:variant variant}))

(defmethod perform-effects events/control-cart-remove [_ event variant-id app-state]
  (api/delete-line-item (get-in app-state keypaths/order) variant-id))

(defmethod perform-effects events/control-checkout-as-guest-submit [_ event _ app-state]
  (redirect-to-return-navigation app-state))

(defmethod perform-effects events/control-checkout-cart-submit [dispatch event args app-state]
  (analytics/track dispatch event args app-state)
  (routes/enqueue-navigate events/navigate-checkout-address))

(defmethod perform-effects events/control-checkout-cart-paypal-setup [dispatch event args app-state]
  (analytics/track dispatch event args app-state)
  (let [order (get-in app-state keypaths/order)]
    (api/update-cart-payments
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
      :event events/external-redirect-paypal-setup})))

(defmethod perform-effects events/control-stylist-account-profile-submit [_ events args app-state]
  (let [user-token      (get-in app-state keypaths/user-token)
        stylist-account (get-in app-state keypaths/stylist-manage-account)]
    (api/update-stylist-account-profile user-token stylist-account)))

(defmethod perform-effects events/control-stylist-account-password-submit [_ events args app-state]
  (let [user-token            (get-in app-state keypaths/user-token)
        stylist-account       (get-in app-state keypaths/stylist-manage-account)]
    (when (empty? (get-in app-state keypaths/errors))
      (api/update-stylist-account-password user-token stylist-account))))

(defmethod perform-effects events/control-stylist-account-commission-submit [_ events args app-state]
  (let [user-token      (get-in app-state keypaths/user-token)
        stylist-account (get-in app-state keypaths/stylist-manage-account)]
    (api/update-stylist-account-commission user-token stylist-account)))

(defmethod perform-effects events/control-stylist-account-social-submit [_ events args app-state]
  (let [user-token      (get-in app-state keypaths/user-token)
        stylist-account (get-in app-state keypaths/stylist-manage-account)]
    (api/update-stylist-account-social user-token stylist-account)))

(defmethod perform-effects events/control-stylist-account-photo-pick [_ events args app-state]
  (let [user-token      (get-in app-state keypaths/user-token)
        profile-picture (:file args)]
    (api/update-stylist-account-photo user-token profile-picture)))

(defmethod perform-effects events/control-checkout-update-addresses-submit [_ event args app-state]
  (let [guest-checkout? (get-in app-state keypaths/checkout-as-guest)
        billing-address (get-in app-state keypaths/checkout-billing-address)
        shipping-address (get-in app-state keypaths/checkout-shipping-address)
        update-addresses (if guest-checkout? api/guest-update-addresses api/update-addresses)]
    (update-addresses
     (cond-> (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                    {:billing-address billing-address :shipping-address shipping-address})
       guest-checkout?
       (assoc :email (get-in app-state keypaths/checkout-guest-email))

       (get-in app-state keypaths/checkout-bill-to-shipping-address)
       (assoc :billing-address shipping-address)))))

(defmethod perform-effects events/control-checkout-shipping-method-select [_ event args app-state]
  (api/update-shipping-method (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                                     {:shipping-method-sku (get-in
                                                            app-state
                                                            keypaths/checkout-selected-shipping-method-sku)})))

(defmethod perform-effects events/stripe-success-create-token [_ _ stripe-response app-state]
  (api/update-cart-payments
   {:order (-> app-state
               (get-in keypaths/order)
               (select-keys [:token :number])
               (assoc :cart-payments (get-in app-state keypaths/checkout-selected-payment-methods))
               (assoc-in [:cart-payments :stripe :source] (:id stripe-response))
               (assoc-in [:cart-payments :stripe :save?] (boolean (and (get-in app-state keypaths/user-id)
                                                                       (get-in app-state keypaths/checkout-credit-card-save)))))
    :navigate events/navigate-checkout-confirmation
    :place-order? (:place-order? stripe-response)}))

(defmethod perform-effects events/stripe-failure-create-token [_ _ stripe-response app-state]
  (handle-message events/flash-show-failure
                  {:message (get-in stripe-response [:error :message])
                   :navigation (get-in app-state keypaths/navigation-message)}))

(defn create-stripe-token [app-state args]
  ;; create stripe token (success handler commands waiter w/ payment methods (success  navigates to confirm))
  (let [expiry (parse-expiration (get-in app-state keypaths/checkout-credit-card-expiration))]
    (stripe/create-token (get-in app-state keypaths/checkout-credit-card-name)
                         (get-in app-state keypaths/checkout-credit-card-number)
                         (get-in app-state keypaths/checkout-credit-card-ccv)
                         (first expiry)
                         (last expiry)
                         (get-in app-state (conj keypaths/order :billing-address))
                         args)))

(defmethod perform-effects events/control-checkout-payment-method-submit [_ event args app-state]
  (handle-message events/flash-dismiss)
  (let [use-store-credit (pos? (get-in app-state keypaths/user-total-available-store-credit))
        covered-by-store-credit (orders/fully-covered-by-store-credit?
                                 (get-in app-state keypaths/order)
                                 (get-in app-state keypaths/user))
        selected-saved-card-id (get-in app-state keypaths/checkout-credit-card-selected-id)]
    (if (and use-store-credit covered-by-store-credit)
      ;; command waiter w/ payment methods(success handler navigate to confirm)
      (api/update-cart-payments
       {:order (-> app-state
                   (get-in keypaths/order)
                   (select-keys [:token :number])
                   (merge {:cart-payments (get-in app-state keypaths/checkout-selected-payment-methods)}))
        :navigate events/navigate-checkout-confirmation})
      ;; create stripe token (success handler commands waiter w/ payment methods (success  navigates to confirm))
      (if (and selected-saved-card-id (not= selected-saved-card-id "add-new-card"))
        (api/update-cart-payments
         {:order (-> app-state
                     (get-in keypaths/order)
                     (select-keys [:token :number])
                     (merge {:cart-payments (get-in app-state keypaths/checkout-selected-payment-methods)})
                     (assoc-in [:cart-payments :stripe :source] selected-saved-card-id))
          :navigate events/navigate-checkout-confirmation})
        (create-stripe-token app-state args)))))

(defmethod perform-effects events/control-checkout-remove-promotion [_ _ {:keys [code]} app-state]
  (api/remove-promotion-code (get-in app-state keypaths/order) code))

(defmethod perform-effects events/control-checkout-confirmation-submit [_ event {:keys [place-order?] :as args} app-state]
  (if place-order?
    (create-stripe-token app-state args)
    (api/place-order (merge (get-in app-state keypaths/order)
                            {:session-id (get-in app-state keypaths/session-id)}))))

(defmethod perform-effects events/api-success-sign-in [dispatch event args app-state]
  (save-cookie app-state)
  (when-not (get-in app-state keypaths/order-number)
    ;; Try to fetch latest cart order
    (refresh-current-order app-state))
  (redirect-to-return-navigation app-state)
  (handle-message events/flash-show-success
                  {:message "Logged in successfully"
                   :navigation [events/navigate-home {}]})
  (analytics/track dispatch event args app-state))

(defmethod perform-effects events/api-success-sign-up [dispatch event args app-state]
  (save-cookie app-state)
  (redirect-to-return-navigation app-state)
  (handle-message events/flash-show-success
                  {:message "Welcome! You have signed up successfully."
                   :navigation [events/navigate-home {}]})
  (analytics/track dispatch event args app-state))

(defmethod perform-effects events/api-success-forgot-password [_ event args app-state]
  (routes/enqueue-navigate events/navigate-home)
  (handle-message events/flash-show-success
                  {:message "You will receive an email with instructions on how to reset your password in a few minutes."
                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-reset-password [dispatch event args app-state]
  (save-cookie app-state)
  (redirect-to-return-navigation app-state)
  (handle-message events/flash-show-success
                  {:message "Your password was changed successfully. You are now signed in."
                   :navigation [events/navigate-home {}]})
  (analytics/track dispatch event args app-state))

(defmethod perform-effects events/api-success-account [_ event {:keys [community-url]} app-state]
  (when community-url
    (fastpass/insert-fastpass community-url)))

(defmethod perform-effects events/api-success-manage-account [_ event args app-state]
  (save-cookie app-state)
  (routes/enqueue-navigate events/navigate-home)
  (handle-message events/flash-show-success
                  {:message "Account updated"
                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-stylist-account-profile [_ event args app-state]
  (save-cookie app-state)
  (handle-message events/flash-show-success
                  {:message "Profile updated"
                   :navigation [events/navigate-stylist-account-profile {}]}))

(defmethod perform-effects events/api-success-stylist-account-password [_ event args app-state]
  (save-cookie app-state)
  (handle-message events/flash-show-success
                  {:message "Password updated"
                   :navigation [events/navigate-stylist-account-password {}]}))

(defmethod perform-effects events/api-success-stylist-account-commission [_ event args app-state]
  (save-cookie app-state)
  (handle-message events/flash-show-success
                  {:message "Commission settings updated"
                   :navigation [events/navigate-stylist-account-commission {}]}))

(defmethod perform-effects events/api-success-stylist-account-social [_ event args app-state]
  (save-cookie app-state)
  (handle-message events/flash-show-success
                  {:message "Social settings updated"
                   :navigation [events/navigate-stylist-account-social {}]}))

(defmethod perform-effects events/api-success-stylist-account-photo [_ event args app-state]
  (save-cookie app-state)
  (handle-message events/flash-show-success
                  {:message "Photo updated"
                   :navigate (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/api-success-send-stylist-referrals [_ event args app-state]
  (handle-later events/control-popup-hide {} 2000))

(defn add-pending-promo-code [app-state {:keys [number token] :as order}]
  (when-let [pending-promo-code (get-in app-state keypaths/pending-promo-code)]
    (api/add-promotion-code number token pending-promo-code true)))

(defmethod perform-effects events/api-success-get-order [_ event order app-state]
  (ensure-products app-state (map :product-id (orders/product-items order)))
  (if (and (orders/incomplete? order)
           (= (:number order)
              (get-in app-state keypaths/order-number)))
    (do
      (save-cookie app-state)
      (add-pending-promo-code app-state order))
    (cookie-jar/clear-order (get-in app-state keypaths/cookie))))

(defmethod perform-effects events/api-success-update-order-place-order [_ event {:keys [order]} app-state]
  (handle-message events/order-completed order))

(defmethod perform-effects events/order-completed [dispatch event order app-state]
  (analytics/track dispatch event order app-state)
  (cookie-jar/clear-order (get-in app-state keypaths/cookie))
  (talkable/show-pending-offer app-state))

(defmethod perform-effects events/api-success-update-order-update-address [_ event {:keys [order]} app-state]
  (api/update-account-address (get-in app-state keypaths/states)
                              (get-in app-state keypaths/user)
                              (:billing-address order)
                              (:shipping-address order)))

(defmethod perform-effects events/api-success-update-order-update-cart-payments [_ event {:keys [order place-order?]} app-state]
  (when place-order?
    (api/place-order (merge order {:session-id (get-in app-state keypaths/session-id)}))))

(defmethod perform-effects events/api-success-update-order [_ event {:keys [order navigate event]} app-state]
  (save-cookie app-state)
  (when event
    (handle-message event {:order order}))
  (when navigate
    (routes/enqueue-navigate navigate {:number (:number order)})))

(defmethod perform-effects events/api-failure-no-network-connectivity [_ event response app-state]
  (handle-message events/flash-show-failure
                  {:message "Something went wrong. Please refresh and try again or contact customer service."
                   :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/api-failure-bad-server-response [_ event response app-state]
  (handle-message events/flash-show-failure
                  {:message "Uh oh, an error occurred. Reload the page and try again."
                   :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/api-failure-stylist-account-photo-too-large [_ event response app-state]
  (handle-message events/flash-show-failure
                  {:message "Whoa, the photo you uploaded is too large"
                   :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/flash-show [_ event args app-state]
  (scroll/snap-to-top))

(defmethod perform-effects events/api-failure-pending-promo-code [_ event args app-state]
  (cookie-jar/clear-pending-promo-code (get-in app-state keypaths/cookie)))

(defmethod perform-effects events/api-failure-errors [_ event errors app-state]
  (condp = (:error-code errors)
    "stripe-card-failure" (when (= (get-in app-state keypaths/navigation-event) events/navigate-checkout-confirmation)
                            (routes/enqueue-redirect events/navigate-checkout-payment)
                            (handle-later events/api-failure-errors errors)
                            (scroll/snap-to-top))
    (scroll/snap-to-top)))

(defmethod perform-effects events/api-success-add-to-bag [dispatch event args app-state]
  (save-cookie app-state)
  (add-pending-promo-code app-state (get-in app-state keypaths/order))
  (analytics/track dispatch event args app-state)
  (handle-later events/added-to-bag))

(defmethod perform-effects events/added-to-bag [_ _ _ app-state]
  (when-let [el (.querySelector js/document "[data-ref=cart-button]")]
    (scroll/scroll-to-elem el)))

(defmethod perform-effects events/reviews-component-mounted [_ event args app-state]
  (let [expected-reviews-count 2
        actual-reviews-count (get-in app-state keypaths/review-components-count)]
    (when (= expected-reviews-count actual-reviews-count)
      (reviews/start))))

(defmethod perform-effects events/reviews-component-will-unmount [_ event args app-state]
  (when (= 0 (get-in app-state keypaths/review-components-count))
    (reviews/stop)))

(defmethod perform-effects events/checkout-address-component-mounted
  [_ event {:keys [address-elem address-keypath]} app-state]
  (places-autocomplete/attach address-elem address-keypath))

(defmethod perform-effects events/shop-by-look-component-mounted [_ event {:keys [container-id]} app-state]
  (pixlee/attach-mosaic-widget container-id))

(defmethod perform-effects events/shop-by-look-component-unmounted [_ event _ app-state]
  (pixlee/close-all))

(defmethod perform-effects events/ugc-component-mounted [_ event {:keys [pixlee-sku container-id]} app-state]
  (pixlee/attach-product-widget container-id pixlee-sku))

(defmethod perform-effects events/ugc-component-unmounted [_ event _ app-state]
  (pixlee/close-all))

(defmethod perform-effects events/video-component-mounted [_ event {:keys [video-id]} app-state]
  (wistia/attach video-id))

(defmethod perform-effects events/video-component-unmounted [_ event {:keys [video-id]} app-state]
  (wistia/detach video-id))

(defmethod perform-effects events/api-success-update-order-modify-promotion-code [_ _ _ app-state]
  (handle-message events/flash-dismiss)
  (cookie-jar/clear-pending-promo-code (get-in app-state keypaths/cookie)))

(defn update-cart-flash [app-state msg]
  (handle-message events/flash-show-success {:message msg :navigation [events/navigate-cart {}]}))

(defmethod perform-effects events/api-success-update-order-add-promotion-code [_ _ {allow-dormant? :allow-dormant?} app-state]
  (when-not allow-dormant? (update-cart-flash app-state "The coupon code was successfully applied to your order."))
  (api/get-promotions (get-in app-state keypaths/api-cache)
                      (first (get-in app-state keypaths/order-promotion-codes))))

(defmethod perform-effects events/api-success-update-order-remove-promotion-code [_ _ _ app-state]
  (update-cart-flash app-state "The coupon code was successfully removed from your order."))

(defmethod perform-effects events/convert [dispatch event {:keys [variation] :as args} app-state]
  ;; TODO: when the pixlee-product? experiment is over, this will be unnecessary
  (when (and (= variation "pixlee-product")
             (accessors.pixlee/content-available? (taxons/current-taxon app-state)))
    (pixlee/insert)))

(defmethod perform-effects events/optimizely [dispatch event {:keys [variation] :as args} app-state]
  (analytics/track dispatch event args app-state))

(defmethod perform-effects events/inserted-talkable [_ event args app-state]
  (talkable/show-pending-offer app-state)
  (when (#{events/navigate-friend-referrals events/navigate-account-referrals}
         (get-in app-state keypaths/navigation-event))
    (talkable/show-referrals app-state)))
