(ns storefront.transitions
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.accessors.taxons :refer [taxon-path-for]]
            [storefront.accessors.orders :as orders]
            [storefront.hooks.talkable :as talkable]
            [storefront.state :as state]
            [storefront.utils.combinators :refer [map-values key-by]]
            [storefront.utils.sequences :refer [index-sequence]]
            [clojure.string :as string]))

(defn clear-fields [app-state & fields]
  (reduce #(assoc-in %1 %2 "") app-state fields))

(defmulti transition-state identity)

(defmethod transition-state :default [dispatch event args app-state]
  ;; (js/console.log "IGNORED transition" (clj->js event) (clj->js args)) ;; enable to see ignored transitions
  app-state)

(def return-event-blacklisted? #{events/navigate-not-found
                                 events/navigate-sign-in
                                 events/navigate-sign-up
                                 events/navigate-forgot-password
                                 events/navigate-reset-password
                                 events/navigate-checkout-sign-in
                                 events/navigate-sign-in-getsat})

(defn add-return-event [app-state]
  (let [[return-event return-args] (get-in app-state keypaths/navigation-message)]
    (if (return-event-blacklisted? return-event)
      app-state
      (assoc-in app-state keypaths/return-navigation-message [return-event return-args]))))

(defn add-pending-promo-code [app-state args]
  (let [{{sha :sha} :query-params} args]
    (if sha
      (assoc-in app-state keypaths/pending-promo-code sha)
      app-state)))

(defmethod transition-state events/app-start [_ event args app-state]
  (assoc-in app-state keypaths/store (js->clj js/store :keywordize-keys true)))

(defmethod transition-state events/navigate [_ event args app-state]
  (-> app-state
      add-return-event
      (add-pending-promo-code args)
      (assoc-in keypaths/previous-navigation-message
                (get-in app-state keypaths/navigation-message))
      (assoc-in keypaths/validation-errors {})
      (assoc-in keypaths/navigation-message [event args])))

(defmethod transition-state events/navigate-sign-in-getsat [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/get-satisfaction-login? true)
      (assoc-in keypaths/return-navigation-message [event args])))

(defmethod transition-state events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (-> app-state
      (assoc-in keypaths/browse-taxon-query {taxon-path-for taxon-path})
      (assoc-in keypaths/browse-recently-added-variants [])
      (assoc-in keypaths/browse-variant-quantity 1)
      (assoc-in keypaths/bundle-builder nil)))

(defmethod transition-state events/navigate-product [_ event {:keys [product-path query-params]} app-state]
  (let [taxon-id (js/parseInt (:taxon-id query-params))]
    (-> app-state
        (assoc-in keypaths/browse-taxon-query {:id taxon-id})
        (assoc-in keypaths/browse-product-query {:slug product-path})
        (assoc-in keypaths/browse-variant-query nil)
        (assoc-in keypaths/browse-variant-quantity 1)
        (assoc-in keypaths/browse-recently-added-variants []))))

(defmethod transition-state events/navigate-reset-password [_ event {:keys [reset-token]} app-state]
  (assoc-in app-state keypaths/reset-password-token reset-token))

(defmethod transition-state events/navigate-account-manage [_ event args app-state]
  (assoc-in app-state
            keypaths/manage-account-email
            (get-in app-state keypaths/user-email)))

