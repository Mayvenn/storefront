(ns storefront.transitions
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.routes :as routes]
            [storefront.taxons :refer [taxon-path-for]]))

(defn clear-fields [app-state & fields]
  (reduce #(assoc-in %1 %2 "") app-state fields))

(defmulti transition-state identity)
(defmethod transition-state [] [dispatch event args app-state]
  ;; (js/console.log (clj->js event) (clj->js args)) ;; enable to see all events
  app-state)
(defmethod transition-state :default [dispatch event args app-state]
  ;; (js/console.log "IGNORED transition" (clj->js event) (clj->js args)) ;; enable to see ignored transitions
  app-state)

(defmethod transition-state events/navigate [_ event args app-state]
  (assoc-in app-state state/navigation-event-path event))

(defmethod transition-state events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (assoc-in app-state state/browse-taxon-query-path {taxon-path-for taxon-path}))

(defmethod transition-state events/navigate-product [_ event {:keys [product-path query-params]} app-state]
  (let [taxon-id (js/parseInt (:taxon_id query-params))]
    (-> app-state
        (assoc-in state/browse-taxon-query-path {:id taxon-id})
        (assoc-in state/browse-product-query-path {:slug product-path})
        (assoc-in state/browse-variant-query-path nil)
        (assoc-in state/browse-variant-quantity-path 1)
        (assoc-in state/browse-recently-added-variants-path []))))

(defmethod transition-state events/navigate-reset-password [_ event {:keys [reset-token]} app-state]
  (assoc-in app-state state/reset-password-token-path reset-token))

(defmethod transition-state events/navigate-manage-account [_ event args app-state]
  (assoc-in app-state
            state/manage-account-email-path
            (get-in app-state state/user-email-path)))

(defmethod transition-state events/navigate-checkout-address [_ event args app-state]
  (-> app-state
      (assoc-in state/checkout-current-step-path "address")
      (update-in state/checkout-billing-address-path merge (get-in app-state state/billing-address-path))
      (update-in state/checkout-shipping-address-path merge (get-in app-state state/shipping-address-path))))

(defmethod transition-state events/navigate-checkout-delivery [_ event args app-state]
  (assoc-in app-state state/checkout-current-step-path "delivery"))

(defmethod transition-state events/control-menu-expand [_ event args app-state]
  (assoc-in app-state state/menu-expanded-path true))

(defmethod transition-state events/control-menu-collapse [_ event args app-state]
  (assoc-in app-state state/menu-expanded-path false))

(defmethod transition-state events/control-account-menu-expand [_ event args app-state]
  (assoc-in app-state state/account-menu-expanded-path true))

(defmethod transition-state events/control-account-menu-collapse [_ event args app-state]
  (assoc-in app-state state/account-menu-expanded-path false))

(defmethod transition-state events/control-sign-in-change [_ event args app-state]
  (update-in app-state state/sign-in-path merge args))

(defmethod transition-state events/control-sign-up-change [_ event args app-state]
  (update-in app-state state/sign-up-path merge args))

(defmethod transition-state events/control-sign-out [_ event args app-state]
  ;; FIXME clear other user specific pieces of state
  (assoc-in app-state state/user-path {}))

(defmethod transition-state events/control-manage-account-change [_ event args app-state]
  (update-in app-state state/manage-account-path merge args))

(defmethod transition-state events/control-browse-variant-select [_ event {:keys [variant]} app-state]
  (assoc-in app-state state/browse-variant-query-path {:id (variant :id)}))

(defmethod transition-state events/control-browse-add-to-bag [_ event args app-state]
  app-state)

(defmethod transition-state events/control-counter-inc [_ event args app-state]
  (update-in app-state (:path args) inc))

(defmethod transition-state events/control-counter-dec [_ event args app-state]
  (update-in app-state (:path args) (comp (partial max 1) dec)))

(defmethod transition-state events/control-counter-set [_ event {:keys [path value-str]} app-state]
  (assoc-in app-state path
            (-> (js/parseInt value-str 10)
                (Math/abs)
                (max 1))))

(defmethod transition-state events/control-cart-coupon-change [_ event {coupon-code :coupon-code} app-state]
  (assoc-in app-state state/cart-coupon-code-path coupon-code))

(defmethod transition-state events/control-forgot-password-change [_ event args app-state]
  (update-in app-state state/forgot-password-path merge args))

(defmethod transition-state events/control-reset-password-change [_ event args app-state]
  (update-in app-state state/reset-password-path merge args))

(defmethod transition-state events/control-checkout-change [_ event args app-state]
  (reduce-kv (fn [m k v]
               (assoc-in app-state k v))
             app-state
             args))

(defmethod transition-state events/control-checkout-shipping-method-select [_ event {id :id} app-state]
  (assoc-in app-state state/checkout-selected-shipping-method-id id))

(defmethod transition-state events/api-success-taxons [_ event args app-state]
  (assoc-in app-state state/taxons-path (:taxons args)))

(defmethod transition-state events/api-success-store [_ event args app-state]
  (assoc-in app-state state/store-path args))

(defmethod transition-state events/api-success-products [_ event {:keys [taxon-path products]} app-state]
  (update-in app-state state/products-path
             merge
             (->> products
                  (mapcat (fn [p] [(:id p) p]))
                  (apply hash-map))))

(defmethod transition-state events/api-success-product [_ event {:keys [product-path product]} app-state]
  (-> app-state
      (assoc-in state/browse-product-query-path {:slug product-path})
      (assoc-in (conj state/products-path (:id product)) product)))

(defmethod transition-state events/api-success-states [_ event {:keys [states]} app-state]
  (assoc-in app-state state/states-path states))

(defmethod transition-state events/api-success-stylist-commissions
  [_ event {:keys [rate next-amount paid-total new-orders payouts]} app-state]
  (-> app-state
      (assoc-in state/stylist-commissions-rate-path rate)
      (assoc-in state/stylist-commissions-next-amount-path next-amount)
      (assoc-in state/stylist-commissions-paid-total-path paid-total)
      (assoc-in state/stylist-commissions-new-orders-path new-orders)
      (assoc-in state/stylist-commissions-payouts-path payouts)))

(defmethod transition-state events/api-success-stylist-bonus-credits
  [_ event {:keys [bonuses bonus-amount earning-amount commissioned-revenue total-credit available-credit]} app-state]
  (-> app-state
      (assoc-in state/stylist-bonus-credit-bonuses-path bonuses)
      (assoc-in state/stylist-bonus-credit-bonus-amount-path bonus-amount)
      (assoc-in state/stylist-bonus-credit-earning-amount-path earning-amount)
      (assoc-in state/stylist-bonus-credit-commissioned-revenue-path commissioned-revenue)
      (assoc-in state/stylist-bonus-credit-total-credit-path total-credit)
      (assoc-in state/stylist-bonus-credit-available-credit-path available-credit)))

(defmethod transition-state events/api-success-stylist-referral-program
  [_ event {:keys [sales-rep-email bonus-amount earning-amount total-amount referrals]} app-state]
  (-> app-state
      (assoc-in state/stylist-referral-program-bonus-amount-path bonus-amount)
      (assoc-in state/stylist-referral-program-earning-amount-path earning-amount)
      (assoc-in state/stylist-referral-program-total-amount-path total-amount)
      (assoc-in state/stylist-referral-program-referrals-path referrals)
      (assoc-in state/stylist-sales-rep-email-path sales-rep-email)))

(defn sign-in-user [app-state {:keys [email token store_slug id]}]
  (-> app-state
      (assoc-in state/user-id-path id)
      (assoc-in state/user-email-path email)
      (assoc-in state/user-token-path token)
      (assoc-in state/user-store-slug-path store_slug)))

(defmethod transition-state events/api-success-sign-in [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields state/sign-in-email-path
                    state/sign-in-password-path)))

(defmethod transition-state events/api-success-sign-up [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields state/sign-up-email-path
                    state/sign-up-password-path
                    state/sign-up-password-confirmation-path)))

