(ns storefront.effects
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.api :as api]
            [storefront.routes :as routes]
            [storefront.cookie-jar :as cookie-jar]
            [storefront.taxons :refer [taxon-name-from]]
            [storefront.query :as query]
            [storefront.credit-cards :refer [parse-expiration]]
            [storefront.riskified :as riskified]
            [storefront.checkout :as checkout]
            [storefront.messages :refer [enqueue-message]]
            [storefront.analytics :as analytics]))

(defn scroll-to-top []
  (set! (.. js/document -body -scrollTop) 0))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/app-start [_ event args app-state]
  (riskified/insert-beacon (get-in app-state keypaths/session-id))
  (analytics/insert-tracking))

(defmethod perform-effects events/app-stop [_ event args app-state]
  (riskified/remove-beacon)
  (analytics/remove-tracking))

(defmethod perform-effects events/navigate [_ event args app-state]
  (api/get-taxons (get-in app-state keypaths/event-ch)
                  (get-in app-state keypaths/api-cache))
  (api/get-store (get-in app-state keypaths/event-ch)
                 (get-in app-state keypaths/api-cache)
                 (get-in app-state keypaths/store-slug))
  (api/get-sms-number (get-in app-state keypaths/event-ch))
  (api/get-promotions (get-in app-state keypaths/event-ch)
                      (get-in app-state keypaths/api-cache))

  (when-let [order-number (get-in app-state keypaths/order-number)]
    (api/get-order (get-in app-state keypaths/event-ch)
                   order-number
                   (get-in app-state keypaths/order-token)))
  (scroll-to-top)

  (let [[flash-event flash-args] (get-in app-state keypaths/flash-success-nav)]
    (when-not (or
               (empty? (get-in app-state keypaths/flash-success-nav))
               (= [event (seq args)] [flash-event (seq flash-args)]))
      (enqueue-message (get-in app-state keypaths/event-ch)
                       [events/flash-dismiss-success])))
  (let [[flash-event flash-args] (get-in app-state keypaths/flash-failure-nav)]
    (when-not (or
               (empty? (get-in app-state keypaths/flash-failure-nav))
               (= [event (seq args)] [flash-event (seq flash-args)]))
      (enqueue-message (get-in app-state keypaths/event-ch)
                       [events/flash-dismiss-failure])))

  (riskified/track-page (routes/path-for app-state event args))
  (analytics/track-page (routes/path-for app-state event args)))

(defmethod perform-effects events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (api/get-products (get-in app-state keypaths/event-ch)
                    (get-in app-state keypaths/api-cache)
                    (taxon-name-from taxon-path)))

(defmethod perform-effects events/navigate-product [_ event {:keys [product-path]} app-state]
  (api/get-product (get-in app-state keypaths/event-ch)
                   product-path))

(defmethod perform-effects events/navigate-checkout [_ event args app-state]
  (when-not (get-in app-state keypaths/order-number)
    (routes/enqueue-redirect app-state events/navigate-cart)))

