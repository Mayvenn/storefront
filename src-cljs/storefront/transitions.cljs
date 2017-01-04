(ns storefront.transitions
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.hooks.talkable :as talkable]
            [storefront.state :as state]
            [storefront.routes :as routes]
            [storefront.utils.query :as query]
            [storefront.utils.maps :refer [map-values key-by]]
            [storefront.config :as config]
            [clojure.string :as string]
            [cemerick.url :as url]
            [clojure.set :as set]))

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
      (assoc-in keypaths/flash-now-success nil)
      (assoc-in keypaths/flash-now-failure nil)
      (assoc-in keypaths/errors {})))

(defmulti transition-state identity)

(defmethod transition-state :default [dispatch event args app-state]
  ;; (js/console.log "IGNORED transition" (clj->js event) (clj->js args)) ;; enable to see ignored transitions
  app-state)

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

(defmethod transition-state events/redirect [_ event {:keys [nav-message]} app-state]
  (assoc-in app-state keypaths/redirecting? true))

(defmethod transition-state events/navigate [_ event args app-state]
  (-> app-state
      collapse-menus
      add-return-event
      (add-pending-promo-code args)
      clear-flash
      (assoc-in keypaths/flash-now-success (get-in app-state keypaths/flash-later-success))
      (assoc-in keypaths/flash-now-failure (get-in app-state keypaths/flash-later-failure))
      (assoc-in keypaths/flash-later-success nil)
      (assoc-in keypaths/flash-later-failure nil)
      (assoc-in keypaths/redirecting? false)
      (assoc-in keypaths/navigation-message [event args])))

(def ^:private hostname (comp :host url/url))

(defn assoc-valid-telligent-url [app-state telligent-url]
  (if telligent-url
    (when (= (hostname telligent-url) (hostname config/telligent-community-url))
      (assoc-in app-state keypaths/telligent-community-url telligent-url))
    (assoc-in app-state keypaths/telligent-community-url nil)))

(defmethod transition-state events/navigate-sign-in
  [_ event {{:keys [telligent-url]} :query-params} app-state]
  (assoc-valid-telligent-url app-state telligent-url))

(defmethod transition-state events/navigate-sign-out [_ event {{:keys [telligent-url]} :query-params} app-state]
  (assoc-valid-telligent-url app-state telligent-url))

(defn initialize-bundle-builder [app-state]
  (let [bundle-builder (bundle-builder/initialize (named-searches/current-named-search app-state)
                                                  (get-in app-state keypaths/products)
                                                  (experiments/kinky-straight? app-state))
        saved-options  (get-in app-state keypaths/saved-bundle-builder-options)]
    (if saved-options
      (bundle-builder/reset-options bundle-builder saved-options)
      bundle-builder)))

(defn ensure-bundle-builder [app-state]
  (if (and (nil? (get-in app-state keypaths/bundle-builder))
           (named-searches/products-loaded? app-state (named-searches/current-named-search app-state)))
    (-> app-state
        (assoc-in keypaths/bundle-builder (initialize-bundle-builder app-state))
        (update-in keypaths/ui dissoc :saved-bundle-builder-options))
    app-state))

