(ns storefront.frontend-transitions
  (:require [adventure.keypaths :as adventure.keypaths]
            [catalog.products :as products]
            catalog.categories
            [cemerick.url :as url]
            [clojure.string :as string]
            promotion-helper.keypaths
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.orders :as orders]
            catalog.keypaths
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.state :as state]
            [storefront.transitions
             :refer [transition-state
                     sign-in-user
                     clear-fields]
             :as transitions]))

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

(defmethod transition-state events/control-account-profile-submit [_ _event _args app-state]
  (let [password              (get-in app-state keypaths/manage-account-password)
        field-errors          (cond-> {}
                                (> 6 (count password))
                                (merge (group-by :path [{:path ["password"] :long-message "New password must be at least 6 characters"}])))]
    (if (and (seq password) (seq field-errors))
      (assoc-in app-state keypaths/errors {:field-errors field-errors :error-code "invalid-input" :error-message "Oops! Please fix the errors below."})
      (clear-field-errors app-state))))

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

(defn add-affiliate-stylist-id
  [app-state {{affiliate-stylist-id :affiliate_stylist_id} :query-params}]
  (if affiliate-stylist-id
    (assoc-in app-state adventure.keypaths/adventure-affiliate-stylist-id (spice/parse-int affiliate-stylist-id))
    app-state))

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

(def unrecordable-nav-events
  ;; Events that are not allowed to be navigated back (or forward) to
  #{events/navigate-added-to-cart})

(def max-nav-stack-depth 5)

(defn push-nav-stack [app-state stack-keypath stack-item]
  (let [leaving-nav (get-in app-state keypaths/navigation-message)
        item        (merge {:navigation-message leaving-nav} stack-item)
        nav-stack   (get-in app-state stack-keypath nil)]
    (if (contains? unrecordable-nav-events (first leaving-nav))
      nav-stack
      (take max-nav-stack-depth (conj nav-stack item)))))

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

(defmethod transition-state events/redirect [_ event _ app-state]
  (assoc-in app-state keypaths/redirecting? true))

(defn clear-completed-order [app-state]
  (cond-> app-state
    (nav/auth-events (get-in app-state keypaths/navigation-event))
    (assoc-in keypaths/completed-order nil)))

(defn prefill-guest-email-address [app-state]
  (update-in app-state keypaths/checkout-guest-email
             #(or (not-empty %1) %2)
             (get-in app-state keypaths/order-user-email)))

;; TODO ask product about this
(def ^:private adventure-slug->video
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}
   "free-install"   {:youtube-id "oR1keQ-31yc"}})

(defmethod transition-state events/navigate-home
  [_ event {:keys [query-params]} app-state]
  (assoc-in app-state adventure.keypaths/adventure-home-video
            (adventure-slug->video (:video query-params))))

(defmethod transition-state events/navigate-about-mayvenn-install
  [_ event {:keys [query-params]} app-state]
  (assoc-in app-state adventure.keypaths/adventure-home-video
            (adventure-slug->video (:video query-params))))

(defn clean-up-open-category-panels
  [app-state
   [current-nav-event current-nav-event-args]
   [_ previous-nav-event-args]]
  (cond-> app-state
    (or (not= current-nav-event events/navigate-category)
        (not= (:catalog/category-id current-nav-event-args)
              (:catalog/category-id previous-nav-event-args)))
    (-> (assoc-in keypaths/hide-header? false)
        (assoc-in catalog.keypaths/category-panel nil))))

(defn clear-recently-added-skus
  [app-state [previous-nav-event _]]
  (cond-> app-state
    (= previous-nav-event events/navigate-added-to-cart)
    (assoc-in keypaths/cart-recently-added-skus {})))

(defn clear-detailed-product-related-addons
  [app-state [previous-nav-event _]]
  (cond-> app-state
    (= previous-nav-event events/navigate-product-details)
    (assoc-in catalog.keypaths/detailed-product-related-addons nil)))