(defmethod perform-effects events/navigate-stylist-manage-account [_ event args app-state]
  (api/get-states (get-in app-state keypaths/event-ch)
                  (get-in app-state keypaths/api-cache))
  (api/get-stylist-account (get-in app-state keypaths/event-ch)
                           (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-stylist-commissions [_ event args app-state]
  (api/get-stylist-commissions (get-in app-state keypaths/event-ch)
                               (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-stylist-bonus-credit [_ event args app-state]
  (api/get-stylist-bonus-credits (get-in app-state keypaths/event-ch)
                                 (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-stylist-referrals [_ event args app-state]
  (api/get-stylist-referral-program (get-in app-state keypaths/event-ch)
                                    (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-checkout-address [_ event args app-state]
  (api/get-states (get-in app-state keypaths/event-ch)
                  (get-in app-state keypaths/api-cache)))

(defmethod perform-effects events/navigate-checkout-payment [_ event args app-state]
  (api/get-payment-methods (get-in app-state keypaths/event-ch)
                           (get-in app-state keypaths/api-cache)))

(defmethod perform-effects events/navigate-order [_ event args app-state]
  (api/get-past-order (get-in app-state keypaths/event-ch)
                      (get-in app-state keypaths/past-order-id)
                      (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-my-orders [_ event args app-state]
  (api/get-my-orders (get-in app-state keypaths/event-ch)
                     (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-not-found [_ event args app-state]
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-failure {:message "The page you were looking for could not be found."
                                               :navigation [event args]}]))

(defmethod perform-effects events/control-menu-expand [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "hidden"))

(defmethod perform-effects events/control-menu-collapse [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "auto"))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state keypaths/event-ch)
               (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/control-sign-up-submit [_ event args app-state]
  (api/sign-up (get-in app-state keypaths/event-ch)
               (get-in app-state keypaths/sign-up-email)
               (get-in app-state keypaths/sign-up-password)
               (get-in app-state keypaths/sign-up-password-confirmation)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/control-sign-out [_ event args app-state]
  (cookie-jar/clear (get-in app-state keypaths/cookie))
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-success {:message "Logged out successfully"
                                               :navigation [events/navigate-home {}]}])
  (routes/enqueue-navigate app-state events/navigate-home))

(defmethod perform-effects events/control-browse-add-to-bag [_ event _ app-state]
  (let [product (query/get (get-in app-state keypaths/browse-product-query)
                           (vals (get-in app-state keypaths/products)))
        variant (query/get (get-in app-state keypaths/browse-variant-query)
                           (:variants product))]
    (api/add-to-bag (get-in app-state keypaths/event-ch)
                    (variant :id)
                    (get-in app-state keypaths/browse-variant-quantity)
                    (get-in app-state keypaths/store-stylist-id)
                    (get-in app-state keypaths/order-token)
                    (get-in app-state keypaths/order-number)
                    (get-in app-state keypaths/user-token))))

(defmethod perform-effects events/control-forgot-password-submit [_ event args app-state]
  (api/forgot-password (get-in app-state keypaths/event-ch)
                       (get-in app-state keypaths/forgot-password-email)))

(defmethod perform-effects events/control-reset-password-submit [_ event args app-state]
  (if (empty? (get-in app-state keypaths/reset-password-password))
    (enqueue-message (get-in app-state keypaths/event-ch)
                     [events/flash-show-failure {:message "Your password cannot be blank."
                                                 :navigation (get-in app-state keypaths/navigation-message)}])
    (api/reset-password (get-in app-state keypaths/event-ch)
                        (get-in app-state keypaths/reset-password-password)
                        (get-in app-state keypaths/reset-password-password-confirmation)
                        (get-in app-state keypaths/reset-password-token))))

(defn save-cookie [app-state remember?]
  (cookie-jar/save-order (get-in app-state keypaths/cookie)
                         (get-in app-state keypaths/order)
                         {:remember? remember?})
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user)
                        {:remember? remember?}))

(defmethod perform-effects events/control-manage-account-submit [_ event args app-state]
  (api/update-account (get-in app-state keypaths/event-ch)
                      (get-in app-state keypaths/user-id)
                      (get-in app-state keypaths/manage-account-email)
                      (get-in app-state keypaths/manage-account-password)
                      (get-in app-state keypaths/manage-account-password-confirmation)
                      (get-in app-state keypaths/user-token)))

(defn updated-quantities [line-items quantities]
  (->>
   line-items
   (map #(select-keys % [:id :quantity :variant_id]))
   (map #(assoc % :quantity (-> % :id quantities)))))

(defmethod perform-effects events/control-cart-update [_ event {:keys [navigate-to-checkout?]} app-state]
  (let [order (get-in app-state keypaths/order)]
    (api/update-cart
     (get-in app-state keypaths/event-ch)
     (get-in app-state keypaths/user-token)
     (merge (select-keys order [:id :number :guest-token])
            (when navigate-to-checkout?
              {:state "address"
               :email (get-in app-state keypaths/user-email)
               :user_id (get-in app-state keypaths/user-id)})
            {:coupon_code (get-in app-state keypaths/cart-coupon-code)
             :line_items_attributes (updated-quantities
                                     (:line_items order)
                                     (get-in app-state keypaths/cart-quantities))})
     {:navigate (when navigate-to-checkout? [events/navigate-checkout-address])})))

(defmethod perform-effects events/control-cart-remove [_ event args app-state]
  (let [order (get-in app-state keypaths/order)
        line-item (->> order :line_items (filter (comp #{(:id args)} :id)) first)]
    (api/update-cart
     (get-in app-state keypaths/event-ch)
     (get-in app-state keypaths/user-token)
     (merge (select-keys order [:id :number :guest-token])
            {:line_items_attributes [(merge (select-keys line-item [:id :variant_id])
                                            {:quantity 0})]})
     {})))

(defmethod perform-effects events/control-stylist-manage-account-submit [_ events args app-state]
  (let [event-ch (get-in app-state keypaths/event-ch)
        user-token (get-in app-state keypaths/user-token)
        stylist-account (get-in app-state keypaths/stylist-manage-account)]
    (api/update-stylist-account event-ch user-token stylist-account)
    (when (stylist-account :profile-picture)
      (api/update-stylist-account-profile-picture event-ch user-token stylist-account))))

(defmethod perform-effects events/control-checkout-update-addresses-submit [_ event args app-state]
  (let [event-ch (get-in app-state keypaths/event-ch)
        token (get-in app-state keypaths/user-token)
        addresses {:bill_address (get-in app-state keypaths/checkout-billing-address)
                   :ship_address (get-in app-state
                                         (if (get-in app-state keypaths/checkout-shipping-address-use-billing-address)
                                           keypaths/checkout-billing-address
                                           keypaths/checkout-shipping-address))}]
    (when (get-in app-state keypaths/checkout-billing-address-save-my-address)
      (api/update-account-address event-ch
                                  (get-in app-state keypaths/user-id)
                                  (get-in app-state keypaths/user-email)
                                  (:bill_address addresses)
                                  (:ship_address addresses)
                                  token))
    (api/update-order event-ch token
                      (merge (get-in app-state keypaths/order)
                             addresses
                             {:state "address"})
                      {:navigate [events/navigate-checkout-delivery]})))

(defmethod perform-effects events/control-checkout-shipping-method-submit [_ event args app-state]
  (api/update-order (get-in app-state keypaths/event-ch)
                    (get-in app-state keypaths/user-token)
                    (let [order (get-in app-state keypaths/order)]
                      (merge (select-keys order [:id :number :guest-token])
                             {:state "delivery"
                              :shipments_attributes
                              {:id (get-in order [:shipments 0 :id])
                               :selected_shipping_rate_id (get-in app-state keypaths/checkout-selected-shipping-method-id)}}))
                    {:navigate [events/navigate-checkout-payment]}))

(defmethod perform-effects events/control-checkout-payment-method-submit [_ event args app-state]
  (api/update-order (get-in app-state keypaths/event-ch)
                    (get-in app-state keypaths/user-token)
                    (let [order (get-in app-state keypaths/order)]
                      (merge (select-keys order [:id :number :guest-token])
                             {:state "payment"
                              :use-store-credits (get-in app-state keypaths/checkout-use-store-credits)}
                             (if (and (get-in app-state keypaths/checkout-use-store-credits)
                                      (get-in app-state keypaths/order-covered-by-store-credit))
                               {:payments_attributes
                                [{:payment_method_id (or (get-in order [:payment_methods 0 :id])
                                                         (get-in app-state (into keypaths/payment-methods [0 :id])))}]}

                               {:payments_attributes
                                [{:payment_method_id (or (get-in order [:payment_methods 0 :id])
                                                         (get-in app-state (into keypaths/payment-methods [0 :id])))
                                  :source_attributes
                                  {:number (get-in app-state keypaths/checkout-credit-card-number)
                                   :expiry (get-in app-state keypaths/checkout-credit-card-expiration)
                                   :verification_value (get-in app-state keypaths/checkout-credit-card-ccv)
                                   :name (get-in app-state keypaths/checkout-credit-card-name)}}]})))
                    {:navigate [events/navigate-checkout-confirmation]}))

(defmethod perform-effects events/control-checkout-confirmation-submit [_ event args app-state]
  (api/update-order (get-in app-state keypaths/event-ch)
                    (get-in app-state keypaths/user-token)
                    (merge (select-keys (get-in app-state keypaths/order) [:id :number :guest-token])
                           {:session_id (get-in app-state keypaths/session-id)})
                    {:navigate [events/navigate-order-complete {:order-id (get-in app-state keypaths/order-number)}]}))

(defmethod perform-effects events/api-success-sign-in [_ event args app-state]
  (save-cookie app-state (get-in app-state keypaths/sign-in-remember))
  (when (= (get-in app-state keypaths/navigation-event) events/navigate-sign-in)
    (routes/enqueue-navigate app-state events/navigate-home))
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-success {:message "Logged in successfully"
                                               :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-success {:message "Welcome! You have signed up successfully."
                                               :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-forgot-password [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home)
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-success {:message "You will receive an email with instructions on how to reset your password in a few minutes."
                                               :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-reset-password [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-success {:message "Your password was changed successfully. You are now signed in."
                                               :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-manage-account [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-success {:message "Account updated"
                                               :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-stylist-manage-account [_ event args app-state]
  (save-cookie app-state true)
  (when (:updated args)
    (enqueue-message (get-in app-state keypaths/event-ch)
                     [events/flash-show-success {:message "Account updated"
                                                 :navigation [events/navigate-stylist-manage-account {}]}])))

(defmethod perform-effects events/api-success-store [_ event order app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (when (and user-id token)
      (api/get-account (get-in app-state keypaths/event-ch) user-id token stylist-id))))

(defmethod perform-effects events/api-success-get-order [_ event order app-state]
  (save-cookie app-state true))

(defmethod perform-effects events/api-success-update-cart [_ event {:keys [order navigate]} app-state]
  (when navigate
    (apply routes/enqueue-navigate app-state navigate)))

(defmethod perform-effects events/api-success-update-order [_ event {:keys [order navigate]} app-state]
  (save-cookie app-state true)
  (when navigate
    (apply routes/enqueue-navigate app-state navigate)))

(defmethod perform-effects events/api-failure-no-network-connectivity [_ event response app-state]
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-failure
                    {:message "Could not connect to the internet. Reload the page and try again."
                     :navigation (get-in app-state keypaths/navigation-message)}]))

(defmethod perform-effects events/api-failure-bad-server-response [_ event response app-state]
  (enqueue-message (get-in app-state keypaths/event-ch)
                   [events/flash-show-failure
                    {:message "Uh oh, an error occurred. Reload the page and try again."
                     :navigation (get-in app-state keypaths/navigation-message)}]))

(defmethod perform-effects events/flash-show [_ event args app-state]
  (scroll-to-top))

(defmethod perform-effects events/api-failure-validation-errors [_ event validation-errors app-state]
  (if (seq (:fields validation-errors))
    (scroll-to-top)
    (enqueue-message (get-in app-state keypaths/event-ch)
                     [events/flash-show-failure
                      {:message (:error validation-errors)
                       :navigation (get-in app-state keypaths/navigation-message)}])))
