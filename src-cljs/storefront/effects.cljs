(ns storefront.effects
  (:require [ajax.core :refer [-abort]]
            [cemerick.url :refer [url-encode]]
            [goog.labs.userAgent.device :as device]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.accessors.credit-cards :refer [parse-expiration]]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.taxons :as taxons :refer [taxon-path-for]]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.hooks.analytics :as analytics]
            [storefront.hooks.experiments :as experiments]
            [storefront.hooks.facebook :as facebook]
            [storefront.hooks.fastpass :as fastpass]
            [storefront.hooks.opengraph :as opengraph]
            [storefront.hooks.places-autocomplete :as places-autocomplete]
            [storefront.hooks.reviews :as reviews]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.stripe :as stripe]
            [storefront.keypaths :as keypaths]
            [storefront.messages :refer [send send-later]]
            [storefront.routes :as routes]
            [storefront.utils.query :as query]))

(defn refresh-account [app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (when (and user-id token stylist-id)
      (api/get-account (get-in app-state keypaths/handle-message) user-id token stylist-id))))

(defn refresh-current-order [app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)
        order-number (get-in app-state keypaths/order-number)]
    (when (and user-id token stylist-id (not order-number))
      (api/get-current-order (get-in app-state keypaths/handle-message)
                             user-id
                             token
                             stylist-id))))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/app-start [_ event args app-state]
  (experiments/insert-optimizely)
  (riskified/insert-beacon (get-in app-state keypaths/session-id))
  (analytics/insert-tracking)
  (api/get-store (get-in app-state keypaths/handle-message)
                 (get-in app-state keypaths/api-cache)
                 (get-in app-state keypaths/store-slug)))

(defmethod perform-effects events/app-stop [_ event args app-state]
  (experiments/remove-optimizely)
  (riskified/remove-beacon)
  (analytics/remove-tracking))

(defmethod perform-effects events/external-redirect-community [_ event args app-state]
  (set! (.-location js/window) (get-in app-state keypaths/community-url)))

(defmethod perform-effects events/external-redirect-paypal-setup [_ event args app-state]
  (set! (.-location js/window) (get-in app-state keypaths/order-cart-payments-paypal-redirect-url)))

(defmethod perform-effects events/navigate [_ _ _ app-state]
  (let [[nav-event nav-args] (get-in app-state keypaths/navigation-message)]
    (api/get-taxons (get-in app-state keypaths/handle-message)
                    (get-in app-state keypaths/api-cache))
    (refresh-account app-state)
    (api/get-sms-number (get-in app-state keypaths/handle-message))
    (api/get-promotions (get-in app-state keypaths/handle-message)
                        (get-in app-state keypaths/api-cache))

    (when-let [order-number (get-in app-state keypaths/order-number)]
      (api/get-order (get-in app-state keypaths/handle-message)
                     order-number
                     (get-in app-state keypaths/order-token)))
    (opengraph/set-site-tags)
    (scroll/scroll-to-top)

    (let [[flash-event flash-args] (get-in app-state keypaths/flash-success-nav)]
      (when-not (or
                 (empty? (get-in app-state keypaths/flash-success-nav))
                 (= [nav-event (seq nav-args)] [flash-event (seq flash-args)]))
        (send app-state
              events/flash-dismiss-success)))
    (let [[flash-event flash-args] (get-in app-state keypaths/flash-failure-nav)]
      (when-not (or
                 (empty? (get-in app-state keypaths/flash-failure-nav))
                 (= [nav-event (seq nav-args)] [flash-event (seq flash-args)]))
        (send app-state
              events/flash-dismiss-failure)))

    (when-not (= [nav-event nav-args] (get-in app-state keypaths/previous-navigation-message))
      (let [path (routes/current-path app-state)]
        (riskified/track-page path)
        (analytics/track-page path)
        (experiments/track-event path)))))

(defmethod perform-effects events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (reviews/insert-reviews app-state)
  (api/get-products (get-in app-state keypaths/handle-message)
                    (get-in app-state keypaths/api-cache)
                    taxon-path
                    (get-in app-state keypaths/user-token)))