(defmethod transition-state events/navigate-category [_ event {:keys [named-search-slug]} app-state]
  (let [bundle-builder-options (-> (get-in app-state keypaths/bundle-builder)
                                   bundle-builder/constrained-options
                                   (dissoc :length))]
    (-> app-state
        (assoc-in (conj keypaths/browse-named-search-query :slug) named-search-slug)
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

(defmethod transition-state events/navigate-shared-cart [_ event {:keys [shared-cart-id]} app-state]
  (assoc-in app-state keypaths/shared-cart-id shared-cart-id))

(defmethod transition-state events/navigate-checkout-sign-in [_ event args app-state]
  ;; Direct loads of checkout-sign-in should advance to checkout flow, not return to home page
  (when (= [events/navigate-home {}]
         (get-in app-state keypaths/return-navigation-message))
    (assoc-in app-state keypaths/return-navigation-message
              [events/navigate-checkout-address {}])))

(defmethod transition-state events/navigate-checkout-address [_ event args app-state]
  (cond-> app-state
    (get-in app-state keypaths/user-email)
    ;; help with analytics of funnel
    (assoc-in keypaths/navigation-message [event {:query-params {:loggedin true}}])))

(defmethod transition-state events/navigate-checkout-payment [_ event args app-state]
  (default-credit-card-name app-state (get-in app-state (conj keypaths/order :billing-address))))

(defn ^:private parse-ugc-album [album]
  (map (fn [{:keys [id user_name content_type source products title source_url pixlee_cdn_photos] :as item}]
         (let [extract-img-urls (fn [coll original large medium small]
                                  (-> coll
                                      (select-keys [original large medium small])
                                      (set/rename-keys {original :original
                                                        large    :large
                                                        medium   :medium
                                                        small    :small})
                                      (->>
                                       (remove (comp string/blank? val))
                                       (into {}))))
               root-img-urls    (extract-img-urls item :source_url :big_url :medium_url :thumbnail_url)
               cdn-img-urls     (extract-img-urls pixlee_cdn_photos :original_url :large_url :medium_url :small_url)

               imgs (reduce-kv (fn [result name url] (assoc result name {:src url :alt title}))
                               {}
                               (merge root-img-urls cdn-img-urls))

               [nav-event nav-args :as nav-message] (-> products
                                                        first
                                                        :link
                                                        url/url-decode
                                                        url/url
                                                        :path
                                                        routes/navigation-message-for)
               view-look-link                       (when (= nav-event events/navigate-shared-cart)
                                                      [events/navigate-shop-by-look-details {:look-id id}])
               ;; TODO: if the view-look? experiment wins, we will not need the purchase-look-link
               purchase-look-link                   (when (= nav-event events/navigate-shared-cart)
                                                      ;; both navigate-shared-cart and
                                                      ;; control-create-order-from-shared-cart have
                                                      ;; :shared-cart-id in the nav-message
                                                      [events/control-create-order-from-shared-cart (assoc nav-args :selected-look-id id)])]
           {:id             id
            :content-type   content_type
            :source-url     source_url
            :user-handle    user_name
            :imgs           imgs
            :social-service source
            :shared-cart-id (:shared-cart-id nav-args)
            :links          {:view-other    nav-message
                             :view-look     view-look-link
                             :purchase-look purchase-look-link}
            :title          title}))
       album))

(defmethod transition-state events/pixlee-api-success-fetch-mosaic [_ event {:keys [data]} app-state]
  (assoc-in app-state keypaths/ugc-looks
            (->> data
                 parse-ugc-album
                 ;; we have no design for how to play videos on the shop-by-look pages
                 (remove (comp #{"video"} :content-type)))))

(defmethod transition-state events/pixlee-api-success-fetch-named-search-album [_ event {:keys [album-data named-search-slug]} app-state]
  (assoc-in app-state (conj keypaths/ugc-named-searches named-search-slug)
            (parse-ugc-album album-data)))

(defmethod transition-state events/navigate-shop-by-look [_ event _ app-state]
  (assoc-in app-state keypaths/selected-look-id nil))

(defmethod transition-state events/navigate-shop-by-look-details [_ event {:keys [look-id]} app-state]
  (assoc-in app-state keypaths/selected-look-id (js/parseInt look-id)))

(defmethod transition-state events/control-popup-ugc-category [_ event {:keys [offset]} app-state]
  (-> app-state
      (assoc-in keypaths/popup :category-ugc)
      (assoc-in keypaths/ui-ugc-category-popup-offset offset)))

(defmethod transition-state events/pixlee-api-success-fetch-named-search-album-ids [_ event {:keys [data]} app-state]
  (reduce (fn [app-state {:keys [sku album_id]}]
            (if-let [named-search-slug (pixlee/sku->named-search-slug sku)]
              (assoc-in app-state (conj keypaths/named-search-slug->pixlee-album-id named-search-slug) album_id)
              app-state))
          app-state
          data))

(defn ensure-cart-has-shipping-method [app-state]
  (-> app-state
      (assoc-in keypaths/checkout-selected-shipping-method
                (merge (first (get-in app-state keypaths/shipping-methods))
                       (orders/shipping-item (:order app-state))))))

(defmethod transition-state events/navigate-checkout-confirmation [_ event args app-state]
  (-> app-state
      ensure-cart-has-shipping-method
      (update-in keypaths/checkout-credit-card-existing-cards empty)))

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

(defmethod transition-state events/control-play-video [_ events {:keys [video]} app-state]
  (-> app-state
      (assoc-in keypaths/popup :video)
      (assoc-in keypaths/video video)))

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

(defmethod transition-state events/control-bundle-option-select
  [_ event {:keys [selected-options]} app-state]
  (update-in app-state
             keypaths/bundle-builder bundle-builder/reset-options selected-options))

(defmethod transition-state events/control-checkout-shipping-method-select [_ event shipping-method app-state]
  (assoc-in app-state keypaths/checkout-selected-shipping-method shipping-method))

(defn become-guest [app-state]
  (assoc-in app-state keypaths/checkout-as-guest true))

(defmethod transition-state events/control-checkout-update-addresses-submit [_ event {:keys [become-guest?]} app-state]
  (when become-guest? (become-guest app-state)))

(defmethod transition-state events/control-checkout-as-guest-submit [_ event args app-state]
  (become-guest app-state))

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
      (update-in keypaths/order merge order)
      (update-in keypaths/bundle-builder bundle-builder/rollback)))

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
      (assoc-in app-state keypaths/errors {}))))

(defmethod transition-state events/control-stylist-account-password-submit [_ event args app-state]
  (let [stylist-account       (get-in app-state keypaths/stylist-manage-account)
        password              (-> stylist-account :user :password)
        field-errors          (cond-> {}
                                (> 6 (count password))
                                (merge (group-by :path [{:path ["user" "password"] :long-message "New password must be at least 6 characters"}])))]
    (if (seq field-errors)
      (assoc-in app-state keypaths/errors {:field-errors field-errors :error-code "invalid-input" :error-message "Oops! Please fix the errors below."})
      (assoc-in app-state keypaths/errors {}))))

(defmethod transition-state events/control-popup-hide [_ event args app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/popup nil)))

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

(defmethod transition-state events/enable-feature
  [_ event {:keys [feature]} app-state]
  (experiments/enable-feature app-state feature))

(defmethod transition-state events/inserted-convert [_ event args app-state]
  (assoc-in app-state keypaths/loaded-convert true))

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
          (assoc-in keypaths/errors {})
          (assoc-in keypaths/popup nil)
          (assoc-in keypaths/email-capture-session "opted-in")))))

(defmethod transition-state events/show-email-popup [_ event args app-state]
  (assoc-in app-state keypaths/popup :email-capture))

(defmethod transition-state events/sign-out [_ event args app-state]
  (let [signed-out-app-state (-> app-state
                                 (assoc-in keypaths/user {})
                                 (assoc-in keypaths/order nil)
                                 (assoc-in keypaths/stylist state/initial-stylist-state)
                                 (assoc-in keypaths/checkout state/initial-checkout-state)
                                 (assoc-in keypaths/billing-address {})
                                 (assoc-in keypaths/shipping-address {})
                                 (assoc-in keypaths/facebook-email-denied nil))
        opted-in?            (= "opted-in" (get-in signed-out-app-state keypaths/email-capture-session))]
    (cond-> signed-out-app-state
      (not opted-in?)
      (assoc-in keypaths/email-capture-session "dismissed"))))
