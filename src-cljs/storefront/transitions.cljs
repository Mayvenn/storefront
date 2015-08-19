(ns storefront.transitions
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.accessors.taxons :refer [taxon-path-for]]
            [storefront.accessors.orders :as orders]
            [storefront.state :as state]))

(defn clear-fields [app-state & fields]
  (reduce #(assoc-in %1 %2 "") app-state fields))

(defmulti transition-state identity)

(defmethod transition-state :default [dispatch event args app-state]
  ;; (js/console.log "IGNORED transition" (clj->js event) (clj->js args)) ;; enable to see ignored transitions
  app-state)

(defmethod transition-state events/navigate [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/validation-errors {})
      (assoc-in keypaths/navigation-message [event args])))

(defmethod transition-state events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (assoc-in app-state keypaths/browse-taxon-query {taxon-path-for taxon-path}))

(defmethod transition-state events/navigate-product [_ event {:keys [product-path query-params]} app-state]
  (let [taxon-id (js/parseInt (:taxon-id query-params))]
    (-> app-state
        (assoc-in keypaths/browse-taxon-query {:id taxon-id})
        (assoc-in keypaths/browse-product-query {:slug product-path})
        (assoc-in keypaths/browse-variant-query nil)
        (assoc-in keypaths/browse-variant-quantity 1)
        (assoc-in keypaths/browse-recently-added-variants []))))

(defmethod transition-state events/navigate-stylist [_ event args app-state]
  (assoc-in app-state keypaths/return-navigation-event event))

(defmethod transition-state events/navigate-reset-password [_ event {:keys [reset-token]} app-state]
  (assoc-in app-state keypaths/reset-password-token reset-token))

(defmethod transition-state events/navigate-manage-account [_ event args app-state]
  (assoc-in app-state
            keypaths/manage-account-email
            (get-in app-state keypaths/user-email)))

(defmethod transition-state events/navigate-checkout [_ event args app-state]
  (assoc-in app-state keypaths/return-navigation-event event))

(defmethod transition-state events/navigate-stylist [_ event args app-state]
  (assoc-in app-state keypaths/return-navigation-event event))

(defmethod transition-state events/navigate-checkout-address [_ event args app-state]
  (-> app-state
      (update-in keypaths/checkout-billing-address merge (get-in app-state keypaths/billing-address))
      (update-in keypaths/checkout-shipping-address merge (get-in app-state keypaths/shipping-address))))

(defmethod transition-state events/navigate-checkout-delivery [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/checkout-selected-shipping-method (merge (first (get-in app-state keypaths/shipping-methods))
                                                                 (get-in app-state keypaths/order-shipping-method)))))

(defmethod transition-state events/navigate-checkout-payment [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/checkout-use-store-credits
                (pos? (get-in app-state keypaths/user-total-available-store-credit)))))

(defmethod transition-state events/navigate-order [_ event args app-state]
  (assoc-in app-state keypaths/past-order-id (args :order-id)))

(defmethod transition-state events/control-menu-expand
  [_ event {keypath :keypath} app-state]
  (assoc-in app-state keypath true))

(defmethod transition-state events/control-menu-collapse
  [_ event {keypath :keypath} app-state]
  (assoc-in app-state keypath false))

(defmethod transition-state events/control-sign-out [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/user {})
      (assoc-in keypaths/order nil)
      (assoc-in keypaths/past-orders {})
      (assoc-in keypaths/my-order-ids nil)
      (assoc-in keypaths/stylist {})
      (assoc-in keypaths/billing-address {})
      (assoc-in keypaths/shipping-address {})))

(defmethod transition-state events/control-change-state
  [_ event {:keys [keypath value]} app-state]
  (assoc-in app-state keypath (if (fn? value) (value) value)))

(defmethod transition-state events/control-browse-variant-select [_ event {:keys [variant]} app-state]
  (assoc-in app-state keypaths/browse-variant-query {:id (variant :id)}))

(defmethod transition-state events/control-counter-inc [_ event args app-state]
  (update-in app-state (:path args) inc))

(defmethod transition-state events/control-counter-dec [_ event args app-state]
  (update-in app-state (:path args) (comp (partial max 1) dec)))

(defmethod transition-state events/control-counter-set [_ event {:keys [path value-str]} app-state]
  (assoc-in app-state path
            (-> (js/parseInt value-str 10)
                (Math/abs)
                (max 1))))

(defmethod transition-state events/control-checkout-shipping-method-select [_ event shipping-method app-state]
  (assoc-in app-state keypaths/checkout-selected-shipping-method shipping-method))

(defmethod transition-state events/api-start
  [_ event request app-state]
  (update-in app-state keypaths/api-requests conj request))

