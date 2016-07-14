(ns storefront.transitions
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.accessors.taxons :as taxons]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.accessors.experiments :as experiments]
            [storefront.hooks.talkable :as talkable]
            [storefront.state :as state]
            [storefront.utils.query :as query]
            [storefront.utils.combinators :refer [map-values key-by]]
            [clojure.string :as string]))

(defn clear-fields [app-state & fields]
  (reduce #(assoc-in %1 %2 "") app-state fields))

(defn collapse-menus
  ([app-state] (collapse-menus app-state nil))
  ([app-state menus]
   (reduce (fn [state menu] (assoc-in state menu false))
           app-state
           (or menus keypaths/menus))))

(defn clear-flash [app-state]
  (-> app-state
      (assoc-in keypaths/flash-success nil)
      (assoc-in keypaths/flash-failure nil)
      (assoc-in keypaths/validation-errors {:error-message nil :details {}})
      (assoc-in keypaths/errors {})))

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
                                 events/navigate-getsat-sign-in})

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

(defmethod transition-state events/navigate [_ event args app-state]
  (-> app-state
      collapse-menus
      add-return-event
      (add-pending-promo-code args)
      (assoc-in keypaths/previous-navigation-message
                (get-in app-state keypaths/navigation-message))
      clear-flash
      (assoc-in keypaths/navigation-message [event args])))

(defmethod transition-state events/navigate-getsat-sign-in [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/get-satisfaction-login? true)
      (assoc-in keypaths/return-navigation-message [event args])))

(defn initialize-bundle-builder [app-state]
  (let [bundle-builder (bundle-builder/initialize (taxons/current-taxon app-state)
                                                  (get-in app-state keypaths/products))
        saved-options  (get-in app-state keypaths/saved-bundle-builder-options)]
    (if saved-options
      (bundle-builder/reset-options bundle-builder saved-options)
      bundle-builder)))

(defn ensure-bundle-builder [app-state]
  (if (and (nil? (get-in app-state keypaths/bundle-builder))
           (taxons/products-loaded? app-state (taxons/current-taxon app-state)))
    (-> app-state
        (assoc-in keypaths/bundle-builder (initialize-bundle-builder app-state))
        (update-in keypaths/ui dissoc :saved-bundle-builder-options))
    app-state))

(defmethod transition-state events/navigate-category [_ event {:keys [taxon-slug]} app-state]
  (let [bundle-builder-options (-> (get-in app-state keypaths/bundle-builder)
                                   bundle-builder/constrained-options
                                   (dissoc :length))]
    (-> app-state
        (assoc-in (conj keypaths/browse-taxon-query :slug) taxon-slug)
        (assoc-in keypaths/browse-recently-added-variants [])
        (assoc-in keypaths/browse-variant-quantity 1)
        (assoc-in keypaths/bundle-builder nil)
        (assoc-in keypaths/saved-bundle-builder-options bundle-builder-options)
        ensure-bundle-builder)))

(defmethod transition-state events/navigate-reset-password [_ event {:keys [reset-token]} app-state]
  (assoc-in app-state keypaths/reset-password-token reset-token))

(defmethod transition-state events/navigate-account-manage [_ event args app-state]
  (assoc-in app-state
            keypaths/manage-account-email
            (get-in app-state keypaths/user-email)))