(defn bundle-builder-redirect [app-state product]
  (routes/enqueue-navigate app-state
                           events/navigate-category
                           {:taxon-path (-> product :product_attrs :category first taxon-path-for)}))

(defmethod perform-effects events/navigate-product [_ event {:keys [product-path]} app-state]
  (api/get-product (get-in app-state keypaths/handle-message)
                   product-path)
  (reviews/insert-reviews app-state)
  (let [product (query/get (get-in app-state keypaths/browse-product-query)
                           (vals (get-in app-state keypaths/products)))]
    (when (and product (bundle-builder/included-product? product))
      (bundle-builder-redirect app-state product))))

(defmethod perform-effects events/navigate-stylist [_ event args app-state]
  (when-not (get-in app-state keypaths/user-token)
    (routes/enqueue-redirect app-state events/navigate-sign-in)))

(defmethod perform-effects events/navigate-stylist-manage-account [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-states (get-in app-state keypaths/handle-message)
                    (get-in app-state keypaths/api-cache))
    (api/get-stylist-account (get-in app-state keypaths/handle-message)
                             user-token)))

(defmethod perform-effects events/navigate-stylist-commissions [_ event args app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (when (and user-id user-token)
      (api/get-stylist-commissions (get-in app-state keypaths/handle-message)
                                   user-id
                                   user-token))))

(defmethod perform-effects events/navigate-stylist-bonus-credit [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-stylist-bonus-credits (get-in app-state keypaths/handle-message)
                                   user-token)))

(defmethod perform-effects events/navigate-stylist-referrals [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-stylist-referral-program (get-in app-state keypaths/handle-message)
                                      user-token)))

(def cart-error-codes
  {"paypal-incomplete" "We were unable to complete your order with PayPal. Please try again."})

(defmethod perform-effects events/navigate-cart [_ event args app-state]
  (refresh-current-order app-state)
  (when-let [error-msg (-> args :query-params :error cart-error-codes)]
    (send app-state
          events/flash-show-failure
          {:message error-msg
           :navigation (get-in app-state keypaths/navigation-message)})))

(defmethod perform-effects events/navigate-checkout [_ event args app-state]
  (cond
    (not (get-in app-state keypaths/order-number))
    (routes/enqueue-redirect app-state events/navigate-cart)

    (not (get-in app-state keypaths/user-token))
    (routes/enqueue-redirect app-state events/navigate-sign-in)))

(defmethod perform-effects events/navigate-checkout-address [_ event args app-state]
  (places-autocomplete/insert-places-autocomplete app-state)
  (api/get-states (get-in app-state keypaths/handle-message)
                  (get-in app-state keypaths/api-cache)))

(defmethod perform-effects events/navigate-checkout-delivery [_ event args app-state]
  (api/get-shipping-methods (get-in app-state keypaths/handle-message)))

(defmethod perform-effects events/navigate-checkout-payment [_ event args app-state]
  (stripe/insert app-state))

(defmethod perform-effects events/navigate-order-complete [_ event args app-state]
  (when (:paypal (:query-params args))
    (experiments/track-event "place-order")
    (routes/enqueue-redirect app-state events/navigate-order-complete {:number (:number args)})))

(defmethod perform-effects events/navigate-order [_ event args app-state]
  (api/get-past-order (get-in app-state keypaths/handle-message)
                      (get-in app-state keypaths/past-order-id)
                      (get-in app-state keypaths/user-token)
                      (get-in app-state keypaths/user-id)))

(defmethod perform-effects events/navigate-my-orders [_ event args app-state]
  (comment (when-let [user-token (get-in app-state keypaths/user-token)]
             (api/get-my-orders (get-in app-state keypaths/handle-message)
                                user-token))))

(defn redirect-to-return-navigation [app-state]
  (apply routes/enqueue-redirect
         app-state
         (get-in app-state keypaths/return-navigation-message)))

(defn redirect-when-signed-in [app-state]
  (when (get-in app-state keypaths/user-email)
    (send app-state
          events/flash-show-success {:message "You are already signed in."
                                     :navigation (get-in app-state keypaths/return-navigation-message)})
    (redirect-to-return-navigation app-state)))