(defmethod transition-state events/api-end
  [_ event {:keys [request-id] :as request} app-state]
  (update-in app-state keypaths/api-requests (partial remove (comp #{request-id} :request-id))))

(defmethod transition-state events/api-success-taxons [_ event args app-state]
  (assoc-in app-state keypaths/taxons (:taxons args)))

(defmethod transition-state events/api-success-store [_ event args app-state]
  (assoc-in app-state keypaths/store args))

(defmethod transition-state events/api-success-products [_ event {:keys [taxon-path products]} app-state]
  (update-in app-state keypaths/products
             merge
             (->> products
                  (mapcat
                   (fn [idx p] [(:id p) (assoc p :index idx)])
                   (range))
                  (apply hash-map))))

(defmethod transition-state events/api-success-product [_ event {:keys [product-path product]} app-state]
  (-> app-state
      (assoc-in keypaths/browse-product-query {:slug product-path})
      (assoc-in (conj keypaths/products (:id product)) product)))

(defmethod transition-state events/api-success-states [_ event {:keys [states]} app-state]
  (assoc-in app-state keypaths/states states))

(defmethod transition-state events/api-success-payment-methods [_ event {:keys [payment_methods]} app-state]
  (assoc-in app-state keypaths/payment-methods payment_methods))

(defmethod transition-state events/api-success-stylist-manage-account
  [_ event {:keys [stylist]} app-state]
  (-> app-state
      (assoc-in keypaths/validation-errors {})
      (update-in keypaths/stylist-manage-account merge stylist)
      (update-in keypaths/store merge (select-keys stylist [:instagram_account :profile_picture_url]))))

(defmethod transition-state events/api-success-stylist-commissions
  [_ event {:keys [rate next-amount paid-total new-orders payouts]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-commissions-rate rate)
      (assoc-in keypaths/stylist-commissions-next-amount next-amount)
      (assoc-in keypaths/stylist-commissions-paid-total paid-total)
      (assoc-in keypaths/stylist-commissions-new-orders new-orders)
      (assoc-in keypaths/stylist-commissions-payouts payouts)))

(defmethod transition-state events/api-success-stylist-bonus-credits
  [_ event {:keys [bonuses bonus-amount earning-amount commissioned-revenue total-credit available-credit]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-bonus-credit-bonuses bonuses)
      (assoc-in keypaths/stylist-bonus-credit-bonus-amount bonus-amount)
      (assoc-in keypaths/stylist-bonus-credit-earning-amount earning-amount)
      (assoc-in keypaths/stylist-bonus-credit-commissioned-revenue commissioned-revenue)
      (assoc-in keypaths/stylist-bonus-credit-total-credit total-credit)
      (assoc-in keypaths/stylist-bonus-credit-available-credit available-credit)))

(defmethod transition-state events/api-success-stylist-referral-program
  [_ event {:keys [sales-rep-email bonus-amount earning-amount total-amount referrals]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-referral-program-bonus-amount bonus-amount)
      (assoc-in keypaths/stylist-referral-program-earning-amount earning-amount)
      (assoc-in keypaths/stylist-referral-program-total-amount total-amount)
      (assoc-in keypaths/stylist-referral-program-referrals referrals)
      (assoc-in keypaths/stylist-sales-rep-email sales-rep-email)))

(defn sign-in-user
  [app-state {:keys [email token store_slug id total_available_store_credit]}]
  (-> app-state
      (assoc-in keypaths/user-id id)
      (assoc-in keypaths/user-email email)
      (assoc-in keypaths/user-token token)
      (assoc-in keypaths/user-store-slug store_slug)
      (assoc-in keypaths/user-total-available-store-credit (js/parseFloat total_available_store_credit))))

(defmethod transition-state events/api-success-sign-in [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields keypaths/sign-in-email
                    keypaths/sign-in-password)))

(defmethod transition-state events/api-success-sign-up [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields keypaths/sign-up-email
                    keypaths/sign-up-password
                    keypaths/sign-up-password-confirmation)))

(defmethod transition-state events/api-success-forgot-password [_ event args app-state]
  (clear-fields app-state keypaths/forgot-password-email))

(defmethod transition-state events/api-success-reset-password [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields keypaths/reset-password-password
                    keypaths/reset-password-password-confirmation
                    keypaths/reset-password-token)
      (assoc-in keypaths/sign-in-remember true)))

(defmethod transition-state events/api-success-add-to-bag [_ event {:keys [order requested]} app-state]
  (-> app-state
      (update-in keypaths/browse-recently-added-variants conj requested)
      (assoc-in keypaths/cart-quantities
                (into {} (map (fn [[k v]] [k (:quantity v)]) (:line-items order))))
      (update-in keypaths/order merge order)))

(defmethod transition-state events/api-success-get-order [_ event order app-state]
  (if (orders/incomplete? order)
    (-> app-state
        (assoc-in keypaths/order order)
        (assoc-in keypaths/checkout-selected-shipping-method (merge (first (get-in app-state keypaths/shipping-methods))
                                                                    (:shipping-method order)))
        (assoc-in keypaths/cart-quantities
                  (into {} (map (fn [[k v]] [k (:quantity v)]) (:line-items order)))))
    app-state))

(defmethod transition-state events/api-success-get-past-order [_ event order app-state]
  (update-in app-state keypaths/past-orders merge {(:number order) order}))

(defmethod transition-state events/api-success-my-orders [_ event {orders :orders} app-state]
  (let [order-ids (map :number orders)]
    (-> app-state
        (assoc-in keypaths/my-order-ids order-ids)
        (update-in keypaths/past-orders merge (zipmap order-ids orders)))))

(defmethod transition-state events/api-success-manage-account [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields keypaths/manage-account-email
                    keypaths/manage-account-password
                    keypaths/manage-account-password-confirmation)))