(defmethod transition-state events/control-commission-order-expand [_ _ {:keys [number]} app-state]
  (assoc-in app-state keypaths/expanded-commission-order-id #{number}))

(defn cart-message [code app-state]
  (case code
    "shared-cart" (str (get-in app-state (conj keypaths/store :store_nickname) "We") " made this bag just for you!")
    nil))

(defmethod transition-state events/navigate-cart [_ event args app-state]
  (if-let [source (-> args :query-params :message (cart-message app-state))]
    (assoc-in app-state keypaths/cart-source source)
    app-state))

(defmethod transition-state events/navigate-checkout-address [_ event args app-state]
  (cond-> app-state
    (get-in app-state keypaths/user-email)
    ;; help with analytics of funnel
    (assoc-in keypaths/navigation-message [event {:query-params {:loggedin true}}])

    true
    (assoc-in keypaths/places-enabled true)))

(defn ensure-cart-has-shipping-method [app-state]
  (-> app-state
      (assoc-in keypaths/checkout-selected-shipping-method
                (merge (first (get-in app-state keypaths/shipping-methods))
                       (orders/shipping-item (:order app-state))))))

(defmethod transition-state events/navigate-checkout-confirmation [_ event args app-state]
  (ensure-cart-has-shipping-method app-state))

(defmethod transition-state events/control-checkout-payment-method-submit [_ _ _ app-state]
  (assoc-in app-state keypaths/checkout-selected-payment-methods
            (orders/form-payment-methods (get-in app-state keypaths/order-total)
                                         (get-in app-state keypaths/user-total-available-store-credit))))

(defmethod transition-state events/control-menu-expand
  [_ event {keypath :keypath} app-state]
  (reduce (fn [state menu] (assoc-in state menu (= menu keypath)))
          app-state
          keypaths/menus))

(defmethod transition-state events/control-menu-collapse-all
  [_ _ {:keys [menus]} app-state]
  (collapse-menus app-state menus))

(defmethod transition-state events/control-sign-out [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/user {})
      (assoc-in keypaths/order nil)
      (assoc-in keypaths/stylist state/initial-stylist-state)
      (assoc-in keypaths/checkout state/initial-checkout-state)
      (assoc-in keypaths/billing-address {})
      (assoc-in keypaths/shipping-address {})
      (assoc-in keypaths/facebook-email-denied nil)))

(defmethod transition-state events/control-change-state
  [_ event {:keys [keypath value]} app-state]
  (assoc-in app-state keypath (if (fn? value) (value) value)))

(defmethod transition-state events/control-counter-inc [_ event args app-state]
  (update-in app-state (:path args) inc))

(defmethod transition-state events/control-counter-dec [_ event args app-state]
  (update-in app-state (:path args) (comp (partial max 1) dec)))

(defmethod transition-state events/control-bundle-option-select
  [_ event {:keys [selected-options]} app-state]
  (update-in app-state
             keypaths/bundle-builder bundle-builder/reset-options selected-options))

(defmethod transition-state events/control-checkout-shipping-method-select [_ event shipping-method app-state]
  (assoc-in app-state keypaths/checkout-selected-shipping-method shipping-method))

(defmethod transition-state events/control-checkout-as-guest-submit [_ event args app-state]
  (assoc-in app-state keypaths/checkout-as-guest true))

(defmethod transition-state events/control-checkout-cart-paypal-setup [_ event args app-state]
  (assoc-in app-state keypaths/cart-paypal-redirect true))

(defmethod transition-state events/api-start
  [_ event request app-state]
  (update-in app-state keypaths/api-requests conj request))

(defmethod transition-state events/api-end
  [_ event {:keys [request-id app-version] :as request} app-state]
  (-> app-state
      (update-in keypaths/app-version #(or % app-version))
      (update-in keypaths/api-requests (partial remove (comp #{request-id} :request-id)))))

(defmethod transition-state events/api-success-products [_ event {:keys [products]} app-state]
  (-> app-state
    (update-in keypaths/products merge (key-by :id products))
    ensure-bundle-builder))

(defmethod transition-state events/api-success-states [_ event {:keys [states]} app-state]
  (assoc-in app-state keypaths/states states))

(defmethod transition-state events/api-success-stylist-account
  [_ event {:keys [stylist]} app-state]
  (-> app-state
      (update-in keypaths/stylist-manage-account merge stylist)
      (update-in keypaths/store merge (select-keys stylist [:instagram_account :styleseat_account :profile_picture_url]))))

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


(defmethod transition-state events/api-partial-success-send-stylist-referrals
  [_ event {:keys [results] :as x} app-state]
  (update-in app-state keypaths/stylist-referrals
             (fn [old-referrals]
               (->> (map (fn [n o] [n o]) results old-referrals)
                    (filter (fn [[nr or]]
                              (seq (:error nr))))
                    (map last)
                    vec))))

(defmethod transition-state events/api-success-send-stylist-referrals
  [_ event {:keys [results] :as x} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-referrals [state/empty-referral])
      (assoc-in keypaths/errors {})
      (assoc-in keypaths/popup :refer-stylist-thanks)))

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

(defmethod transition-state events/api-success-add-to-bag [_ event {:keys [order requested]} app-state]
  (-> app-state
      (update-in keypaths/browse-recently-added-variants conj requested)
      (assoc-in keypaths/browse-variant-quantity 1)
      (update-in keypaths/order merge order)
      (update-in keypaths/bundle-builder bundle-builder/rollback)))

(defmethod transition-state events/api-success-remove-from-bag [_ event {:keys [order]} app-state]
  (-> app-state
      (update-in keypaths/order merge order)))

(defmethod transition-state events/api-success-shared-cart [_ event {:keys [cart]} app-state]
  (-> app-state
      (assoc-in keypaths/shared-cart-url (str (.-protocol js/location) "//" (.-host js/location) "/c/" (:number cart)))
      (assoc-in keypaths/popup :share-cart)))

(defmethod transition-state events/control-stylist-referral-add-another [_ event args app-state]
  (update-in app-state keypaths/stylist-referrals conj state/empty-referral))

(defmethod transition-state events/control-stylist-referral-remove [_ event {:keys [index]} app-state]
  (update-in app-state
             keypaths/stylist-referrals
             #(vec (remove nil? (assoc % index nil)))))

(defmethod transition-state events/control-stylist-referral-submit [_ event args app-state]
  (clear-flash app-state))

(defmethod transition-state events/control-stylist-banner-close [_ event args app-state]
  (assoc-in app-state keypaths/stylist-banner-hidden true))

(defmethod transition-state events/control-popup-show-refer-stylists [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/popup :refer-stylist)
      clear-flash))

(defmethod transition-state events/control-account-profile-submit [_ event args app-state]
  (let [password              (get-in app-state keypaths/manage-account-password)
        password-confirmation (get-in app-state keypaths/manage-account-password-confirmation)]
    (cond-> (assoc-in app-state keypaths/errors nil)
      (not= password password-confirmation)
      (assoc-in keypaths/errors (group-by :path [{:path [:user :password-confirmation] :long-message "Passwords must match!"}]))
      (> 6 (count password))
      (assoc-in keypaths/errors (group-by :path [{:path [:user :password] :long-message "New password must be at least 6 characters"}])))))

(defmethod transition-state events/control-stylist-account-password-submit [_ event args app-state]
  (let [stylist-account       (get-in app-state keypaths/stylist-manage-account)
        password              (-> stylist-account :user :password)
        password-confirmation (-> stylist-account :user :password-confirmation)]
    (cond-> (assoc-in app-state keypaths/errors nil)
      (not= password password-confirmation)
      (assoc-in keypaths/errors (group-by :path [{:path [:stylist :user :password-confirmation] :long-message "Passwords must match!"}]))
      (> 6 (count password))
      (assoc-in keypaths/errors (group-by :path [{:path [:stylist :user :password] :long-message "New password must be at least 6 characters"}])))))

(defmethod transition-state events/control-popup-hide [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod transition-state events/api-success-get-order [_ event order app-state]
  (if (orders/incomplete? order)
    (-> app-state
        (update-in keypaths/checkout-billing-address merge (:billing-address order))
        (update-in keypaths/checkout-shipping-address merge (:shipping-address order))
        (assoc-in keypaths/order order)
        (assoc-in keypaths/checkout-selected-shipping-method
                  (merge (first (get-in app-state keypaths/shipping-methods))
                         (orders/shipping-item order))))
    (assoc-in app-state keypaths/order {})))

(defmethod transition-state events/api-success-messenger-token [_ event {:keys [messenger_token]} app-state]
  (assoc-in app-state keypaths/user-messenger-token messenger_token))

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
  (-> app-state
      (update-in address-keypath merge address)
      (assoc-in keypaths/places-enabled false)))

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
      (assoc-in keypaths/cart state/initial-cart-state)
      (assoc-in keypaths/order {})
      (assoc-in keypaths/pending-talkable-order (talkable/completed-order order))))

(defmethod transition-state events/api-success-promotions [_ event {promotions :promotions} app-state]
  (assoc-in app-state keypaths/promotions promotions))

(defmethod transition-state events/api-success-cache [_ event new-data app-state]
  (update-in app-state keypaths/api-cache merge new-data))

(defmethod transition-state events/api-failure-errors [_ event errors app-state]
  (assoc-in app-state keypaths/errors (group-by :path errors)))

(defmethod transition-state events/api-failure-validation-errors [_ event validation-errors app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/validation-errors validation-errors)))

(defmethod transition-state events/api-failure-pending-promo-code [_ event args app-state]
  (assoc-in app-state keypaths/pending-promo-code nil))

(defmethod transition-state events/api-handle-order-not-found [_ _ _ app-state]
  (assoc-in app-state keypaths/order nil))

(defmethod transition-state events/flash-show-success [_ event args app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/flash-success (select-keys args [:message :navigation]))))

(defmethod transition-state events/flash-dismiss [_ event args app-state]
  (clear-flash app-state))

(defmethod transition-state events/flash-show-failure [_ event args app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/flash-failure (select-keys args [:message :navigation]))))

(defn set-color-option-variation [app-state variation]
  (-> app-state
      (assoc-in keypaths/bundle-builder nil)
      ensure-bundle-builder))

(defmethod transition-state events/optimizely
  [_ event {:keys [variation feature]} app-state]
  (-> app-state
      (update-in keypaths/features conj (or feature variation))
      (set-color-option-variation variation)))

(defmethod transition-state events/inserted-optimizely [_ event args app-state]
  (assoc-in app-state keypaths/loaded-optimizely true))

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
