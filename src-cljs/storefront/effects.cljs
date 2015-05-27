(ns storefront.effects
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.api :as api]
            [storefront.routes :as routes]
            [storefront.cookie-jar :as cookie-jar]
            [storefront.taxons :refer [taxon-name-from]]
            [storefront.query :as query]
            [cljs.core.async :refer [put!]]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/navigate [_ event args app-state]
  (api/get-taxons (get-in app-state state/event-ch-path))
  (api/get-store (get-in app-state state/event-ch-path)
                 (get-in app-state state/store-slug-path))
  (api/get-sms-number (get-in app-state state/event-ch-path))
  (let [user-id (get-in app-state state/user-id-path)
        token (get-in app-state state/user-token-path)]
    (when (and user-id token)
      (api/get-account (get-in app-state state/event-ch-path) user-id token)))
  (when-let [order-id (get-in app-state state/user-order-id-path)]
      (api/get-order (get-in app-state state/event-ch-path)
                     order-id
                     (get-in app-state state/user-order-token-path)))
  (set! (.. js/document -body -scrollTop) 0)
  (when-not (or
             (empty? (get-in app-state state/flash-success-nav-path))
             (= [event args] (get-in app-state state/flash-success-nav-path)))
    (put! (get-in app-state state/event-ch-path)
          [events/flash-dismiss-success])))

(defmethod perform-effects events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (api/get-products (get-in app-state state/event-ch-path)
                    (taxon-name-from taxon-path)))

(defmethod perform-effects events/navigate-product [_ event {:keys [product-path]} app-state]
  (api/get-product (get-in app-state state/event-ch-path)
                   product-path))

(defmethod perform-effects events/navigate-stylist-commissions [_ event args app-state]
  (api/get-stylist-commissions (get-in app-state state/event-ch-path)
                               (get-in app-state state/user-token-path)))

(defmethod perform-effects events/navigate-stylist-bonus-credit [_ event args app-state]
  (api/get-stylist-bonus-credits (get-in app-state state/event-ch-path)
                               (get-in app-state state/user-token-path)))

(defmethod perform-effects events/navigate-stylist-referrals [_ event args app-state]
  (api/get-stylist-referral-program (get-in app-state state/event-ch-path)
                                    (get-in app-state state/user-token-path)))

(defmethod perform-effects events/navigate-checkout-address [_ event args app-state]
  (api/get-states (get-in app-state state/event-ch-path)))

(defmethod perform-effects events/control-menu-expand [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "hidden"))

(defmethod perform-effects events/control-menu-collapse [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "auto"))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state state/event-ch-path)
               (get-in app-state state/sign-in-email-path)
               (get-in app-state state/sign-in-password-path)))

(defmethod perform-effects events/control-sign-up-submit [_ event args app-state]
  (api/sign-up (get-in app-state state/event-ch-path)
               (get-in app-state state/sign-up-email-path)
               (get-in app-state state/sign-up-password-path)
               (get-in app-state state/sign-up-password-confirmation-path)))

(defmethod perform-effects events/control-sign-out [_ event args app-state]
  (cookie-jar/clear (get-in app-state state/cookie-path)))

(defmethod perform-effects events/control-browse-add-to-bag [_ event _ app-state]
  (let [product (query/get (get-in app-state state/browse-product-query-path)
                           (vals (get-in app-state state/products-path)))
        variant (query/get (get-in app-state state/browse-variant-query-path)
                           (:variants product))]
    (api/add-to-bag (get-in app-state state/event-ch-path)
                    (variant :id)
                    (get-in app-state state/browse-variant-quantity-path)
                    (get-in app-state state/user-order-token-path)
                    (get-in app-state state/user-order-id-path)
                    (get-in app-state state/user-token-path))))

(defmethod perform-effects events/control-forgot-password-submit [_ event args app-state]
  (api/forgot-password (get-in app-state state/event-ch-path)
                       (get-in app-state state/forgot-password-email-path)))