(defn update-flash [app-state caused-by]
  (cond-> app-state
    (not= caused-by :module-load)
    (->
     clear-flash
     (assoc-in keypaths/flash-now-success (get-in app-state keypaths/flash-later-success))
     (assoc-in keypaths/flash-now-failure (get-in app-state keypaths/flash-later-failure))
     (assoc-in keypaths/flash-later-success nil)
     (assoc-in keypaths/flash-later-failure nil))))

(defmethod transition-state events/navigate [_ event args app-state]
  (let [args                 (dissoc args :nav-stack-item)
        uri                  (url/url js/window.location)
        new-nav-message      [event args]
        previous-nav-message (get-in app-state keypaths/navigation-message)]
    (-> app-state
        collapse-menus
        add-return-event
        (assoc-in keypaths/footer-email-submitted nil)
        (clean-up-open-category-panels new-nav-message previous-nav-message)
        (clear-recently-added-skus previous-nav-message)
        (clear-detailed-product-related-addons previous-nav-message)
        (add-pending-promo-code args)
        (add-affiliate-stylist-id args)
        clear-completed-order
        (assoc-in keypaths/flyout-stuck-open? false)
        (update-flash (:navigate/caused-by args))
        (update-in keypaths/ui dissoc :navigation-stashed-stack-item)
        (assoc-in keypaths/navigation-uri uri)
        ;; order is important from here on
        (assoc-in keypaths/redirecting? false)
        (assoc-in keypaths/navigation-message new-nav-message))))

(def ^:private hostname (comp :host url/url))

(defmethod transition-state events/navigate-cart
  [_ event _ app-state]
  (-> app-state
      (assoc-in keypaths/cart-paypal-redirect false)
      (assoc-in keypaths/promo-code-entry-open? false)))

(defmethod transition-state events/navigate-reset-password [_ event {:keys [reset-token]} app-state]
  (assoc-in app-state keypaths/reset-password-token reset-token))

(defmethod transition-state events/navigate-account-manage [_ event args app-state]
  (assoc-in app-state
            keypaths/manage-account-email
            (get-in app-state keypaths/user-email)))