(defmethod perform-effects events/navigate-sign-in [_ event args app-state]
  (facebook/insert app-state)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-sign-in-getsat [_ event args app-state]
  (when-not (get-in app-state keypaths/user-token)
    (routes/enqueue-redirect app-state events/navigate-sign-in)))
(defmethod perform-effects events/navigate-sign-up [_ event args app-state]
  (facebook/insert app-state)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-forgot-password [_ event args app-state]
  (facebook/insert app-state)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-reset-password [_ event args app-state]
  (facebook/insert app-state)
  (redirect-when-signed-in app-state))

(defmethod perform-effects events/navigate-not-found [_ event args app-state]
  (send app-state
        events/flash-show-failure {:message "The page you were looking for could not be found."
                                   :navigation [event args]}))

(defmethod perform-effects events/control-menu-expand
  [_ event {keypath :keypath} app-state]
  (when (#{keypaths/menu-expanded} keypath)
    (set! (.. js/document -body -style -overflow) "hidden")))

(defmethod perform-effects events/control-menu-collapse
  [_ event {keypath :keypath} app-state]
  (when (#{keypaths/menu-expanded} keypath)
    (set! (.. js/document -body -style -overflow) "auto")))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state keypaths/handle-message)
               (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)
               (get-in app-state keypaths/store-stylist-id)))

(defmethod perform-effects events/control-sign-up-submit [_ event args app-state]
  (api/sign-up (get-in app-state keypaths/handle-message)
               (get-in app-state keypaths/sign-up-email)
               (get-in app-state keypaths/sign-up-password)
               (get-in app-state keypaths/sign-up-password-confirmation)
               (get-in app-state keypaths/store-stylist-id)))

(defmethod perform-effects events/control-facebook-sign-in [_ event args app-state]
  (facebook/start-log-in app-state))

(defmethod perform-effects events/control-facebook-reset [_ event args app-state]
  (facebook/start-reset app-state))

(defmethod perform-effects events/facebook-success-sign-in [_ _ facebook-response app-state]
  (api/facebook-sign-in (get-in app-state keypaths/handle-message)
                        (-> facebook-response :authResponse :userID)
                        (-> facebook-response :authResponse :accessToken)
                        (get-in app-state keypaths/store-stylist-id)))

