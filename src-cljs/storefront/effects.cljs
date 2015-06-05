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
            [storefront.messages :refer [enqueue-message]]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/app-start [_ event args app-state]
  (riskified/insert-beacon (get-in app-state keypaths/session-id)))

(defmethod perform-effects events/app-stop [_ event args app-state]
  (riskified/remove-beacon))

(defmethod perform-effects events/navigate [_ event args app-state]
  (api/get-taxons (get-in app-state keypaths/event-ch))
  (api/get-store (get-in app-state keypaths/event-ch)
                 (get-in app-state keypaths/store-slug))
  (api/get-sms-number (get-in app-state keypaths/event-ch))
  (api/get-promotions (get-in app-state keypaths/event-ch))
  (let [user-id (get-in app-state keypaths/user-id)
        token (get-in app-state keypaths/user-token)]
    (when (and user-id token)
      (api/get-account (get-in app-state keypaths/event-ch) user-id token)))
  (when-let [order-id (get-in app-state keypaths/user-order-id)]
    (api/get-order (get-in app-state keypaths/event-ch)
                   order-id
                   (get-in app-state keypaths/user-order-token)))
  (set! (.. js/document -body -scrollTop) 0)
  (when-not (or
             (empty? (get-in app-state keypaths/flash-success-nav))
             (= [event args] (get-in app-state keypaths/flash-success-nav)))
    (enqueue-message (get-in app-state keypaths/event-ch)
                     [events/flash-dismiss-success]))
  (when (.hasOwnProperty js/window "RISKX")
    (.go js/RISKX (clj->js (routes/path-for app-state event args)))))

(defmethod perform-effects events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (api/get-products (get-in app-state keypaths/event-ch)
                    (taxon-name-from taxon-path)))

(defmethod perform-effects events/navigate-product [_ event {:keys [product-path]} app-state]
  (api/get-product (get-in app-state keypaths/event-ch)
                   product-path))

(defmethod perform-effects events/navigate-checkout [_ event args app-state]
  (let [allowed-steps (->> checkout/steps
                           (take-while (comp not #{event} :event)))
        allowed-states (-> (map :name allowed-steps)
                           (conj "cart")
                           set)
        order (get-in app-state keypaths/order)]
    (js/console.log (clj->js allowed-states) (clj->js (:state (get-in app-state keypaths/order))))
    (if (:state order)
      (if (allowed-states (:state (get-in app-state keypaths/order)))
        :success
        (routes/enqueue-redirect app-state (:event (last allowed-steps))))
      (routes/enqueue-redirect app-state events/navigate-cart))))

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
  (api/get-states (get-in app-state keypaths/event-ch)))

(defmethod perform-effects events/navigate-order [_ event args app-state]
  (api/get-past-order (get-in app-state keypaths/event-ch)
                      (get-in app-state keypaths/past-order-id)
                      (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-my-orders [_ event args app-state]
  (api/get-my-orders (get-in app-state keypaths/event-ch)
                     (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/control-menu-expand [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "hidden"))

(defmethod perform-effects events/control-menu-collapse [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "auto"))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state keypaths/event-ch)
               (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)))

(defmethod perform-effects events/control-sign-up-submit [_ event args app-state]
  (api/sign-up (get-in app-state keypaths/event-ch)
               (get-in app-state keypaths/sign-up-email)
               (get-in app-state keypaths/sign-up-password)
               (get-in app-state keypaths/sign-up-password-confirmation)))

(defmethod perform-effects events/control-sign-out [_ event args app-state]
  (cookie-jar/clear (get-in app-state keypaths/cookie)))

(defmethod perform-effects events/control-browse-add-to-bag [_ event _ app-state]
  (let [product (query/get (get-in app-state keypaths/browse-product-query)
                           (vals (get-in app-state keypaths/products)))
        variant (query/get (get-in app-state keypaths/browse-variant-query)
                           (:variants product))]
    (api/add-to-bag (get-in app-state keypaths/event-ch)
                    (variant :id)
                    (get-in app-state keypaths/browse-variant-quantity)
                    (get-in app-state keypaths/user-order-token)
                    (get-in app-state keypaths/user-order-id)
                    (get-in app-state keypaths/user-token))))

(defmethod perform-effects events/control-forgot-password-submit [_ event args app-state]
  (api/forgot-password (get-in app-state keypaths/event-ch)
                       (get-in app-state keypaths/forgot-password-email)))

(defmethod perform-effects events/control-reset-password-submit [_ event args app-state]
  (api/reset-password (get-in app-state keypaths/event-ch)
                      (get-in app-state keypaths/reset-password-password)
                      (get-in app-state keypaths/reset-password-password-confirmation)
                      (get-in app-state keypaths/reset-password-token)))

(defn save-cookie [app-state remember?]
  (cookie-jar/save (get-in app-state keypaths/cookie)
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
     (merge (select-keys order [:id :number :token])
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
     (merge (select-keys order [:id :number :token])
            {:line_items_attributes [(merge (select-keys line-item [:id :variant_id])
                                            {:quantity 0})]})
     {})))

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
                      (merge (select-keys order [:id :number :token])
                             {:state "delivery"
                              :shipments_attributes
                              {:id (get-in order [:shipments 0 :id])
                               :selected_shipping_rate_id (get-in app-state keypaths/checkout-selected-shipping-method-id)}}))
                    {:navigate [events/navigate-checkout-payment]}))

(defmethod perform-effects events/control-checkout-payment-method-submit [_ event args app-state]
  (api/update-order (get-in app-state keypaths/event-ch)
                    (get-in app-state keypaths/user-token)
                    (let [order (get-in app-state keypaths/order)]
                      (merge (select-keys order [:id :number :token])
                             {:state "payment"
                              :payments_attributes
                              [{:payment_method_id (get-in order [:payment_methods 0 :id])
                                :source_attributes
                                {:number (get-in app-state keypaths/checkout-credit-card-number)
                                 :expiry (get-in app-state keypaths/checkout-credit-card-expiration)
                                 :verification_value (get-in app-state keypaths/checkout-credit-card-ccv)
                                 :name (get-in app-state keypaths/checkout-credit-card-name)}}]}))
                    {:navigate [events/navigate-checkout-confirmation]}))

(defmethod perform-effects events/control-checkout-confirmation-submit [_ event args app-state]
  (api/update-order (get-in app-state keypaths/event-ch)
                    (get-in app-state keypaths/user-token)
                    (let [order (get-in app-state keypaths/order)]
                      (select-keys order [:id :number :token]))
                    {:navigate [events/navigate-checkout-complete {:order-id (get-in app-state keypaths/user-order-id)}]}))

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

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home))

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

(defmethod perform-effects events/api-success-get-order [_ event order app-state]
  (save-cookie app-state true))

(defmethod perform-effects events/api-success-update-cart [_ event {:keys [order navigate]} app-state]
  (when navigate
    (apply routes/enqueue-navigate app-state navigate)))

(defmethod perform-effects events/api-success-update-order [_ event {:keys [order navigate]} app-state]
  (save-cookie app-state true)
  (when navigate
    (apply routes/enqueue-navigate app-state navigate)))