(defmethod transition-state events/api-success-forgot-password [_ event args app-state]
  (clear-fields app-state state/forgot-password-email-path))

(defmethod transition-state events/api-success-reset-password [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields state/reset-password-password-path
                    state/reset-password-password-confirmation-path
                    state/reset-password-token-path)
      (assoc-in state/sign-in-remember-path true)))

(defmethod transition-state events/api-success-create-order [_ event {:keys [number token]} app-state]
  (-> app-state
      (assoc-in state/user-order-token-path token)
      (assoc-in state/user-order-id-path number)))

(defmethod transition-state events/api-success-add-to-bag [_ event {:keys [variant-id variant-quantity]} app-state]
  (-> app-state
      (update-in state/browse-recently-added-variants-path
                 conj
                 {:id variant-id
                  :quantity variant-quantity})))

(defmethod transition-state events/api-success-get-order [_ event order app-state]
  (-> app-state
      (assoc-in state/checkout-selected-shipping-method-id (get-in order [:shipments 0 :selected_shipping_rate :id]))
      (assoc-in state/order-path order)
      (assoc-in state/cart-quantities-path
                (into {} (map (juxt :id :quantity) (order :line_items))))))

(defmethod transition-state events/api-success-manage-account [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields state/manage-account-email-path
                    state/manage-account-password-path
                    state/manage-account-password-confirmation-path)))

(defmethod transition-state events/api-success-account-update-addresses [_ event {:keys [billing-address shipping-address]} app-state]
  (-> app-state
      (merge {:billing-address billing-address
              :shipping-address shipping-address})
      (update-in state/checkout-billing-address-path merge billing-address)
      (update-in state/checkout-shipping-address-path merge shipping-address)))

(defmethod transition-state events/api-success-sms-number [_ event args app-state]
  (assoc-in app-state state/sms-number-path (:number args)))

(defmethod transition-state events/api-success-update-order [_ event order app-state]
  (-> app-state
      (assoc-in state/order-path order)
      (assoc-in state/cart-coupon-code-path "")))

(defmethod transition-state events/api-success-promotions [_ event {promotions :promotions} app-state]
  (assoc-in app-state state/promotions promotions))

(defmethod transition-state events/flash-show-success [_ event args app-state]
  (assoc-in app-state state/flash-success-path (select-keys args [:message :navigation])))

(defmethod transition-state events/flash-dismiss-success [_ event args app-state]
  (assoc-in app-state state/flash-success-path nil))