(defmethod perform-effects events/facebook-failure-sign-in [_ _ args app-state]
  (send app-state
        events/flash-show-failure
        {:message "Could not sign in with Facebook.  Please try again, or sign in with email and password."
         :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/facebook-email-denied [_ _ args app-state]
  (send app-state
        events/flash-show-failure
        {:message "We need your Facebook email address to communicate with you about your orders. Please try again."
         :navigation (get-in app-state keypaths/navigation-message)}))

(defn- abort-pending-requests [requests]
  (doseq [{xhr :xhr} requests] (when xhr (-abort xhr))))

(defmethod perform-effects events/control-sign-out [_ event args app-state]
  (cookie-jar/clear (get-in app-state keypaths/cookie))
  (send app-state
        events/flash-show-success {:message "Logged out successfully"
                                   :navigation [events/navigate-home {}]})
  (abort-pending-requests (get-in app-state keypaths/api-requests))
  (routes/enqueue-navigate app-state events/navigate-home))

(defn api-add-to-bag [app-state product variant]
  (api/add-to-bag
   (get-in app-state keypaths/handle-message)
   {:variant variant
    :product product
    :quantity (get-in app-state keypaths/browse-variant-quantity)
    :stylist-id (get-in app-state keypaths/store-stylist-id)
    :token (get-in app-state keypaths/order-token)
    :number (get-in app-state keypaths/order-number)
    :user-id (get-in app-state keypaths/user-id)
    :user-token (get-in app-state keypaths/user-token)}))

(defmethod perform-effects events/control-browse-add-to-bag [_ event _ app-state]
  (let [product (query/get (get-in app-state keypaths/browse-product-query)
                           (vals (get-in app-state keypaths/products)))
        variant (query/get (get-in app-state keypaths/browse-variant-query)
                           (products/all-variants product))]
    (api-add-to-bag app-state product variant)))

(defmethod perform-effects events/control-bundle-option-select
  [_ event {:keys [step-name selected-options]} app-state]
  (when (step-name selected-options)
    (analytics/track-page
     (str (routes/current-path app-state) "/choose_" (clj->js step-name)))))

(defmethod perform-effects events/control-build-add-to-bag [_ event args app-state]
  (let [product (products/selected-product app-state)
        variant (products/selected-variant app-state)]
    (analytics/track-page
     (str (routes/current-path app-state) "/add_to_bag"))
    (api-add-to-bag app-state product variant)))

(defmethod perform-effects events/control-forgot-password-submit [_ event args app-state]
  (api/forgot-password (get-in app-state keypaths/handle-message)
                       (get-in app-state keypaths/forgot-password-email)))

(defmethod perform-effects events/control-reset-password-submit [_ event args app-state]
  (if (empty? (get-in app-state keypaths/reset-password-password))
    (send app-state
          events/flash-show-failure {:message "Your password cannot be blank."
                                     :navigation (get-in app-state keypaths/navigation-message)})
    (api/reset-password (get-in app-state keypaths/handle-message)
                        (get-in app-state keypaths/reset-password-password)
                        (get-in app-state keypaths/reset-password-password-confirmation)
                        (get-in app-state keypaths/reset-password-token))))

(defmethod perform-effects events/facebook-success-reset [_ event facebook-response app-state]
  (api/facebook-reset-password (get-in app-state keypaths/handle-message)
                               (-> facebook-response :authResponse :userID)
                               (-> facebook-response :authResponse :accessToken)
                               (get-in app-state keypaths/reset-password-token)))

(defn save-cookie [app-state remember?]
  (cookie-jar/save-order (get-in app-state keypaths/cookie)
                         (get-in app-state keypaths/order)
                         {:remember? remember?})
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user)
                        {:remember? remember?}))

(defmethod perform-effects events/api-handle-order-not-found [_ _ _ app-state]
  (cookie-jar/save-order (get-in app-state keypaths/cookie)
                         (get-in app-state keypaths/order)
                         {:remember? nil}))

(defmethod perform-effects events/control-manage-account-submit [_ event args app-state]
  (api/update-account (get-in app-state keypaths/handle-message)
                      (get-in app-state keypaths/user-id)
                      (get-in app-state keypaths/manage-account-email)
                      (get-in app-state keypaths/manage-account-password)
                      (get-in app-state keypaths/manage-account-password-confirmation)
                      (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/control-cart-update-coupon [_ event args app-state]
  (api/add-promotion-code (get-in app-state keypaths/handle-message)
                          (get-in app-state keypaths/order-number)
                          (get-in app-state keypaths/order-token)
                          (get-in app-state keypaths/cart-coupon-code)))

(defn- modify-cart [app-state args f]
  (f (get-in app-state keypaths/handle-message)
     (get-in app-state keypaths/order)
     args))

(defmethod perform-effects events/control-cart-line-item-inc [_ event {:keys [path]} app-state]
  (modify-cart app-state {:variant-id (last path)} api/inc-line-item))

(defmethod perform-effects events/control-cart-line-item-dec [_ event {:keys [path]} app-state]
  (modify-cart app-state {:variant-id (last path)} api/dec-line-item))

(defmethod perform-effects events/control-cart-remove [_ event variant-id app-state]
  (modify-cart app-state variant-id api/delete-line-item))

(defmethod perform-effects events/control-checkout-cart-submit [_ event _ app-state]
  (when (experiments/paypal? app-state)
    (experiments/track-event "click-checkout-button"))
  (routes/enqueue-navigate app-state events/navigate-checkout-address))

(defmethod perform-effects events/control-checkout-cart-paypal-setup [_ event _ app-state]
  (when (experiments/paypal? app-state)
    (experiments/track-event "click-paypal-button"))
  (let [store-url (str (-> js/window .-location .-protocol) "//"
                       (-> js/window .-location .-host))
        order (get-in app-state keypaths/order)]
    (api/update-cart-payments
     (get-in app-state keypaths/handle-message)
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
                                     :return-url (str store-url "/orders/" (:number order) "/paypal/"
                                                      (url-encode (url-encode (:token order)))
                                                      "?sid="
                                                      (url-encode (get-in app-state keypaths/session-id)))
                                     :callback-url (str config/api-base-url "/v2/paypal-callback?number=" (:number order)
                                                        "&token=" (url-encode (:token order)))
                                     :cancel-url (str store-url "/cart?error=paypal-cancel")}}))
      :event events/external-redirect-paypal-setup})))