(defmethod transition-state events/control-commission-order-expand [_ _ {:keys [number]} app-state]
  (assoc-in app-state keypaths/expanded-commission-order-id #{number}))

(defmethod transition-state events/navigate-checkout-address [_ event args app-state]
  (when (get-in app-state keypaths/user-email)
    (assoc-in app-state keypaths/navigation-message
              [event {:query-params {:loggedin true}}]))) ;; help with analytics of funnel

(defn ensure-cart-has-shipping-method [app-state]
  (-> app-state
      (assoc-in keypaths/checkout-selected-shipping-method
                (merge (first (get-in app-state keypaths/shipping-methods))
                       (orders/shipping-item (:order app-state))))))

(defmethod transition-state events/navigate-checkout-delivery [_ event args app-state]
  (ensure-cart-has-shipping-method app-state))

(defmethod transition-state events/navigate-checkout-confirmation [_ event args app-state]
  (ensure-cart-has-shipping-method app-state))

(defmethod transition-state events/control-checkout-payment-method-submit [_ _ _ app-state]
  (assoc-in app-state keypaths/checkout-selected-payment-methods
            (orders/form-payment-methods (get-in app-state keypaths/order-total)
                                         (get-in app-state keypaths/user-total-available-store-credit))))

(def menus #{keypaths/menu-expanded
             keypaths/account-menu-expanded
             keypaths/shop-menu-expanded})

(defmethod transition-state events/control-menu-expand
  [_ event {keypath :keypath} app-state]
  (reduce (fn [state menu] (assoc-in state menu (= menu keypath)))
          app-state
          menus))

(defmethod transition-state events/control-menu-collapse-all
  [_ event {keypath :keypath} app-state]
  (reduce (fn [state menu] (assoc-in state menu false))
          app-state
          menus))

(defmethod transition-state events/control-sign-out [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/user {})
      (assoc-in keypaths/order nil)
      (assoc-in keypaths/stylist {})
      (assoc-in keypaths/checkout state/initial-checkout-state)
      (assoc-in keypaths/billing-address {})
      (assoc-in keypaths/shipping-address {})
      (assoc-in keypaths/facebook-email-denied nil)))

(defmethod transition-state events/control-change-state
  [_ event {:keys [keypath value]} app-state]
  (assoc-in app-state keypath (if (fn? value) (value) value)))

(defmethod transition-state events/control-browse-variant-select [_ event {:keys [variant]} app-state]
  (assoc-in app-state keypaths/browse-variant-query {:id (:id variant)}))

(defmethod transition-state events/control-counter-inc [_ event args app-state]
  (update-in app-state (:path args) inc))

(defmethod transition-state events/control-counter-dec [_ event args app-state]
  (update-in app-state (:path args) (comp (partial max 1) dec)))

(defmethod transition-state events/control-counter-set [_ event {:keys [path value-str]} app-state]
  (assoc-in app-state path
            (-> (js/parseInt value-str 10)
                (Math/abs)
                (max 1))))

(defmethod transition-state events/control-bundle-option-select
  [_ event {:keys [selected-options selected-variants step-name], :as args} app-state]
  (-> app-state
      (assoc-in keypaths/bundle-builder-selected-options selected-options)
      (assoc-in keypaths/bundle-builder-previous-step step-name)
      (assoc-in keypaths/bundle-builder-selected-variants selected-variants)))

(defmethod transition-state events/control-checkout-shipping-method-select [_ event shipping-method app-state]
  (assoc-in app-state keypaths/checkout-selected-shipping-method shipping-method))

(defmethod transition-state events/control-carousel-move [_ event {:keys [index]} app-state]
  (assoc-in app-state keypaths/bundle-builder-carousel-index index))

(defmethod transition-state events/control-stylist-view-stat [_ _ stat app-state]
  (assoc-in app-state keypaths/selected-stylist-stat stat))

(defmethod transition-state events/control-checkout-as-guest-submit [_ event args app-state]
  (assoc-in app-state keypaths/checkout-as-guest true))

(defmethod transition-state events/control-checkout-cart-paypal-setup [_ event args app-state]
  (assoc-in app-state keypaths/cart-paypal-redirect true))

(defmethod transition-state events/api-start
  [_ event request app-state]
  (update-in app-state keypaths/api-requests conj request))

