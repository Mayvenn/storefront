(ns storefront.frontend-transitions
  (:require [cemerick.url :as url]
            [clojure.string :as string]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.hooks.talkable :as talkable]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.state :as state]
            [storefront.transitions :refer [transition-state]]
            [spice.maps :as maps]
            [spice.core :as spice]
            [storefront.accessors.experiments :as experiments]))

(defn clear-fields [app-state & fields]
  (reduce #(assoc-in %1 %2 "") app-state fields))

(defn clear-nav-traversal
  [app-state]
  (assoc-in app-state
            keypaths/current-traverse-nav
            nil))

(defn collapse-menus
  ([app-state] (collapse-menus app-state nil))
  ([app-state menus]
   (reduce (fn [state menu] (assoc-in state menu false))
           app-state
           (or menus keypaths/menus))))

(defn clear-field-errors [app-state]
  (assoc-in app-state keypaths/errors {}))

(defn clear-flash [app-state]
  (-> app-state
      clear-field-errors
      (assoc-in keypaths/flash-now-success nil)
      (assoc-in keypaths/flash-now-failure nil)))

(defn add-return-event [app-state]
  (let [[return-event return-args] (get-in app-state keypaths/navigation-message)]
    (if (nav/return-blacklisted? return-event)
      app-state
      (assoc-in app-state keypaths/return-navigation-message [return-event return-args]))))

(defn add-pending-promo-code [app-state args]
  (let [{{sha :sha} :query-params} args]
    (if sha
      (assoc-in app-state keypaths/pending-promo-code sha)
      app-state)))

(defn default-credit-card-name [app-state {:keys [first-name last-name]}]
  (if-not (string/blank? (get-in app-state keypaths/checkout-credit-card-name))
    app-state
    (let [default (->> [first-name last-name]
                       (remove string/blank?)
                       (string/join " "))]
      (if (string/blank? default)
        app-state
        (assoc-in app-state keypaths/checkout-credit-card-name default)))))

(defmethod transition-state events/stash-nav-stack-item [_ _ stack-item app-state]
  (assoc-in app-state keypaths/navigation-stashed-stack-item stack-item))

(def max-nav-stack-depth 5)

(defn push-nav-stack [app-state stack-keypath stack-item]
  (let [leaving-nav (get-in app-state keypaths/navigation-message)
        item        (merge {:navigation-message leaving-nav} stack-item)
        nav-stack   (get-in app-state stack-keypath nil)]
    (->> (conj nav-stack item) (take max-nav-stack-depth))))

(defmethod transition-state events/navigation-save [_ _ stack-item app-state]
  ;; Going to a new page; add an element to the undo stack, and discard the redo stack
  (let [nav-undo-stack (push-nav-stack app-state keypaths/navigation-undo-stack stack-item)]
    (-> app-state
        (assoc-in keypaths/navigation-undo-stack nav-undo-stack)
        (assoc-in keypaths/navigation-redo-stack nil))))

(defmethod transition-state events/navigation-undo [_ _ stack-item app-state]
  ;; Going to prior page; pop an element from the undo stack, push one onto the redo stack
  (let [nav-redo-stack (push-nav-stack app-state keypaths/navigation-redo-stack stack-item)]
    (-> app-state
        (update-in keypaths/navigation-undo-stack rest)
        (assoc-in keypaths/navigation-redo-stack nav-redo-stack))))

(defmethod transition-state events/navigation-redo [_ _ stack-item app-state]
  ;; Going to next page; pop an element from the redo stack, push one onto the undo stack
  (let [nav-undo-stack (push-nav-stack app-state keypaths/navigation-undo-stack stack-item)]
    (-> app-state
        (assoc-in keypaths/navigation-undo-stack nav-undo-stack)
        (update-in keypaths/navigation-redo-stack rest))))

(defmethod transition-state events/redirect [_ event {:keys [nav-message]} app-state]
  (assoc-in app-state keypaths/redirecting? true))

(defn clear-completed-order [app-state]
  (cond-> app-state
    (nav/auth-events (get-in app-state keypaths/navigation-event))
    (assoc-in keypaths/completed-order nil)))

(defn prefill-guest-email-address [app-state]
  (update-in app-state keypaths/checkout-guest-email
             #(or (not-empty %1) %2 %3)
             (get-in app-state keypaths/order-user-email)
             (get-in app-state keypaths/captured-email)))

(defn update-state-from-order [app-state]
  (let [order (get-in app-state keypaths/order)]
    (if (orders/incomplete? order)
      (-> app-state
          (update-in keypaths/checkout-billing-address merge (:billing-address order))
          (update-in keypaths/checkout-shipping-address merge (:shipping-address order))
          (assoc-in keypaths/order order)
          (assoc-in keypaths/checkout-selected-shipping-method
                    (merge (first (get-in app-state keypaths/shipping-methods))
                           (orders/shipping-item order)))
          prefill-guest-email-address)
      (assoc-in app-state keypaths/order nil))))

(defmethod transition-state events/navigate [_ event args app-state]
  (let [args (dissoc args :nav-stack-item)]
    (-> app-state
        collapse-menus
        add-return-event
        (add-pending-promo-code args)
        clear-flash
        clear-completed-order
        (assoc-in keypaths/flash-now-success (get-in app-state keypaths/flash-later-success))
        (assoc-in keypaths/flash-now-failure (get-in app-state keypaths/flash-later-failure))
        (assoc-in keypaths/flash-later-success nil)
        (assoc-in keypaths/flash-later-failure nil)
        (update-in keypaths/ui dissoc :navigation-stashed-stack-item)
        ;; order is important from here on
        update-state-from-order
        (assoc-in keypaths/redirecting? false)
        (assoc-in keypaths/navigation-message [event args]))))

(def ^:private hostname (comp :host url/url))

(defn assoc-valid-telligent-url [app-state telligent-url]
  (if telligent-url
    (when (= (hostname telligent-url) (hostname config/telligent-community-url))
      (assoc-in app-state keypaths/telligent-community-url telligent-url))
    (assoc-in app-state keypaths/telligent-community-url nil)))

(defn assoc-valid-path [app-state path]
  (let [[event msg] (routes/navigation-message-for path)]
    (if (not= event events/navigate-not-found)
      (assoc-in app-state keypaths/return-navigation-message [event msg])
      app-state)))

(defmethod transition-state events/navigate-cart
  [_ event _ app-state]
  (assoc-in app-state keypaths/cart-paypal-redirect false))

(defmethod transition-state events/navigate-sign-in
  [_ event {{:keys [telligent-url path]} :query-params} app-state]
  (-> app-state
      (assoc-valid-telligent-url telligent-url)
      (assoc-valid-path path)))

(defmethod transition-state events/navigate-sign-out [_ event {{:keys [telligent-url]} :query-params} app-state]
  (assoc-valid-telligent-url app-state telligent-url))

(defmethod transition-state events/navigate-ugc-named-search [_ event {:keys [named-search-slug query-params]} app-state]
  (-> app-state
      (assoc-in (conj keypaths/browse-named-search-query :slug) named-search-slug)
      (assoc-in keypaths/ui-ugc-category-popup-offset (js/parseInt (:offset query-params 0) 10))))

(defmethod transition-state events/navigate-reset-password [_ event {:keys [reset-token]} app-state]
  (assoc-in app-state keypaths/reset-password-token reset-token))

(defmethod transition-state events/navigate-account-manage [_ event args app-state]
  (assoc-in app-state
            keypaths/manage-account-email
            (get-in app-state keypaths/user-email)))

(defmethod transition-state events/control-commission-order-expand [_ _ {:keys [number]} app-state]
  (assoc-in app-state keypaths/expanded-commission-order-id #{number}))

(defmethod transition-state events/navigate-shared-cart [_ event {:keys [shared-cart-id]} app-state]
  (assoc-in app-state keypaths/shared-cart-id shared-cart-id))

(defn ensure-direct-load-of-checkout-auth-advances-to-checkout-flow [app-state]
  (let [direct-load? (= [events/navigate-home {}]
                        (get-in app-state keypaths/return-navigation-message))]
    (when direct-load?
      (assoc-in app-state keypaths/return-navigation-message
                [events/navigate-checkout-address {}]))))

(defmethod transition-state events/navigate-checkout-returning-or-guest [_ event args app-state]
  (ensure-direct-load-of-checkout-auth-advances-to-checkout-flow app-state))

(defmethod transition-state events/navigate-checkout-sign-in [_ event args app-state]
  (ensure-direct-load-of-checkout-auth-advances-to-checkout-flow app-state))

(defmethod transition-state events/navigate-checkout-payment [_ event args app-state]
  (cond-> (default-credit-card-name app-state (get-in app-state (conj keypaths/order :billing-address)))
    (and (experiments/affirm? app-state)
         (orders/fully-covered-by-store-credit?
          (get-in app-state keypaths/order)
          (get-in app-state keypaths/user)))
    (assoc-in (conj keypaths/checkout-selected-payment-methods :store-credit) {})))

(defmethod transition-state events/pixlee-api-success-fetch-album [_ event {:keys [album-data album-name]} app-state]
  (let [images (pixlee/parse-ugc-album album-data)]
    (-> app-state
        (assoc-in (conj keypaths/ugc-albums album-name) (map :id images))
        (update-in keypaths/ugc-images merge (pixlee/images-by-id images)))))

(defmethod transition-state events/pixlee-api-success-fetch-image [_ event {:keys [image-data]} app-state]
  (let [image (pixlee/parse-ugc-image image-data)]
    (assoc-in app-state (conj keypaths/ugc-images (:id image)) image)))

(defmethod transition-state events/navigate-shop-by-look [_ event _ app-state]
  (assoc-in app-state keypaths/selected-look-id nil))

(defmethod transition-state events/navigate-shop-by-look-details [_ event {:keys [look-id] :as args} app-state]
  (let [shared-cart-id (:shared-cart-id (pixlee/selected-look app-state))
        current-shared-cart (get-in app-state keypaths/shared-cart-current)]
    (cond-> app-state
      true
      (assoc-in keypaths/selected-look-id (js/parseInt look-id))

      (not= shared-cart-id (:number current-shared-cart))
      (assoc-in keypaths/shared-cart-current nil))))

(defn ensure-cart-has-shipping-method [app-state]
  (-> app-state
      (assoc-in keypaths/checkout-selected-shipping-method
                (merge (first (get-in app-state keypaths/shipping-methods))
                       (orders/shipping-item (:order app-state))))))

(defmethod transition-state events/navigate-checkout-address [_ event args app-state]
  (prefill-guest-email-address app-state))

(defmethod transition-state events/navigate-checkout-confirmation [_ event args app-state]
  (-> app-state
      ensure-cart-has-shipping-method
      (update-in keypaths/checkout-credit-card-existing-cards empty)))

(defmethod transition-state events/navigate-order-complete [_ event args app-state]
  (when-not (get-in app-state keypaths/user-id)
    (add-return-event app-state)))

(defmethod transition-state events/navigate-gallery [_ event args app-state]
  (assoc-in app-state keypaths/editing-gallery? false))

;; TODO: GROT when affirm? experiment succeeds
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
  (-> app-state
      clear-nav-traversal
      (collapse-menus menus)))

(defmethod transition-state events/control-change-state
  [_ event {:keys [keypath value]} app-state]
  (assoc-in app-state keypath (if (fn? value) (value) value)))

(defmethod transition-state events/control-focus
  [_ event {:keys [keypath]} app-state]
  (assoc-in app-state keypaths/ui-focus keypath))

(defmethod transition-state events/control-blur
  [_ event {:keys [keypath]} app-state]
  (assoc-in app-state keypaths/ui-focus nil))

(defmethod transition-state events/control-counter-inc [_ event args app-state]
  (update-in app-state (:path args) inc))

(defmethod transition-state events/control-counter-dec [_ event args app-state]
  (update-in app-state (:path args) (comp (partial max 1) dec)))

(defmethod transition-state events/control-checkout-shipping-method-select
  [_ event shipping-method app-state]
  (assoc-in app-state keypaths/checkout-selected-shipping-method shipping-method))

(defmethod transition-state events/control-checkout-update-addresses-submit [_ event {:keys [become-guest?]} app-state]
  (when become-guest?
    (assoc-in app-state keypaths/checkout-as-guest true)))

(defmethod transition-state events/control-checkout-cart-paypal-setup [_ event args app-state]
  (assoc-in app-state keypaths/cart-paypal-redirect true))

(defmethod transition-state events/control-create-order-from-shared-cart [_ event {:keys [selected-look-id]} app-state]
  (assoc-in app-state keypaths/selected-look-id selected-look-id))

(defmethod transition-state events/control-essence-offer-details [_ event args app-state]
  (assoc-in app-state keypaths/popup :essence))

(defmethod transition-state events/api-start
  [_ event request app-state]
  (update-in app-state keypaths/api-requests conj request))

(defmethod transition-state events/api-end
  [_ event {:keys [request-id app-version] :as request} app-state]
  (-> app-state
      (update-in keypaths/app-version #(or % app-version))
      (update-in keypaths/api-requests (partial remove (comp #{request-id} :request-id)))))

(defmethod transition-state events/apple-pay-availability
  [_ event {:keys [available?]} app-state]
  (assoc-in app-state keypaths/show-apple-pay? available?))

(defmethod transition-state events/apple-pay-begin [_ _ _ app-state]
  (assoc-in app-state keypaths/disable-apple-pay-button? true))

(defmethod transition-state events/apple-pay-end [_ _ _ app-state]
  (assoc-in app-state keypaths/disable-apple-pay-button? false) )

(defmethod transition-state events/api-success-get-saved-cards [_ event {:keys [cards default-card]} app-state]
  (let [valid-id? (set (conj (map :id cards) "add-new-card"))]
    (cond-> app-state
      :start
      (assoc-in keypaths/checkout-credit-card-existing-cards cards)

      (not (valid-id? (get-in app-state keypaths/checkout-credit-card-selected-id)))
      (assoc-in keypaths/checkout-credit-card-selected-id nil)

      :finally
      (update-in keypaths/checkout-credit-card-selected-id #(or % (:id default-card))))))

(defmethod transition-state events/api-success-products [_ event {:keys [products]} app-state]
  (update-in app-state keypaths/products merge (maps/index-by :id products)))

(defmethod transition-state events/api-success-facets
  [_ event {:keys [facets]} app-state]
  (assoc-in app-state keypaths/facets (map #(update % :facet/slug keyword) facets)))

(defmethod transition-state events/api-success-states [_ event {:keys [states]} app-state]
  (assoc-in app-state keypaths/states states))

(defmethod transition-state events/api-success-stylist-account
  [_ event {:keys [stylist]} app-state]
  (-> app-state
      (update-in keypaths/stylist-manage-account merge stylist)
      (update-in keypaths/store merge (select-keys stylist [:instagram_account :styleseat_account :portrait]))))

(defmethod transition-state events/api-success-stylist-account-commission [_ event {:keys [stylist]} app-state]
  (let [green-dot-payout-attributes (some-> stylist :green_dot_payout_attributes (select-keys [:last4 :payout_timeframe]))]
    (cond-> (update-in app-state keypaths/stylist-manage-account dissoc :green_dot_payout_attributes)
      (= "green_dot" (:chosen_payout_method stylist))
      (-> (assoc-in keypaths/stylist-manage-account-green-dot-card-selected-id (:last4 green-dot-payout-attributes))
          (assoc-in (conj keypaths/stylist-manage-account :green_dot_payout_attributes) green-dot-payout-attributes)))))

(defmethod transition-state events/api-success-gallery [_ event {:keys [images]} app-state]
  (-> app-state
      (assoc-in keypaths/store-gallery-images images)))

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
      clear-field-errors
      (assoc-in keypaths/stylist-referrals [state/empty-referral])
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

(defmethod transition-state events/api-success-auth [_ event {:keys [user order]} app-state]
  (let [signed-in-app-state (-> app-state
                                (sign-in-user user)
                                (clear-fields keypaths/sign-up-email
                                              keypaths/sign-up-password
                                              keypaths/sign-in-email
                                              keypaths/sign-in-password
                                              keypaths/reset-password-password
                                              keypaths/reset-password-token)
                                (assoc-in keypaths/order order))
        opted-in?           (= "opted-in" (get-in signed-in-app-state keypaths/email-capture-session))]
    (cond-> signed-in-app-state
      (not opted-in?)
      (assoc-in keypaths/email-capture-session "signed-in"))))

(defmethod transition-state events/api-success-forgot-password [_ event args app-state]
  (clear-fields app-state keypaths/forgot-password-email))

(defmethod transition-state events/api-success-add-to-bag [_ event {:keys [order quantity variant]} app-state]
  (-> app-state
      (update-in keypaths/browse-recently-added-variants conj {:quantity quantity :variant variant})
      (assoc-in keypaths/browse-variant-quantity 1)
      (update-in keypaths/order merge order)))

(defmethod transition-state events/api-success-remove-from-bag [_ event {:keys [order]} app-state]
  (-> app-state
      (update-in keypaths/order merge order)))

(defmethod transition-state events/api-success-shared-cart-create [_ event {:keys [cart]} app-state]
  (-> app-state
      (assoc-in keypaths/shared-cart-url (str (.-protocol js/location) "//" (.-host js/location) "/c/" (:number cart)))
      (assoc-in keypaths/popup :share-cart)))

(defmethod transition-state events/api-success-shared-cart-fetch [_ event {:keys [cart]} app-state]
  (assoc-in app-state keypaths/shared-cart-current cart))

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
        field-errors          (cond-> {}
                                (> 6 (count password))
                                (merge (group-by :path [{:path ["password"] :long-message "New password must be at least 6 characters"}])))]
    (if (and (seq password) (seq field-errors))
      (assoc-in app-state keypaths/errors {:field-errors field-errors :error-code "invalid-input" :error-message "Oops! Please fix the errors below."})
      (clear-field-errors app-state))))

(defmethod transition-state events/control-stylist-account-commission-submit [_ event args app-state]
  (let [selected-id (get-in app-state keypaths/stylist-manage-account-green-dot-card-selected-id)
        last4       (get-in app-state (conj keypaths/stylist-manage-account :green_dot_payout_attributes :last4))]
    (cond-> app-state
      (and (seq last4) (= selected-id last4))
      (assoc-in (conj keypaths/stylist-manage-account :green_dot_payout_attributes) {:last4 last4}))))

(defmethod transition-state events/control-stylist-account-password-submit [_ event args app-state]
  (let [stylist-account       (get-in app-state keypaths/stylist-manage-account)
        password              (-> stylist-account :user :password)
        field-errors          (cond-> {}
                                (> 6 (count password))
                                (merge (group-by :path [{:path ["user" "password"] :long-message "New password must be at least 6 characters"}])))]
    (if (seq field-errors)
      (assoc-in app-state keypaths/errors {:field-errors field-errors :error-code "invalid-input" :error-message "Oops! Please fix the errors below."})
      (clear-field-errors app-state))))


(defmethod transition-state events/control-cancel-editing-gallery [_ event args app-state]
  (assoc-in app-state keypaths/editing-gallery? false))

(defmethod transition-state events/control-edit-gallery [_ event args app-state]
  (assoc-in app-state keypaths/editing-gallery? true))

(defmethod transition-state events/control-popup-hide [_ event args app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/popup nil)))



(defmethod transition-state events/api-success-manage-account [_ event args app-state]
  (-> app-state
      (sign-in-user args)
      (clear-fields keypaths/manage-account-email
                    keypaths/manage-account-password)))

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
  (cond-> app-state
    (vals-empty? (get-in app-state keypaths/checkout-billing-address))
    (assoc-in keypaths/checkout-billing-address billing-address)

    (vals-empty? (get-in app-state keypaths/checkout-shipping-address))
    (assoc-in keypaths/checkout-shipping-address shipping-address)))

(defmethod transition-state events/autocomplete-update-address [_ event {:keys [address address-keypath] :as args} app-state]
  (update-in app-state address-keypath merge address))

(defmethod transition-state events/api-success-account [_ event {:keys [billing-address shipping-address] :as args} app-state]
  (-> app-state
      (sign-in-user args)
      (update-account-address args)
      (default-checkout-addresses billing-address shipping-address)))

(defmethod transition-state events/api-success-update-order-add-promotion-code [_ event args app-state]
  (-> app-state
      clear-field-errors
      (assoc-in keypaths/cart-coupon-code "")
      (assoc-in keypaths/pending-promo-code nil)))

(defmethod transition-state events/api-success-update-order-remove-promotion-code [_ event args app-state]
  (-> app-state
      clear-field-errors
      (assoc-in keypaths/cart-coupon-code "")))

(defmethod transition-state events/api-success-sms-number [_ event args app-state]
  (assoc-in app-state keypaths/sms-number (:number args)))

(defmethod transition-state events/api-success-update-order [_ event {:keys [order]} app-state]
  (assoc-in app-state keypaths/order order))

(defmethod transition-state events/order-completed [_ event order app-state]
  (-> app-state
      (assoc-in keypaths/sign-up-email (get-in app-state keypaths/checkout-guest-email))
      (assoc-in keypaths/checkout state/initial-checkout-state)
      (assoc-in keypaths/cart state/initial-cart-state)
      (assoc-in keypaths/order nil)
      (assoc-in keypaths/completed-order order)
      (assoc-in keypaths/pending-talkable-order (talkable/completed-order order))))

(defmethod transition-state events/api-success-promotions [_ event {promotions :promotions} app-state]
  (assoc-in app-state keypaths/promotions promotions))

(defmethod transition-state events/api-success-get-static-content
  [_ event args app-state]
  (assoc-in app-state keypaths/static args))

(defmethod transition-state events/api-success-cache [_ event new-data app-state]
  (update-in app-state keypaths/api-cache merge new-data))

(defmethod transition-state events/api-failure-errors [_ event errors app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/errors (update errors :field-errors (partial group-by :path)))))

(defmethod transition-state events/api-failure-order-not-created-from-shared-cart [_ event args app-state]
  (when-let [error-message (get-in app-state keypaths/error-message)]
    (assoc-in app-state keypaths/flash-later-failure {:message error-message})))

(defmethod transition-state events/api-failure-pending-promo-code [_ event args app-state]
  (assoc-in app-state keypaths/pending-promo-code nil))

(defmethod transition-state events/flash-show-success [_ _ {:keys [message]} app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/flash-now-success {:message message})))

(defmethod transition-state events/flash-show-failure [_ _ {:keys [message]} app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/flash-now-failure {:message message})))

(defmethod transition-state events/flash-later-show-success [_ _ {:keys [message]} app-state]
  (assoc-in app-state keypaths/flash-later-success {:message message}))

(defmethod transition-state events/flash-later-show-failure [_ _ {:keys [message]} app-state]
  (assoc-in app-state keypaths/flash-later-failure {:message message}))

(defmethod transition-state events/flash-dismiss [_ event args app-state]
  (clear-flash app-state))

(defmethod transition-state events/bucketed-for [_ event {:keys [experiment]} app-state]
  (update-in app-state keypaths/experiments-bucketed conj experiment))

(defmethod transition-state events/enable-feature [_ event {:keys [feature]} app-state]
  (update-in app-state keypaths/features conj feature))

(defmethod transition-state events/inserted-convert [_ event args app-state]
  (assoc-in app-state keypaths/loaded-convert true))

(defmethod transition-state events/inserted-places [_ event args app-state]
  (assoc-in app-state keypaths/loaded-places true))

(defmethod transition-state events/inserted-reviews [_ event args app-state]
  (assoc-in app-state keypaths/loaded-reviews true))

(defmethod transition-state events/inserted-stripe [_ event {:keys [version]} app-state]
  (assoc-in app-state (conj keypaths/loaded-stripe version) true))

(defmethod transition-state events/inserted-uploadcare [_ _ _ app-state]
  (assoc-in app-state keypaths/loaded-uploadcare true))

(defmethod transition-state events/inserted-facebook [_ event args app-state]
  (assoc-in app-state keypaths/loaded-facebook true))

(defmethod transition-state events/inserted-talkable [_ event args app-state]
  (assoc-in app-state keypaths/loaded-talkable true))

(defmethod transition-state events/reviews-component-mounted [_ event args app-state]
  (update-in app-state keypaths/review-components-count inc))

(defmethod transition-state events/reviews-component-will-unmount [_ event args app-state]
  (update-in app-state keypaths/review-components-count dec))

(defmethod transition-state events/stripe-component-mounted [_ event {:keys [card-element]} app-state]
  (assoc-in app-state keypaths/stripe-card-element card-element))

(defmethod transition-state events/stripe-component-will-unmount [_ event {:keys [card-element]} app-state]
  (update-in app-state keypaths/stripe-card-element dissoc))

(defmethod transition-state events/facebook-success-sign-in [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied nil))

(defmethod transition-state events/facebook-failure-sign-in [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied nil))

(defmethod transition-state events/facebook-email-denied [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied true))

(defmethod transition-state events/talkable-offer-shown [_ event args app-state]
  (assoc-in app-state keypaths/pending-talkable-order nil))

(defmethod transition-state events/control-email-captured-dismiss [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/popup nil)
      (assoc-in keypaths/email-capture-session "dismissed")))

(defmethod transition-state events/control-email-captured-submit [_ event args app-state]
  (let [email (get-in app-state keypaths/captured-email)]
    (if (or (> 3 (count email)) (not (string/includes? email "@")))
      (assoc-in app-state keypaths/errors {:field-errors  {["email"] [{:path ["email"] :long-message "Email is invalid"}]}
                                           :error-code    "invalid-input"
                                           :error-message "Oops! Please fix the errors below."})
      (-> app-state
          clear-field-errors
          (assoc-in keypaths/popup nil)
          (assoc-in keypaths/email-capture-session "opted-in")))))

(defmethod transition-state events/show-email-popup [_ event args app-state]
  (assoc-in app-state keypaths/popup :email-capture))

(defmethod transition-state events/sign-out [_ event args app-state]
  (let [signed-out-app-state (-> app-state
                                 (assoc-in keypaths/user {})
                                 (assoc-in keypaths/order nil)
                                 (assoc-in keypaths/completed-order nil)
                                 (assoc-in keypaths/stylist state/initial-stylist-state)
                                 (assoc-in keypaths/checkout state/initial-checkout-state)
                                 (assoc-in keypaths/billing-address {})
                                 (assoc-in keypaths/shipping-address {})
                                 (assoc-in keypaths/facebook-email-denied nil))
        opted-in?            (= "opted-in" (get-in signed-out-app-state keypaths/email-capture-session))]
    (cond-> signed-out-app-state
      (not opted-in?)
      (assoc-in keypaths/email-capture-session "dismissed"))))

(defmethod transition-state events/stripe-failure-create-token [_ event stripe-response app-state]
  (let [{:keys [code message param]} (:error stripe-response)]
    (assoc-in app-state keypaths/errors
              {:error-code    code
               :error-message message})))