(defmethod perform-effects events/control-stylist-profile-picture [_ events args app-state]
  (let [handle-message (get-in app-state keypaths/handle-message)
        user-token (get-in app-state keypaths/user-token)
        profile-picture (:file args)]
    (api/update-stylist-account-profile-picture handle-message user-token profile-picture)))

(defmethod perform-effects events/control-stylist-manage-account-submit [_ events args app-state]
  (let [handle-message (get-in app-state keypaths/handle-message)
        user-token (get-in app-state keypaths/user-token)
        stylist-account (get-in app-state keypaths/stylist-manage-account)]
    (api/update-stylist-account handle-message user-token stylist-account)
    (when (stylist-account :profile-picture)
      (api/update-stylist-account-profile-picture handle-message user-token stylist-account))))

(defmethod perform-effects events/control-checkout-update-addresses-submit [_ event args app-state]
  (let [use-billing  (get-in app-state keypaths/checkout-ship-to-billing-address)
        billing-address (get-in app-state keypaths/checkout-billing-address)
        shipping-address (if use-billing
                           billing-address
                           (get-in app-state keypaths/checkout-shipping-address))]
    (api/update-addresses (get-in app-state keypaths/handle-message)
                          (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                                 {:email (get-in app-state keypaths/user-email)
                                  :billing-address billing-address
                                  :shipping-address shipping-address}))))

(defmethod perform-effects events/control-checkout-shipping-method-submit [_ event args app-state]
  (api/update-shipping-method (get-in app-state keypaths/handle-message)
                              (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                                     {:shipping-method-sku (get-in
                                                            app-state
                                                            keypaths/checkout-selected-shipping-method-sku)})))

(defmethod perform-effects events/stripe-success-create-token [_ _ stripe-response app-state]
  (api/update-cart-payments
   (get-in app-state keypaths/handle-message)
   {:order (-> app-state
               (get-in keypaths/order)
               (select-keys [:token :number])
               (assoc :cart-payments (get-in app-state keypaths/checkout-selected-payment-methods))
               (assoc-in [:cart-payments :stripe :source] (:id stripe-response)))
    :navigate events/navigate-checkout-confirmation}))