(defmethod transition-state events/api-end
  [_ event {:keys [request-id] :as request} app-state]
  (update-in app-state keypaths/api-requests (partial remove (comp #{request-id} :request-id))))

(defmethod transition-state events/api-success-taxons [_ event args app-state]
  (assoc-in app-state keypaths/taxons (:taxons args)))

(defmethod transition-state events/api-success-taxon-products [_ event {:keys [taxon-path products]} app-state]
  (-> app-state
      (update-in keypaths/taxon-product-order
                 assoc taxon-path (map :id products))
      (update-in keypaths/products
                 merge (into {} (map #(vector (:id %) %) products)))))

(defmethod transition-state events/api-success-product [_ event {:keys [product]} app-state]
  (-> app-state
      (assoc-in (conj keypaths/products (:id product)) product)))

(defmethod transition-state events/api-success-order-products [_ event {:keys [products]} app-state]
  (-> app-state
      (update-in keypaths/products merge (key-by :id products))))

(defmethod transition-state events/api-success-states [_ event {:keys [states]} app-state]
  (assoc-in app-state keypaths/states states))

(defmethod transition-state events/api-success-stylist-manage-account
  [_ event {:keys [stylist]} app-state]
  (-> app-state
      (assoc-in keypaths/validation-errors {})
      (update-in keypaths/stylist-manage-account merge (assoc stylist :original_payout_method (:chosen_payout_method stylist)))
      (update-in keypaths/store merge (select-keys stylist [:instagram_account :profile_picture_url]))))

(defmethod transition-state events/api-success-stylist-stats [_ events stats app-state]
  (-> app-state
      (assoc-in keypaths/stylist-stats (select-keys stats [:previous-payout :next-payout :lifetime-payouts]))))

(defmethod transition-state events/api-success-stylist-commissions
  [_ event {:keys [rate commissions pages current-page]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-commissions-rate rate)
      (assoc-in keypaths/stylist-commissions-pages (or pages 0))
      (assoc-in keypaths/stylist-commissions-page (or current-page 1))
      (update-in keypaths/stylist-commissions-history into commissions)))

(defmethod transition-state events/api-success-stylist-bonus-credits
  [_ event {:keys [bonuses bonus-amount earning-amount progress-to-next-bonus lifetime-total current-page pages]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-bonuses-award-amount bonus-amount)
      (assoc-in keypaths/stylist-bonuses-milestone-amount earning-amount)
      (assoc-in keypaths/stylist-bonuses-progress-to-next-bonus progress-to-next-bonus)
      (assoc-in keypaths/stylist-bonuses-lifetime-total lifetime-total)
      (update-in keypaths/stylist-bonuses-history into bonuses)
      (assoc-in keypaths/stylist-bonuses-page (or current-page 1))
      (assoc-in keypaths/stylist-bonuses-pages (or pages 0))))

(defmethod transition-state events/api-success-stylist-referral-program
  [_ event {:keys [sales-rep-email bonus-amount earning-amount lifetime-total referrals current-page pages]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-referral-program-bonus-amount bonus-amount)
      (assoc-in keypaths/stylist-referral-program-earning-amount earning-amount)
      (assoc-in keypaths/stylist-referral-program-lifetime-total lifetime-total)
      (update-in keypaths/stylist-referral-program-referrals into referrals)
      (assoc-in keypaths/stylist-referral-program-pages (or pages 0))
      (assoc-in keypaths/stylist-referral-program-page (or current-page 1))
      (assoc-in keypaths/stylist-sales-rep-email sales-rep-email)))

(defn sign-in-user
  [app-state {:keys [email token store_slug id total_available_store_credit]}]
  (-> app-state
      (assoc-in keypaths/user-id id)
      (assoc-in keypaths/user-email email)
      (assoc-in keypaths/user-token token)
      (assoc-in keypaths/user-store-slug store_slug)
      (assoc-in keypaths/user-total-available-store-credit (js/parseFloat total_available_store_credit))
      (assoc-in keypaths/checkout-as-guest false)))

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

(defn updated-cart-quantities [order]
  (into {} (map (juxt :id :quantity) (orders/product-items order))))

(defmethod transition-state events/api-success-add-to-bag [_ event {:keys [order requested]} app-state]
  (-> app-state
      (update-in keypaths/browse-recently-added-variants conj requested)
      (assoc-in keypaths/browse-variant-quantity 1)
      (assoc-in keypaths/cart-quantities (updated-cart-quantities order))
      (update-in keypaths/order merge order)))

(defmethod transition-state events/api-success-remove-from-bag [_ event {:keys [order]} app-state]
  (-> app-state
      (assoc-in keypaths/cart-quantities (updated-cart-quantities order))
      (update-in keypaths/order merge order)))

(defmethod transition-state events/api-success-get-order [_ event order app-state]
  (if (orders/incomplete? order)
    (-> app-state
        (update-in keypaths/checkout-billing-address merge (:billing-address order))
        (update-in keypaths/checkout-shipping-address merge (:shipping-address order))
        (assoc-in keypaths/order order)
        (assoc-in keypaths/checkout-selected-shipping-method
                  (merge (first (get-in app-state keypaths/shipping-methods))
                         (orders/shipping-item order)))
        (assoc-in keypaths/cart-quantities (updated-cart-quantities order)))
    (assoc-in app-state keypaths/order {})))

(defmethod transition-state events/api-success-manage-account [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields keypaths/manage-account-email
                    keypaths/manage-account-password
                    keypaths/manage-account-password-confirmation)))

(defn default-credit-card-name [app-state {:keys [first-name last-name]}]
  (if (string/blank? (get-in app-state keypaths/checkout-credit-card-name))
    (assoc-in app-state keypaths/checkout-credit-card-name (str first-name " " last-name))
    app-state))

(defmethod transition-state events/api-success-shipping-methods [_ events {:keys [shipping-methods]} app-state]
  (-> app-state
      (assoc-in keypaths/shipping-methods shipping-methods)
      (assoc-in keypaths/checkout-selected-shipping-method
                (merge (first shipping-methods)
                       (orders/shipping-item (:order app-state))))))

(defn update-account-address [app-state {:keys [billing-address shipping-address] :as args}]
  (-> app-state
      (merge {:billing-address billing-address
              :shipping-address shipping-address})
      (default-credit-card-name billing-address)))

(def vals-empty? (comp (partial every? string/blank?) vals))

(defn default-checkout-addresses [app-state billing-address shipping-address]
  (if (vals-empty? (get-in app-state keypaths/checkout-billing-address))
    (-> app-state
        (assoc-in keypaths/checkout-billing-address billing-address)
        (assoc-in keypaths/checkout-shipping-address shipping-address))
    app-state))

(defmethod transition-state events/autocomplete-update-address [_ event {:keys [address address-keypath] :as args} app-state]
  (update-in app-state address-keypath merge address))

(defmethod transition-state events/api-success-account [_ event {:keys [billing-address shipping-address] :as args} app-state]
  (-> app-state
      (sign-in-user args)
      (update-account-address args)
      (default-checkout-addresses billing-address shipping-address)))

(defmethod transition-state events/api-success-update-order-add-promotion-code [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/cart-coupon-code "")
      (assoc-in keypaths/pending-promo-code nil)))

(defmethod transition-state events/api-success-sms-number [_ event args app-state]
  (assoc-in app-state keypaths/sms-number (:number args)))

(defmethod transition-state events/api-success-update-order [_ event {:keys [order]} app-state]
  (assoc-in app-state keypaths/order order))

(defmethod transition-state events/order-completed [_ event order app-state]
  (-> app-state
      (assoc-in keypaths/checkout state/initial-checkout-state)
      (assoc-in keypaths/order {})
      (assoc-in keypaths/pending-talkable-order (talkable/completed-order order))))

(defmethod transition-state events/api-success-promotions [_ event {promotions :promotions} app-state]
  (assoc-in app-state keypaths/promotions promotions))

(defmethod transition-state events/api-success-cache [_ event new-data app-state]
  (update-in app-state keypaths/api-cache merge new-data))

(defmethod transition-state events/api-failure-validation-errors [_ event validation-errors app-state]
  (assoc-in app-state keypaths/validation-errors validation-errors))

(defmethod transition-state events/api-failure-pending-promo-code [_ event args app-state]
  (assoc-in app-state keypaths/pending-promo-code nil))

(defmethod transition-state events/api-handle-order-not-found [_ _ _ app-state]
  (assoc-in app-state keypaths/order nil))

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

(defmethod transition-state events/inserted-places [_ event args app-state]
  (assoc-in app-state keypaths/loaded-places true))

(defmethod transition-state events/inserted-reviews [_ event args app-state]
  (assoc-in app-state keypaths/loaded-reviews true))

(defmethod transition-state events/inserted-stripe [_ event args app-state]
  (assoc-in app-state keypaths/loaded-stripe true))

(defmethod transition-state events/inserted-facebook [_ event args app-state]
  (assoc-in app-state keypaths/loaded-facebook true))

(defmethod transition-state events/inserted-talkable [_ event args app-state]
  (assoc-in app-state keypaths/loaded-talkable true))

(defmethod transition-state events/reviews-component-mounted [_ event args app-state]
  (update-in app-state keypaths/review-components-count inc))

(defmethod transition-state events/reviews-component-will-unmount [_ event args app-state]
  (update-in app-state keypaths/review-components-count dec))

(defmethod transition-state events/facebook-success-sign-in [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied nil))

(defmethod transition-state events/facebook-failure-sign-in [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied nil))

(defmethod transition-state events/facebook-email-denied [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied true))

(defmethod transition-state events/talkable-offer-shown [_ event args app-state]
  (assoc-in app-state keypaths/pending-talkable-order nil))