(defn default-checkout-addresses [app-state billing-address shipping-address]
  (-> app-state
      (update-in keypaths/checkout-billing-address merge billing-address)
      (update-in keypaths/checkout-shipping-address merge shipping-address)))

(defn default-credit-card-name [app-state {:keys [firstname lastname]}]
  (assoc-in app-state keypaths/checkout-credit-card-name (str firstname " " lastname)))

(defmethod transition-state events/api-success-shipping-methods [_ events {:keys [shipping-methods]} app-state]
  (-> app-state
      (assoc-in keypaths/shipping-methods shipping-methods)
      (assoc-in keypaths/checkout-selected-shipping-method (merge (first (get-in app-state keypaths/shipping-methods))
                                                                  (get-in app-state keypaths/order-shipping-method)))))

(defn update-account-address [app-state {:keys [billing-address shipping-address] :as args}]
  (-> app-state
      (merge {:billing-address billing-address
              :shipping-address shipping-address})
      (default-credit-card-name billing-address)))

(defmethod transition-state events/api-success-address [_ event args app-state]
  (update-account-address app-state args))

(defmethod transition-state events/api-success-account [_ event {:keys [billing-address shipping-address] :as args} app-state]
  (-> app-state
      (sign-in-user args)
      (update-account-address args)
      (default-checkout-addresses billing-address shipping-address)))

(defmethod transition-state events/api-success-sms-number [_ event args app-state]
  (assoc-in app-state keypaths/sms-number (:number args)))

(defmethod transition-state events/api-success-update-order [_ event {:keys [order]} app-state]
  (if (orders/incomplete? order)
    (assoc-in app-state keypaths/order order)
    (-> app-state
        (assoc-in keypaths/last-order order)
        (assoc-in keypaths/checkout state/initial-checkout-state)
        (assoc-in keypaths/order {}))))

(defmethod transition-state events/api-success-cart-update [_ event {:keys [order]} app-state]
  (assoc-in app-state keypaths/order order))

(defmethod transition-state events/api-success-cart-update-coupon [_ event _ app-state]
  (assoc-in app-state keypaths/cart-coupon-code ""))

(defmethod transition-state events/api-success-promotions [_ event {promotions :promotions} app-state]
  (assoc-in app-state keypaths/promotions promotions))

(defmethod transition-state events/api-success-cache [_ event new-data app-state]
  (update-in app-state keypaths/api-cache merge new-data))

(defmethod transition-state events/api-failure-validation-errors [_ event validation-errors app-state]
  (assoc-in app-state keypaths/validation-errors validation-errors))

(defmethod transition-state events/flash-show-success [_ event args app-state]
  (assoc-in app-state keypaths/flash-success (select-keys args [:message :navigation])))

(defmethod transition-state events/flash-dismiss-success [_ event args app-state]
  (assoc-in app-state keypaths/flash-success nil))

(defmethod transition-state events/flash-show-failure [_ event args app-state]
  (assoc-in app-state keypaths/flash-failure (select-keys args [:message :navigation])))

(defmethod transition-state events/flash-dismiss-failure [_ event args app-state]
  (assoc-in app-state keypaths/flash-failure nil))

(defmethod transition-state events/optimizely
  [_ event {:keys [variation]} app-state]
  (update-in app-state keypaths/optimizely-variations conj variation))

(defmethod transition-state events/reviews-inserted [_ event args app-state]
  (assoc-in app-state keypaths/reviews-loaded true))