(defmethod perform-effects events/stripe-failure-create-token [_ _ stripe-response app-state]
  (send app-state
        events/flash-show-failure
        {:message (get-in stripe-response [:error :message])
         :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/control-checkout-payment-method-submit [_ event args app-state]
  (send app-state events/flash-dismiss-failure)
  (let [use-store-credit (pos? (get-in app-state keypaths/user-total-available-store-credit))
        covered-by-store-credit (orders/fully-covered-by-store-credit?
                                 (get-in app-state keypaths/order)
                                 (get-in app-state keypaths/user))]
    (if (and use-store-credit covered-by-store-credit)
      ;; command waiter w/ payment methods(success handler navigate to confirm)
      (api/update-cart-payments
       (get-in app-state keypaths/handle-message)
       {:order (-> app-state
                   (get-in keypaths/order)
                   (select-keys [:token :number])
                   (merge {:cart-payments (get-in app-state keypaths/checkout-selected-payment-methods)}))
        :navigate events/navigate-checkout-confirmation})
      ;; create stripe token (success handler commands waiter w/ payment methods (success  navigates to confirm))
      (let [expiry (parse-expiration (get-in app-state keypaths/checkout-credit-card-expiration))]
        (stripe/create-token app-state
                             (get-in app-state keypaths/checkout-credit-card-name)
                             (get-in app-state keypaths/checkout-credit-card-number)
                             (get-in app-state keypaths/checkout-credit-card-ccv)
                             (first expiry)
                             (last expiry)
                             (get-in app-state (conj keypaths/order :billing-address)))))))

(defmethod perform-effects events/control-checkout-confirmation-submit [_ event args app-state]
  (api/place-order (get-in app-state keypaths/handle-message)
                   (merge (get-in app-state keypaths/order)
                          {:session-id (get-in app-state keypaths/session-id)})))

(defmethod perform-effects events/api-success-sign-in [_ _ _ app-state]
  (save-cookie app-state (get-in app-state keypaths/sign-in-remember))
  (if-let [order-number (get-in app-state keypaths/order-number)]
    ;; Assign guest order to signed-in user
    (api/add-user-in-order (get-in app-state keypaths/handle-message)
                           (get-in app-state keypaths/order-token)
                           order-number
                           (get-in app-state keypaths/user-token)
                           (get-in app-state keypaths/user-id))
    ;; Try to fetch latest cart order
    (refresh-current-order app-state))
  (redirect-to-return-navigation app-state)
  (send app-state
        events/flash-show-success {:message "Logged in successfully"
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
  (save-cookie app-state true)
  (when (get-in app-state keypaths/order-number)
    (api/add-user-in-order (get-in app-state keypaths/handle-message)
                           (get-in app-state keypaths/order-token)
                           (get-in app-state keypaths/order-number)
                           (get-in app-state keypaths/user-token)
                           (get-in app-state keypaths/user-id)))
  (redirect-to-return-navigation app-state)
  (send app-state
        events/flash-show-success {:message "Welcome! You have signed up successfully."
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-forgot-password [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home)
  (send app-state
        events/flash-show-success {:message "You will receive an email with instructions on how to reset your password in a few minutes."
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-reset-password [_ event args app-state]
  (save-cookie app-state true)
  (redirect-to-return-navigation app-state)
  (send app-state
        events/flash-show-success {:message "Your password was changed successfully. You are now signed in."
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-account [_ event {:keys [community-url]} app-state]
  (when community-url
    (fastpass/insert-fastpass community-url)))

(defmethod perform-effects events/api-success-manage-account [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (send app-state
        events/flash-show-success {:message "Account updated"
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-stylist-manage-account [_ event args app-state]
  (save-cookie app-state true)
  (when (:updated args)
    (send app-state
          events/flash-show-success {:message "Account updated"
                                     :navigation [events/navigate-stylist-manage-account {}]})
    (send app-state
          events/flash-dismiss-failure)))

(defmethod perform-effects events/api-success-product [_ event {:keys [product]} app-state]
  (if (and (bundle-builder/included-product? product)
           (= events/navigate-product (get-in app-state keypaths/navigation-event)))
    (bundle-builder-redirect app-state product)
    (do
      (when (and (= events/navigate-product (get-in app-state keypaths/navigation-event))
                 (= (:slug product) (get-in app-state (conj keypaths/navigation-message 1 :product-path))))
        (opengraph/set-product-tags {:name (:name product)
                                     :image (when-let [image-url (->> product
                                                                      :master
                                                                      :images
                                                                      first
                                                                      :large_url)]
                                              (str "http:" image-url))})
        (when-let [variant (if-let [variants (seq (-> product :variants))]
                             (or (->> variants (filter :can_supply?) first) (first variants))
                             (:master product))]
          (send app-state
                events/control-browse-variant-select
                {:variant variant}))))))

(defmethod perform-effects events/api-success-store [_ event order app-state]
  (refresh-account app-state)
  (refresh-current-order app-state))

(defn ensure-products-for-order [order app-state]
  (let [product-ids (map :product-id (orders/product-items order))
        not-cached (filter #(not (get-in app-state (conj keypaths/products %))) product-ids)]
    (when (seq not-cached)
      (api/get-products-by-ids (get-in app-state keypaths/handle-message) not-cached))))

(defmethod perform-effects events/api-success-get-past-order [_ event order app-state]
  (ensure-products-for-order order app-state))

(defmethod perform-effects events/api-success-get-order [_ event order app-state]
  (ensure-products-for-order order app-state)
  (if (and (orders/incomplete? order)
           (= (order :number)
              (get-in app-state keypaths/order-number)))
    (save-cookie app-state true)
    (cookie-jar/clear-order (get-in app-state keypaths/cookie))))

(defmethod perform-effects events/api-success-update-order-place-order [_ event {:keys [order]} app-state]
  (when (stylists/own-store? app-state)
    (experiments/set-dimension "stylist-own-store" "stylists"))
  (experiments/track-event "place-order" {:revenue (* 100 (:total order))})
  (cookie-jar/clear-order (get-in app-state keypaths/cookie)))

(defmethod perform-effects events/api-success-update-order-update-address [_ event {:keys [order]} app-state]
  (when (get-in app-state keypaths/checkout-save-my-addresses)
    (api/update-account-address (get-in app-state keypaths/handle-message)
                                (get-in app-state keypaths/states)
                                (get-in app-state keypaths/user)
                                (:billing-address order)
                                (:shipping-address order))))

(defmethod perform-effects events/api-success-update-order [_ event {:keys [order navigate event]} app-state]
  (save-cookie app-state true)
  (when event
    (send app-state event {:order order}))
  (when navigate
    (routes/enqueue-navigate app-state navigate {:number (:number order)})))

(defmethod perform-effects events/api-failure-no-network-connectivity [_ event response app-state]
  (send app-state
        events/flash-show-failure
        {:message "Something went wrong. Please refresh and try again or contact customer service."
         :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/api-failure-bad-server-response [_ event response app-state]
  (send app-state
        events/flash-show-failure
        {:message "Uh oh, an error occurred. Reload the page and try again."
         :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/flash-show [_ event args app-state]
  (scroll/scroll-to-top))

(defmethod perform-effects events/api-failure-validation-errors [_ event validation-errors app-state]
  (send app-state events/flash-dismiss-success)
  (scroll/scroll-to-top)
  (send app-state
        events/flash-show-failure
        {:message (:error-message validation-errors)
         :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/api-success-add-to-bag [_ _ {:keys [requested]} app-state]
  (let [{:keys [product quantity variant]} requested]
    (save-cookie app-state true)
    (when (bundle-builder/included-product? product)
      (when-let [step (get-in app-state keypaths/bundle-builder-previous-step)]
        (let [previous-options (dissoc (get-in app-state keypaths/bundle-builder-selected-options) step)
              all-variants (products/current-taxon-variants app-state)
              previous-variants (products/filter-variants-by-selections
                                 previous-options
                                 all-variants)]
          (send app-state
                events/control-bundle-option-select
                {:step-name step
                 :selected-options previous-options
                 :selected-variants previous-variants}))))
    (when (stylists/own-store? app-state)
      (experiments/set-dimension "stylist-own-store" "stylists"))
    (experiments/track-event "add-to-bag")
    (send-later app-state events/added-to-bag)))

(defmethod perform-effects events/added-to-bag [_ _ _ app-state]
  (when-let [el (.querySelector js/document ".cart-button")]
    (scroll/scroll-to-elem el)))

(defmethod perform-effects events/reviews-component-mounted [_ event args app-state]
  (reviews/start))

(defmethod perform-effects events/reviews-component-will-unmount [_ event args app-state]
  (reviews/stop))

(defmethod perform-effects events/checkout-address-component-mounted
  [_ event {:keys [address-key]} app-state]
  (when (experiments/display-variation app-state "address-auto-fill")
    (places-autocomplete/attach app-state address-key)))

(defmethod perform-effects events/api-success-update-order-add-promotion-code [_ _ _ app-state]
  (send app-state
        events/flash-show-success {:message "The coupon code was successfully applied to your order."
                                   :navigation [events/navigate-cart {}]})
  (send app-state events/flash-dismiss-failure))

(defmethod perform-effects events/optimizely [_ event args app-state]
  (experiments/activate-universal-analytics)
  (analytics/track-event "optimizely-experiment" (:variation args))
  (when (experiments/get-sat? app-state)
    (fastpass/insert-fastpass (get-in app-state keypaths/community-url))))