(defmethod perform-effects events/control-reset-password-submit [_ event args app-state]
  (api/reset-password (get-in app-state state/event-ch-path)
                      (get-in app-state state/reset-password-password-path)
                      (get-in app-state state/reset-password-password-confirmation-path)
                      (get-in app-state state/reset-password-token-path)))

(defn save-cookie [app-state remember?]
  (cookie-jar/save (get-in app-state state/cookie-path)
                   (get-in app-state state/user-path)
                   {:remember? remember?}))

(defmethod perform-effects events/control-manage-account-submit [_ event args app-state]
  (api/update-account (get-in app-state state/event-ch-path)
                      (get-in app-state state/user-id-path)
                      (get-in app-state state/manage-account-email-path)
                      (get-in app-state state/manage-account-password-path)
                      (get-in app-state state/manage-account-password-confirmation-path)
                      (get-in app-state state/user-token-path)))

(defn updated-quantities [line-items quantities]
  (->>
   line-items
   (map #(select-keys % [:id :quantity :variant_id]))
   (map #(assoc % :quantity (-> % :id quantities)))))

(defmethod perform-effects events/control-cart-update [_ event args app-state]
  (let [order (get-in app-state state/order-path)]
    (api/update-order
     (get-in app-state state/event-ch-path)
     (get-in app-state state/user-token-path)
     (merge (select-keys order [:id :number :token])
            {:line_items_attributes (updated-quantities
                                     (:line_items order)
                                     (get-in app-state state/cart-quantities-path))}))))

(defmethod perform-effects events/control-cart-remove [_ event args app-state]
  (let [order (get-in app-state state/order-path)]
    (api/remove-line-item
     (get-in app-state state/event-ch-path)
     (:number order)
     (:id args)
     (:token order))))

(defmethod perform-effects events/api-success-remove-item [_ event args app-state]
  (let [order (get-in app-state state/order-path)]
    (api/get-order
     (get-in app-state state/event-ch-path)
     (:number order)
     (:token order))))

(defmethod perform-effects events/control-checkout-update-addresses-submit [_ event args app-state]
  (let [event-ch (get-in app-state state/event-ch-path)
        token (get-in app-state state/user-token-path)
        addresses {:bill_address (get-in app-state state/checkout-billing-address-path)
                   :ship_address (get-in app-state
                                         (if (get-in app-state state/checkout-shipping-address-use-billing-address-path)
                                           state/checkout-billing-address-path
                                           state/checkout-shipping-address-path))}]
    (when (get-in app-state state/checkout-billing-address-save-my-address-path)
      (api/update-account-address event-ch
                                  (get-in app-state state/user-id-path)
                                  (get-in app-state state/user-email-path)
                                  (:bill_address addresses)
                                  (:ship_address addresses)
                                  token))
    (api/update-order event-ch token (merge (get-in app-state state/order-path)
                                            addresses))))

(defmethod perform-effects events/control-checkout-shipping-method-submit [_ event args app-state]
  (api/update-order (get-in app-state state/event-ch-path)
                    (get-in app-state state/user-token-path)
                    (let [order (get-in app-state state/order-path)]
                      (merge (select-keys order [:id :number :token])
                             {:shipments_attributes
                              {:id (get-in order [:shipments 0 :id])
                               :selected_shipping_rate_id (get-in app-state state/checkout-selected-shipping-method-id)}}))))

(defmethod perform-effects events/api-success-sign-in [_ event args app-state]
  (save-cookie app-state (get-in app-state state/sign-in-remember-path))
  (when (= (get-in app-state state/navigation-event-path) events/navigate-sign-in)
    (routes/enqueue-navigate app-state events/navigate-home))
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Logged in successfully"
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Welcome! You have signed up successfully."
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home))

(defmethod perform-effects events/api-success-forgot-password [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "You will receive an email with instructions on how to reset your password in a few minutes."
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-reset-password [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Your password was changed successfully. You are now signed in."
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-manage-account [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Account updated"
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-get-order [_ event args app-state]
  (save-cookie app-state true))

(defmethod perform-effects events/api-success-update-order [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-checkout-delivery))