(defmethod transition-state events/control-commission-order-expand [_ _ {:keys [number]} app-state]
  (assoc-in app-state keypaths/expanded-commission-order-id #{number}))

(defn ensure-direct-load-of-checkout-auth-advances-to-checkout-flow [app-state]
  (let [direct-load? (= [events/navigate-home {}]
                        (get-in app-state keypaths/return-navigation-message))
        previously-upsell? (= [events/navigate-checkout-add nil]
                              (get-in app-state keypaths/return-navigation-message))]
    (cond-> app-state
      (or direct-load?
          previously-upsell?)
      (assoc-in keypaths/return-navigation-message [events/navigate-checkout-address {}]))))

(defmethod transition-state events/navigate-checkout-returning-or-guest [_ event args app-state]
  (let [phone-marketing-opt-in (get-in app-state keypaths/order-phone-marketing-opt-in)]
    (-> app-state
        ensure-direct-load-of-checkout-auth-advances-to-checkout-flow
        (assoc-in keypaths/checkout-phone-marketing-opt-in phone-marketing-opt-in))))

(defmethod transition-state events/navigate-checkout-sign-in [_ event args app-state]
  (ensure-direct-load-of-checkout-auth-advances-to-checkout-flow app-state))

;; When you navigate to the checkout payment and are fully covered by store
;; credit (and can use it) Choose store-credit as your payment method (changing
;; to a different payment method is guarded in the UI)
(defmethod transition-state events/navigate-checkout-payment [_ event args app-state]
  (let [order                    (get-in app-state keypaths/order)
        billing-address          (get-in app-state (conj keypaths/order :billing-address))
        user                     (get-in app-state keypaths/user)
        covered-by-store-credit-and-can-use-store-credit?
        (and (orders/fully-covered-by-store-credit? order user)
             (orders/can-use-store-credit? order user))]
    (assoc-in (default-credit-card-name app-state billing-address)
              keypaths/checkout-selected-payment-methods
              (cond
                covered-by-store-credit-and-can-use-store-credit?
                {:store-credit {}}

                (not covered-by-store-credit-and-can-use-store-credit?)
                (orders/form-payment-methods order user)

                :else
                {}))))

(defn ensure-cart-has-shipping-method [app-state]
  (-> app-state
      (assoc-in keypaths/checkout-selected-shipping-method
                (merge (first (get-in app-state keypaths/shipping-methods))
                       (orders/shipping-item (:order app-state))))))

(defmethod transition-state events/navigate-checkout-address [_ event args app-state]
  (let [phone-marketing-opt-in (get-in app-state keypaths/order-phone-marketing-opt-in)]
    (-> app-state
        prefill-guest-email-address
        (assoc-in keypaths/checkout-phone-marketing-opt-in phone-marketing-opt-in))))

(defmethod transition-state events/navigate-checkout-confirmation [_ event args app-state]
  (-> app-state
      ensure-cart-has-shipping-method
      (update-in keypaths/checkout-credit-card-existing-cards empty)))

(defmethod transition-state events/navigate-order-complete [_ event args app-state]
  (when-not (get-in app-state keypaths/user-id)
    (add-return-event app-state)))

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
  [_ event _ app-state]
  (assoc-in app-state keypaths/ui-focus nil))

(defmethod transition-state events/control-counter-inc [_ event args app-state]
  (update-in app-state (:path args) inc))

(defmethod transition-state events/control-counter-dec [_ event args app-state]
  (update-in app-state (:path args) (comp (partial max 1) dec)))

(defmethod transition-state events/control-checkout-shipping-method-select
  [_ event shipping-method app-state]
  (assoc-in app-state keypaths/checkout-selected-shipping-method shipping-method))

(defmethod transition-state events/control-checkout-update-addresses-submit [_ event {:keys [become-guest?]} app-state]
  (cond-> app-state
    become-guest? (assoc-in keypaths/checkout-as-guest true)))

(defmethod transition-state events/control-checkout-cart-paypal-setup [_ event args app-state]
  (assoc-in app-state keypaths/cart-paypal-redirect true))

(defmethod transition-state events/control-create-order-from-look
  [_ event {:keys [look-id]} app-state]
  (assoc-in app-state keypaths/selected-look-id look-id))

(defmethod transition-state events/api-start
  [_ event request app-state]
  (update-in app-state keypaths/api-requests conj request))

(defmethod transition-state events/api-end
  [_ event {:keys [request-id app-version] :as request} app-state]
  (-> app-state
      (update-in keypaths/app-version #(or % app-version))
      (update-in keypaths/api-requests (partial remove (comp #{request-id} :request-id)))))

(defmethod transitions/transition-state events/api-success-store-gallery-fetch [_ event {:keys [images]} app-state]
  (assoc-in app-state keypaths/store-gallery-images images))

(defmethod transition-state events/api-success-get-saved-cards [_ event {:keys [cards default-card]} app-state]
  (let [valid-id? (set (conj (map :id cards) "add-new-card"))]
    (cond-> app-state
      :start
      (assoc-in keypaths/checkout-credit-card-existing-cards cards)

      (not (valid-id? (get-in app-state keypaths/checkout-credit-card-selected-id)))
      (assoc-in keypaths/checkout-credit-card-selected-id nil)

      :finally
      (update-in keypaths/checkout-credit-card-selected-id #(or % (:id default-card))))))

(defmethod transition-state events/api-success-facets
  [_ event {:keys [facets]} app-state]
  (assoc-in app-state keypaths/v2-facets (map #(update % :facet/slug keyword) facets)))

(defmethod transition-state events/api-success-states [_ event {:keys [states]} app-state]
  (assoc-in app-state keypaths/states states))

(defn line-items-same? [prev-order order]
  (and (= (:number prev-order) (:number order))
       (= (->> prev-order orders/all-line-items (map (juxt :sku :quantity)))
          (->> order orders/all-line-items (map (juxt :sku :quantity))))))

(defmethod transition-state events/save-order
  [_ event {:keys [order]} app-state]
  (if (orders/incomplete? order)
    (let [previous-order (get-in app-state keypaths/order)]
      (cond-> (-> app-state
                  (assoc-in keypaths/order order)
                  (update-in keypaths/checkout-billing-address merge (:billing-address order))
                  (update-in keypaths/checkout-shipping-address merge (:shipping-address order))
                  (assoc-in keypaths/checkout-selected-shipping-method
                            (merge (first (get-in app-state keypaths/shipping-methods))
                                   (orders/shipping-item order)))
                  prefill-guest-email-address)
        (not (line-items-same? previous-order order))
        (assoc-in keypaths/cart-recently-added-skus (orders/recently-added-sku-ids->quantities previous-order order))))
    (assoc-in app-state keypaths/order nil)))

(defmethod transition-state events/clear-order [_ event _ app-state]
  (assoc-in app-state keypaths/order nil))

(defmethod transition-state events/api-success-auth [_ event {:keys [user order]} app-state]
  (-> app-state
      (sign-in-user user)
      (clear-fields keypaths/sign-up-email
                    keypaths/sign-up-password
                    keypaths/sign-in-email
                    keypaths/sign-in-password
                    keypaths/reset-password-password
                    keypaths/reset-password-token)))

(defmethod transition-state events/api-success-forgot-password [_ event args app-state]
  (clear-fields app-state keypaths/forgot-password-email))

(defmethod transition-state events/api-success-decrease-quantity [_ event args app-state]
  (assoc-in app-state keypaths/browse-variant-quantity 1))

(defmethod transition-state events/api-success-shared-cart-create [_ event {:keys [cart]} app-state]
  (let [domain (cond-> (.-host js/location)
                 (= "retail-location" (get-in app-state keypaths/store-experience))
                 (clojure.string/replace-first #"^.*?\." "shop."))]
    (-> app-state
        (assoc-in keypaths/shared-cart-url (str (.-protocol js/location) "//" domain "/c/" (:number cart)))
        (assoc-in keypaths/popup :share-cart))))

(defn derive-all-looks
  [cms-data]
  (assoc-in cms-data [:ugc-collection :all-looks]
            (->> (:ugc-collection cms-data)
                 vals
                 (mapcat :looks)
                 (maps/index-by (comp keyword :content/id)))))

(defmethod transition-state events/api-success-fetch-cms-keypath
  [_ event more-cms-data app-state]
  (let [existing-cms-data (get-in app-state keypaths/cms)
        combined-cms-data (maps/deep-merge existing-cms-data more-cms-data)]
    (assoc-in app-state
              keypaths/cms
              (assoc-in combined-cms-data
                        [:ugc-collection :all-looks]
                        (maps/index-by (comp keyword :content/id)
                                       (mapcat :looks
                                               (vals (:ugc-collection combined-cms-data))))))))

(defmethod transition-state events/control-cancel-editing-gallery [_ event args app-state]
  (assoc-in app-state keypaths/editing-gallery? false))

(defmethod transition-state events/control-edit-gallery [_ event args app-state]
  (assoc-in app-state keypaths/editing-gallery? true))

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

(defn update-account-address [app-state {:keys [billing-address shipping-address]}]
  (-> app-state
      (merge {:billing-address billing-address
              :shipping-address shipping-address})
      (default-credit-card-name billing-address)))

(def vals-empty? (comp (partial every? string/blank?) vals))

(defmethod transition-state events/autocomplete-update-address
  [_ _ {:keys [address address-keypath]} app-state]
  (update-in app-state address-keypath merge address))

(defmethod transition-state events/api-success-account [_ event {:keys [billing-address shipping-address] :as args} app-state]
  (-> app-state
      (sign-in-user args)
      (update-account-address args)
      (assoc-in keypaths/checkout-billing-address billing-address)
      (assoc-in keypaths/checkout-shipping-address shipping-address)))

(defmethod transition-state events/api-success-update-order-add-promotion-code [_ event args app-state]
  (-> app-state
      clear-field-errors
      (assoc-in keypaths/cart-coupon-code "")
      (assoc-in keypaths/pending-promo-code nil)))

(defmethod transition-state events/api-success-update-order-add-service-line-item [_ event args app-state]
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

(defmethod transition-state events/order-completed [_ event order app-state]
  (-> app-state
      (assoc-in keypaths/sign-up-email (get-in app-state keypaths/checkout-guest-email))
      (assoc-in keypaths/checkout state/initial-checkout-state)
      (assoc-in keypaths/cart state/initial-cart-state)
      (assoc-in keypaths/completed-order order)))

(defmethod transition-state events/api-success-promotions [_ event {promotions :promotions} app-state]
  (update-in app-state keypaths/promotions #(-> (concat % promotions) set vec)))

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
  (cond-> app-state
    (not (contains? (set (get-in app-state keypaths/features)) feature))
    (update-in keypaths/features conj feature)))

(defmethod transition-state events/clear-features [_ event _ app-state]
  (assoc-in app-state keypaths/features []))

(defmethod transition-state events/inserted-google-maps [_ event args app-state]
  (assoc-in app-state keypaths/loaded-google-maps true))

(defmethod transition-state events/inserted-quadpay [_ event _ app-state]
  (assoc-in app-state keypaths/loaded-quadpay true))

(defmethod transition-state events/inserted-stripe [_ event _ app-state]
  (assoc-in app-state keypaths/loaded-stripe true))

(defmethod transition-state events/inserted-uploadcare [_ _ _ app-state]
  (assoc-in app-state keypaths/loaded-uploadcare true))

(defmethod transition-state events/inserted-facebook [_ event args app-state]
  (assoc-in app-state keypaths/loaded-facebook true))

(defmethod transition-state events/reviews-component-mounted [_ event args app-state]
  (update-in app-state keypaths/review-components-count inc))

(defmethod transition-state events/reviews-component-will-unmount [_ event args app-state]
  (update-in app-state keypaths/review-components-count dec))

(defmethod transition-state events/stripe-component-mounted [_ event {:keys [card-element]} app-state]
  (assoc-in app-state keypaths/stripe-card-element card-element))

(defmethod transition-state events/stripe-component-will-unmount [_ event _ app-state]
  (update-in app-state keypaths/stripe-card-element dissoc))

(defmethod transition-state events/facebook-success-sign-in [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied nil))

(defmethod transition-state events/facebook-failure-sign-in [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied nil))

(defmethod transition-state events/facebook-email-denied [_ event args app-state]
  (assoc-in app-state keypaths/facebook-email-denied true))

(defmethod transition-state events/sign-out [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/v2-dashboard state/initial-dashboard-state)
      (assoc-in keypaths/user {})
      (assoc-in keypaths/completed-order nil)
      (assoc-in keypaths/stylist state/initial-stylist-state)
      (assoc-in keypaths/checkout state/initial-checkout-state)
      (assoc-in keypaths/billing-address {})
      (assoc-in keypaths/shipping-address {})
      (assoc-in keypaths/facebook-email-denied nil)))

(defmethod transition-state events/stripe-failure-create-token [_ event stripe-response app-state]
  (let [{:keys [code message]} (:error stripe-response)]
    (assoc-in app-state keypaths/errors
              {:error-code    code
               :error-message message})))

(defmethod transitions/transition-state events/faq-section-selected [_ _ {:keys [index]} app-state]
  (let [expanded-index (get-in app-state keypaths/faq-expanded-section)]
    (if (= index expanded-index)
      (assoc-in app-state keypaths/faq-expanded-section nil)
      (assoc-in app-state keypaths/faq-expanded-section index))))

(defmethod transition-state events/api-success-user-stylist-service-menu-fetch [_ event {:keys [menu]} app-state]
  (cond-> app-state
    menu (assoc-in keypaths/user-stylist-service-menu menu)))

(defmethod transition-state events/api-success-user-stylist-offered-services
  [_ event {:keys [menu]} app-state]
  (cond-> app-state
    (seq menu)
    (assoc-in keypaths/user-stylist-offered-services
              (maps/index-by (comp keyword :offered-service-slug) menu))))

(defmethod transition-state events/stringer-browser-identified
  [_ _ {:keys [id]} app-state]
  (assoc-in app-state keypaths/stringer-browser-id id))

(defmethod transition-state events/module-loaded [_ _ {:keys [module-name]} app-state]
  (when module-name
    (update app-state :modules (fnil conj #{}) module-name)))

(defmethod transition-state events/api-success-get-skus
  [_ event args app-state]
  (update-in app-state keypaths/v2-skus #(merge (-> args :skus products/index-skus) %)))
