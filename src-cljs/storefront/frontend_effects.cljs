(ns storefront.frontend-effects
  (:require [ajax.core :as ajax]
            api.orders
            [api.catalog :refer [select]]
            [storefront.accessors.contentful :as contentful]
            [clojure.set :as set]
            [mayvenn.concept.email-capture :as email-capture]
            [mayvenn.concept.hard-session :as hard-session]
            [spice.maps :as maps]
            [spice.date :as date]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.events :as browser-events]
            [storefront.browser.scroll :as scroll]
            [storefront.browser.tags :as tags]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.ugc :as ugc]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.hooks.kustomer :as kustomer]
            [storefront.hooks.lucky-orange :as lucky-orange]
            [storefront.hooks.google-maps :as google-maps]
            [storefront.hooks.reviews :as reviews]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.quadpay :as quadpay]
            [storefront.hooks.seo :as seo]
            [storefront.hooks.stringer :as stringer]
            [storefront.hooks.stripe :as stripe]
            [storefront.hooks.svg :as svg]
            [storefront.hooks.uploadcare :as uploadcare]
            [storefront.accessors.line-items :as line-items]
            [storefront.hooks.spreedly :as spreedly]
            [storefront.hooks.wistia :as wistia]
            [storefront.keypaths :as keypaths]
            [adventure.keypaths :as adv-keypaths]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.routes :as routes]
            [storefront.components.share-links :as share-links]
            [storefront.components.popup :as popup]
            [spice.core :as spice]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.sites :as sites]
            [storefront.accessors.promos :as promos]
            [mayvenn.live-help.core :as live-help]
            [storefront.components.marquee :as marquee]))

(defn changed? [previous-app-state app-state keypath]
  (not= (get-in previous-app-state keypath)
        (get-in app-state keypath)))

(defn refresh-account [app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (when (and user-id user-token)
      (api/get-account user-id user-token))))

(defn refresh-current-order [app-state]
  (let [user-id      (get-in app-state keypaths/user-id)
        user-token   (get-in app-state keypaths/user-token)
        stylist-id   (get-in app-state keypaths/store-stylist-id)
        order-number (get-in app-state keypaths/order-number)
        order-token  (get-in app-state keypaths/order-token)]
    (cond
      (and user-id user-token stylist-id (not order-number))
      (api/get-current-order user-id
                             user-token
                             stylist-id)

      (and order-number order-token)
      (api/get-order {:number order-number
                      :token  order-token}))))

(defn scroll-promo-field-to-top []
  ;; In a timeout so that changes to the advertised promo aren't changing the scroll too.
  (js/setTimeout #(scroll/scroll-selector-to-top "[data-ref=promo-code]") 0))

(defmethod effects/perform-effects events/app-start [dispatch event args _ app-state]
  (quadpay/insert)
  (svg/insert-sprite)
  (riskified/insert-tracking (get-in app-state keypaths/session-id))
  (stringer/fetch-browser-id)
  (messages/handle-message events/biz|email-capture|reset)
  (refresh-account app-state)

  #_(when (= :shop (sites/determine-site app-state))
    ;; Enables Kustomer Chat 2.0
    (messages/handle-message events/flow|live-help|reset))

  (browser-events/attach-global-listeners)
  (browser-events/attach-click-away-handler)
  (browser-events/attach-esc-key-listener)
  (browser-events/attach-capture-late-readystatechange-callbacks)
  (lucky-orange/track-store-experience (get-in app-state keypaths/store-experience))
  (doseq [feature (get-in app-state keypaths/features)]
    ;; trigger GA analytics, even though feature is already enabled
    (messages/handle-message events/enable-feature {:feature feature}))
  (when (get-in app-state keypaths/user-id)
    (messages/handle-message events/user-identified {:user (get-in app-state keypaths/user)}))
  ;not= false
  (when (contains? #{nil  ; If browser doesn't support timezone, lookup anyway
                     "America/Chicago"}
                   (some-> (Intl.DateTimeFormat)
                           .resolvedOptions
                           .-timeZone))  ; limit lookup to central timezone
    (api/fetch-geo-location-from-ip (get-in app-state keypaths/api-cache)))
  (messages/handle-message events/account-profile|experience|evaluated))

(defmethod effects/perform-effects events/app-stop [_ event args _ app-state]
  (riskified/remove-tracking)
  (lucky-orange/remove-tracking)
  (browser-events/unattach-capture-late-readystatechange-callbacks)
  (browser-events/detach-click-away-handler)
  (browser-events/detach-esc-key-listener))

(defmethod effects/perform-effects events/enable-feature [_ event {:keys [feature]} _ app-state]
  (when (= feature "add-on-services") ;; Remove when experiments/add-on-services is removed
    (messages/handle-message events/save-order {:order (get-in app-state keypaths/order)}))

  (when (= feature "edit-gallery")
    (messages/handle-message events/poll-gallery))

  (when (= feature "experience-omni")
    (messages/handle-message events/account-profile|experience|evaluated)))

(defmethod effects/perform-effects events/ensure-sku-ids
  [_ _ {:keys [sku-ids]} _ app-state]
  (let [ids-in-db               (keys (get-in app-state keypaths/v2-skus))
        missing-ids             (seq (set/difference (set sku-ids)
                                                     (set ids-in-db)))
        api-cache               (get-in app-state keypaths/api-cache)
        handler                 (partial messages/handle-message
                                         events/api-success-v3-products)]
    (when missing-ids
      (api/get-products api-cache
                        {:selector/sku-ids missing-ids}
                        handler))))

(defmethod effects/perform-effects events/external-redirect-info-page
  [_ event {:keys [info-path]} _ app-state]
  (set! (.-location js/window) (str "https://shop.mayvenn.com" info-path)))

(defmethod effects/perform-effects events/external-redirect-blog-page
  [_ event {:keys [blog-path]} _ app-state]
  (set! (.-location js/window) (str "https://shop.mayvenn.com" blog-path)))

(defmethod effects/perform-effects events/external-redirect-welcome [_ event args _ app-state]
  (set! (.-location js/window) (get-in app-state keypaths/welcome-url)))

(defmethod effects/perform-effects events/external-redirect-sms [_ event {:keys [sms-message number]} _ app-state]
  (set! (.-location js/window) (share-links/sms-link sms-message number)))

(defmethod effects/perform-effects events/external-redirect-paypal-setup [_ event args _ app-state]
  (set! (.-location js/window) (get-in app-state keypaths/order-cart-payments-paypal-redirect-url)))

(defmethod effects/perform-effects events/external-redirect-quadpay-checkout [_ event {:keys [quadpay-redirect-url]} _ app-state]
  (set! (.-location js/window) quadpay-redirect-url))

(defmethod effects/perform-effects events/external-redirect-instagram-profile
  [_ _ {:keys [ig-username open-in-new-tab?]} _ app-state]
  (let [url (marquee/instagram-url ig-username)]
    (if open-in-new-tab?
      (js/window.open url)
      (set! (.-location js/window) url))))

(defmethod effects/perform-effects events/external-redirect-url
  [_ _ {:keys [url open-in-new-tab?] :or {open-in-new-tab? true}} _ _]
  (if open-in-new-tab?
    (js/window.open url)
    (set! (.-location js/window) url)))

(defmethod effects/perform-effects events/control-navigate [_ event {:keys [navigation-message]} _ app-state]
  ;; A user has clicked a link
  ;; The URL has already changed. Save scroll position on the page they are
  ;; leaving, and handle the nav message.
  (messages/handle-message events/navigation-save (-> (get-in app-state keypaths/navigation-stashed-stack-item)
                                                      (assoc :final-scroll js/window.scrollY)))
  (apply messages/handle-message navigation-message))

(defmethod effects/perform-effects events/browser-navigate
  [_ _ {:keys [navigation-message]} _ app-state]
  ;; A user has clicked the forward/back button, or maybe a special link that
  ;; simulates the back button (utils/route-back). The browser already knows
  ;; about the URL, so all we have to do is manipulate the undo/redo stacks and
  ;; handle the nav message.
  (let [leaving-stack-item {:final-scroll js/window.scrollY}
        back               (first (get-in app-state keypaths/navigation-undo-stack))
        forward            (first (get-in app-state keypaths/navigation-redo-stack))]
    (condp routes/exact-page? navigation-message
      (:navigation-message back)
      (do
        (messages/handle-message events/navigation-undo leaving-stack-item)
        (apply messages/handle-message (assoc-in navigation-message [1 :nav-stack-item] back)))

      (:navigation-message forward)
      (do
        (messages/handle-message events/navigation-redo leaving-stack-item)
        (apply messages/handle-message (assoc-in navigation-message [1 :nav-stack-item] forward)))

      (apply messages/handle-message navigation-message))))

(defmethod effects/perform-effects events/redirect
  [_ _ {[event args] :nav-message} _ _]
  (history/enqueue-redirect event args))

(defmethod effects/perform-effects events/go-to-navigate
  [_ _ {:keys [target]} _ _]
  (apply history/enqueue-navigate target))

(defn apply-pending-promo-code [app-state {:keys [number token]}]
  (let [pending-promo-code (get-in app-state keypaths/pending-promo-code)
        requesting? (utils/requesting? app-state request-keys/add-promotion-code)]
    (when (and pending-promo-code (not requesting?))
      (api/add-promotion-code {:session-id     (get-in app-state keypaths/session-id)
                               :number         number
                               :token          token
                               :promo-code     pending-promo-code
                               :allow-dormant? true}))))

(defmethod effects/perform-effects events/navigate
  [_ event {:keys [navigate/caused-by query-params nav-stack-item]} prev-app-state app-state]
  (let [[previous-nav-event previous-nav-args] (get-in prev-app-state keypaths/navigation-message)
        [current-nav-event current-nav-args]   (get-in app-state keypaths/navigation-message)

        landing-on-same-page? (and (= previous-nav-event current-nav-event)
                                   (= (dissoc previous-nav-args :query-params)
                                      (dissoc current-nav-args  :query-params)))
        module-load?          (= caused-by :module-load)
        cookie                (get-in app-state keypaths/cookie)]
    (tags/remove-classname ".kustomer-app-icon" "hide")

    (messages/handle-message events/control-menu-collapse-all)
    (messages/handle-message events/save-order {:order (get-in app-state keypaths/order)})

    (cookie-jar/save-user cookie (get-in app-state keypaths/user))
    (if-let [email-verification-token (and (not (get-in app-state keypaths/user-verified-at))
                                           (not (utils/requesting? app-state request-keys/email-verification-verify))
                                           (:evt query-params))]
      (api/email-verification-verify (get-in app-state keypaths/user-token)
                                     (get-in app-state keypaths/user-id)
                                     email-verification-token)
      (refresh-account app-state))

    (email-capture/refresh-short-timers cookie (email-capture/all-trigger-ids app-state))
    (when (:em_hash query-params)
      (email-capture/start-long-timer-if-unstarted cookie))
    (messages/handle-message events/biz|email-capture|timer-state-observed)

    (when-not (or module-load? (#{:first-nav} caused-by))
      (api/get-promotions (get-in app-state keypaths/api-cache)
                          (or
                           (first (get-in app-state keypaths/order-promotion-codes))
                           (get-in app-state keypaths/pending-promo-code))))

    (seo/set-tags app-state)

    (when-not landing-on-same-page?
      (when-let [conversation-id (get-in app-state keypaths/kustomer-conversation-id)]
        (kustomer/describe-conversation conversation-id
                                        {:page-url     (str (get-in app-state keypaths/navigation-uri))
                                         :order-number (get-in app-state keypaths/order-number)}))
      (let [restore-scroll-top (:final-scroll nav-stack-item 0)]
        (if (zero? restore-scroll-top)
          ;; We can always snap to 0, so just do it immediately. (HEAT is unhappy if the page is scrolling underneath it.)
          (scroll/snap-to-top)
          ;; Otherwise give the screen some time to render before trying to restore scroll
          (messages/handle-later events/snap {:top restore-scroll-top} 1000))))

    (when-let [pending-promo-code (:sha query-params)]
      (cookie-jar/save-pending-promo-code cookie pending-promo-code))

    (when-let [affiliate-stylist-id (:affiliate_stylist_id query-params)]
      (cookie-jar/save-affiliate-stylist-id cookie {:affiliate-stylist-id affiliate-stylist-id}))

    (when-not module-load?
      (when (get-in app-state keypaths/popup)
        (messages/handle-message events/popup-hide))
      (quadpay/hide-modal))

    (live-help/maybe-start app-state)

    (let [utm-params (some-> query-params
                             (select-keys [:utm_source :utm_medium :utm_campaign :utm_content :utm_term])
                             (set/rename-keys {:utm_source   :storefront/utm-source
                                               :utm_medium   :storefront/utm-medium
                                               :utm_campaign :storefront/utm-campaign
                                               :utm_content  :storefront/utm-content
                                               :utm_term     :storefront/utm-term})
                             (maps/deep-remove-nils))]
      (when (seq utm-params)
        (cookie-jar/save-utm-params cookie utm-params)))

    (when (and (get-in app-state keypaths/user-must-set-password)
               (not (contains? routes/routes-free-from-force-set-password event)))
      (effects/redirect events/navigate-force-set-password))

    ;; NOTE(le): This is a very hacky way of ensuring that the fragment gets scrolled to when possible
    (when-let [fragment (:fragment (history/current-uri))]
      (messages/handle-later events/control-scroll-selector-to-top
                             {:selector (str "#" fragment)
                              ;; :y here is an offset. Passing this makes it so we scroll directly to the selector intead of a bit off
                              :y scroll/scroll-padding} 100))

    (exception-handler/refresh)))

(defmethod effects/perform-effects events/navigate-home
  [_ _ _ _ app-state]
  (effects/fetch-cms2 app-state [:homepage :unified])
  (effects/fetch-cms2 app-state [:homepage :classic])
  (effects/fetch-cms2 app-state [:homepage :omni])
  (doseq [keypath [[:advertisedPromo]
                   [:ugc-collection :free-install-mayvenn]
                   [:faq :free-mayvenn-services]]]
    (effects/fetch-cms-keypath app-state keypath)))

(defmethod effects/perform-effects events/navigate-landing-page
  [_ _ args _ app-state]
  (if (= (keyword (:landing-page-slug args)) :free-install)
    (effects/redirect events/navigate-home)
    (-> app-state
        (effects/fetch-cms2 [:landingPageV2 (keyword (:landing-page-slug args))]))))

(defmethod effects/perform-effects events/navigate-content
  [_ [_ _ & static-content-id :as event] _ _ app-state]
  (when-not (= static-content-id
               (get-in app-state keypaths/static-id))
    (api/get-static-content event)))

(defmethod effects/perform-effects events/navigate-content-about-us [_ _ _ _ app-state]
  (wistia/load))

(defmethod effects/perform-effects events/navigate-shop-by-look
  [dispatch event {:keys [album-keyword]} previous-app-state app-state]
  (let [actual-album-kw (ugc/determine-look-album app-state album-keyword)]
    (cond
      (and (#{:wavy-curly-looks :straight-looks} album-keyword)
           (= :classic (sites/determine-site app-state)))
      (effects/redirect events/navigate-shop-by-look {:album-keyword :look})

      (= :ugc/unknown-album actual-album-kw)
      (effects/page-not-found)

      :else
      (let [just-arrived? (not= events/navigate-shop-by-look
                                (get-in previous-app-state keypaths/navigation-event))
            cache         (get-in app-state keypaths/api-cache)
            ;; Is there a reason why carts are only fetched for aladdin free install?
            ;; Do we still have :aladdin-free-install albums?
            handler       (if (= :aladdin-free-install actual-album-kw)
                            ;; *WARNING*, HACK: to limit how many items are
                            ;; *being rendered / fetched from the backend on this page
                            (fn [result]
                              (when-let [cart-ids (->> (get-in result [:ugc-collection :aladdin-free-install :looks])

                                                       (take 99)
                                                       (mapv contentful/shared-cart-id)
                                                       not-empty)]
                                (api/fetch-shared-carts cache cart-ids))
                              (when just-arrived?
                                (messages/handle-message events/flow|facet-filtering|initialized)))
                            (fn [result]
                              (when-let [cart-ids (->> (get-in result [:ugc-collection actual-album-kw :looks])
                                                       (take 99)
                                                       (mapv contentful/shared-cart-id)
                                                       not-empty)]
                                (api/fetch-shared-carts cache cart-ids))))]
        (effects/fetch-cms-keypath app-state [:ugc-collection actual-album-kw] handler)))))

(defmethod effects/perform-effects events/navigate-shop-by-look-details [_ event {:keys [album-keyword]} _ app-state]
  (let [actual-album-kw (ugc/determine-look-album app-state album-keyword)]
    (if-let [shared-cart-id (contentful/shared-cart-id (contentful/selected-look app-state))]
      (do
        (effects/fetch-cms-keypath app-state [:ugc-collection actual-album-kw])
        (effects/fetch-cms-keypath app-state [:faq :shop-by-look])
        (reviews/insert-reviews)
        (api/fetch-shared-cart shared-cart-id))
      (effects/redirect events/navigate-shop-by-look {:album-keyword album-keyword}))))

(defmethod effects/perform-effects events/navigate-account [_ event args _ app-state]
  (let [sign-in-data (hard-session/signed-in app-state)]
    (when (not (hard-session/allow? sign-in-data event))
      (effects/redirect events/navigate-sign-in))))

(defmethod effects/perform-effects events/navigate-stylist [_ event args _ app-state]
  (let [sign-in-data (hard-session/signed-in app-state)]
    (when (or (not (= :stylist (::auth/as sign-in-data)))
              (not (hard-session/allow? sign-in-data event)))
      (effects/redirect events/navigate-sign-in))))

(defmethod effects/perform-effects events/app-restart [_ _ _ _]
  (.reload js/window.location))

(defmethod effects/perform-effects events/api-end [_ event args previous-app-state app-state]
  (let [app-version    (get-in app-state keypaths/app-version)
        remote-version (:app-version args)
        needs-restart? (and app-version remote-version
                            (< app-version remote-version))]
    (when needs-restart?
      (messages/handle-later events/app-restart))))

(defmethod effects/perform-effects events/control-install-landing-page-look-back [_ event args _ app-state]
  (js/history.back))

(def cart-error-codes
  {"quadpay"                     "There was an issue authorizing your Zip payment. Please check out again or use a different payment method."
   "paypal-incomplete"           "We were unable to complete your order with PayPal. Please try again."
   "paypal-cancel"               "We were unable to complete your order with PayPal. Please try again."
   "paypal-invalid-address"      (str "Unfortunately, Mayvenn products cannot be delivered to this address at this time. "
                                      "Please choose a new shipping destination. ")
   "ineligible-for-free-install" (str "The 'FreeInstall' promotion code has been removed from your order."
                                      " Please visit shop.mayvenn.com to complete your order.")
   "discounts-changed"           "Your discounts have changed. Double-check below and then proceed to check out."})

(defmethod effects/perform-effects events/navigate-cart
  [_ event args _ app-state]
  (api/get-shipping-methods)
  (api/get-states (get-in app-state keypaths/api-cache))
  ;; Fetch SKUS of services in case they aren't there- they are needed for add-to-bag stringer events
  ;; TODO: re-evaluate necessity of this request
  (api/get-products (get-in app-state keypaths/api-cache)
                    {:catalog/department "service"
                     :service/category   "install"}
                    (fn [response]
                      (messages/handle-message events/api-success-v3-products response)))
  (google-maps/insert) ;; for address screen on the next page
  (stripe/insert)
  (quadpay/insert)
  (refresh-current-order app-state)
  (messages/handle-message events/cache|current-stylist|requested)
  (when-let [error-msg (-> args :query-params :error cart-error-codes)]
    (messages/handle-message events/flash-show-failure {:message error-msg})))

(defn ensure-bucketed-for [app-state experiment]
  (let [already-bucketed? (contains? (get-in app-state keypaths/experiments-bucketed) experiment)]
    (when-not already-bucketed?
      (when-let [variation (experiments/variation-for app-state experiment)]
        (messages/handle-message events/bucketed-for {:experiment experiment :variation variation})
        (messages/handle-message events/enable-feature {:experiment experiment :feature (:feature variation)})))))

(defmethod effects/perform-effects events/navigate-checkout [_ event {:keys [navigate/caused-by]} _ app-state]
  (when (not= :module-load caused-by)
    (stripe/insert))
  (google-maps/insert)
  (let [have-cart? (get-in app-state keypaths/order-number)]
    (when (not have-cart?)
      (effects/redirect events/navigate-cart))

    (when (and have-cart?
               (not (auth/signed-in-or-initiated-guest-checkout? app-state))
               (not (#{events/navigate-checkout-add
                       events/navigate-checkout-address
                       events/navigate-checkout-returning-or-guest
                       events/navigate-checkout-sign-in
                       events/navigate-checkout-processing} event)))

      (effects/redirect events/navigate-checkout-address))))

(defmethod effects/perform-effects events/navigate-checkout-returning-or-guest [_ event args _ app-state]
  (when (get-in app-state keypaths/user-id)
    (effects/redirect events/navigate-checkout-address args))
  (google-maps/remove-containers)
  (api/get-states (get-in app-state keypaths/api-cache)))

(defn- fetch-saved-cards [app-state]
  (when-let [user-id (get-in app-state keypaths/user-id)]
    (api/get-saved-cards user-id (get-in app-state keypaths/user-token))))

(defmethod effects/perform-effects events/navigate-checkout-address [_ event args _ app-state]
  (when-not (get-in app-state keypaths/user-id)
    (effects/redirect events/navigate-checkout-returning-or-guest args))
  (google-maps/remove-containers)
  (api/get-states (get-in app-state keypaths/api-cache))
  (fetch-saved-cards app-state))

(defmethod effects/perform-effects events/navigate-checkout-payment [dispatch event args _ app-state]
  (when (empty? (get-in app-state keypaths/order-shipping-address))
    (effects/redirect events/navigate-checkout-address))
  (fetch-saved-cards app-state)
  (quadpay/insert))

(defmethod effects/perform-effects events/navigate-checkout-confirmation [_ event args _ app-state]
  ;; To guarantee that shipping times update
  (messages/handle-later events/redirect
                         {:nav-message [events/navigate-checkout-confirmation]}
                         (let [now                   (date/now)
                               minutes               (.getMinutes now)
                               seconds               (.getSeconds now)
                               seconds-past-the-hour (+ (* 60 minutes) seconds)
                               seconds-til-the-hour  (inc (- 3600 seconds-past-the-hour))]
                           (* 1000 seconds-til-the-hour)))
  ;; TODO: get the credit card component to function correctly on direct page load
  (when (empty? (get-in app-state keypaths/order-cart-payments))
    (effects/redirect events/navigate-checkout-payment))
  (api/get-shipping-methods))


;;; Discountable promotions
(def ^:private ?bundles
  {:catalog/department #{"hair"}
   :hair/family        #{"bundles"}})

(def ^:private ?closures
  {:catalog/department #{"hair"}
   :hair/family        #{"closures"}})

(def ^:private ?frontals
  {:catalog/department #{"hair"}
   :hair/family        #{"frontals"}})

(def ^:private ?360-frontals
  {:catalog/department #{"hair"}
   :hair/family        #{"360-frontals"}})

(def SV2-rules
  {"SRV-LBI-000"  [["bundle" ?bundles 3]]
   "SRV-UPCW-000" [["bundle" ?bundles 3]]
   "SRV-CBI-000"  [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "SRV-CLCW-000" [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "SRV-FBI-000"  [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "SRV-LFCW-000" [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "SRV-3BI-000"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
   "SRV-3CW-000"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]})

(defn ^:private check-rules [waiter-order condition-fn [service-slug rule]]
  (let [physical-items (->> waiter-order
                            :shipments
                            (mapcat :line-items)
                            (filter (fn [item]
                                      (= "spree" (:source item))))
                            (map (fn [item]
                                   (merge
                                    (dissoc item :variant-attrs)
                                    (:variant-attrs item)))))]
    (keep (fn [[word essentials rule-quantity]]
            (let [cart-quantity    (->> physical-items
                                        (select essentials)
                                        (map :quantity)
                                        (apply +))
                  missing-quantity (- rule-quantity cart-quantity)]
              (when (condition-fn missing-quantity)
                {:word             word
                 :service-slug     service-slug
                 :cart-quantity    cart-quantity
                 :missing-quantity missing-quantity
                 :essentials       essentials})))
          rule)))

(def rule-succeeded? zero?)

(def rule-failed? pos?)

(defmethod effects/perform-effects events/navigate-order-complete
  [_ _ {{:keys [paypal order-token]} :query-params number :number} _ state]
  (let [waiter-order        (get-in state keypaths/completed-order)
        query        (some-> (:post-purchase-stylist-matching-geo-location waiter-order)
                             (clojure.set/rename-keys {:lat :latitude
                                                       :lng :longitude})
                             (assoc :radius "100mi")
                             (assoc :preferred-services (some (comp :service-slug (partial check-rules waiter-order rule-succeeded?)) SV2-rules)))
        waiter-order (:waiter/order (api.orders/completed state))]
    ;; TODO(corey) getting the current stylist here seems odd, may
    ;; want to fetch the previous-stylist instead
    (messages/handle-message events/cache|current-stylist|requested)
    (when (and (:remove-free-install (get-in state storefront.keypaths/features))
               (some (comp empty? (partial check-rules rule-failed? waiter-order)) SV2-rules))
               query)
      (api/fetch-stylists-matching-filters query
                                           #(messages/handle-message events/api-success-fetch-stylists-matching-filters
                                                                     (merge {:query query} %))))
    (when (and number
               order-token)
      (api/get-completed-order number order-token))
    (when paypal
      (effects/redirect events/navigate-order-complete
                        {:number number})))

(defmethod effects/perform-effects events/api-success-get-completed-order [_ event order _ app-state]
  (messages/handle-message events/order-completed order))

(defn redirect-to-return-navigation [app-state]
  (apply effects/redirect
         (get-in app-state keypaths/return-navigation-message)))

(defn redirect-when-signed-in
  [app-state]
  (when (hard-session/allow? (hard-session/signed-in app-state)
                             (first (get-in app-state keypaths/return-navigation-message)))
    (redirect-to-return-navigation app-state)
    (messages/handle-message events/flash-later-show-success
                             {:message "You are already signed in."})))

(defmethod effects/perform-effects events/navigate-sign-out [_ _ _ _ app-state]
  (messages/handle-message events/sign-out))

(defmethod effects/perform-effects events/navigate-not-found [_ event args _ app-state]
  (effects/redirect events/navigate-home)
  (exception-handler/report {:error "navigate-not-found"})
  (messages/handle-message events/flash-later-show-failure
                           {:message "The page you were looking for could not be found."}))

(defmethod effects/perform-effects events/control-sign-in-submit [_ event args _ app-state]
  (api/sign-in (get-in app-state keypaths/session-id)
               (get-in app-state keypaths/stringer-browser-id)
               (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-number)
               (get-in app-state keypaths/order-token)))

(defmethod effects/perform-effects events/control-sign-up-submit [_ event _ _ app-state]
  (let [{:keys [number token]} (or (get-in app-state keypaths/order)
                                   (get-in app-state keypaths/completed-order))]
    (api/sign-up (get-in app-state keypaths/session-id)
                 (get-in app-state keypaths/stringer-browser-id)
                 (get-in app-state keypaths/sign-up-email)
                 (get-in app-state keypaths/sign-up-password)
                 (get-in app-state keypaths/store-stylist-id)
                 number
                 token)))

(defn abort-pending-requests [requests]
  (doseq [{xhr :xhr} requests] (when xhr (ajax/abort xhr))))

(defmethod effects/perform-effects events/control-sign-out [_ event args _ app-state]
  (messages/handle-message events/sign-out))

(defmethod effects/perform-effects events/control-forgot-password-submit [_ event args _ app-state]
  (api/forgot-password (get-in app-state keypaths/session-id) (get-in app-state keypaths/forgot-password-email)))

(defmethod effects/perform-effects events/image-picker-component-mounted [_ _ args _ app-state]
  (uploadcare/dialog args))

(defmethod effects/perform-effects events/control-account-profile-submit [_ event args _ app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (api/update-account (get-in app-state keypaths/session-id)
                        (get-in app-state keypaths/user-id)
                        (get-in app-state keypaths/manage-account-email)
                        (get-in app-state keypaths/manage-account-password)
                        (get-in app-state keypaths/user-token))))

(defmethod effects/perform-effects events/uploadcare-api-success-upload-portrait [_ _ {:keys [cdnUrl]} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/user-store-id)
        session-id (get-in app-state keypaths/session-id)]
    (api/update-stylist-account-portrait session-id user-id user-token stylist-id {:portrait-url cdnUrl})
    (history/enqueue-navigate events/navigate-stylist-account-profile)))

(defmethod effects/perform-effects events/uploadcare-api-success-upload-gallery [_ event {:keys [cdnUrl]} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (if (experiments/edit-gallery? app-state)
      (api/append-stylist-v2-gallery user-id user-token {:gallery-urls [cdnUrl]})
      (api/append-stylist-gallery user-id user-token {:gallery-urls [cdnUrl]}))
    (history/enqueue-navigate events/navigate-gallery-edit)))

(defmethod effects/perform-effects events/control-checkout-update-addresses-submit [_ event args _ app-state]
  (let [guest-checkout?            (get-in app-state keypaths/checkout-as-guest)
        billing-address            (get-in app-state keypaths/checkout-billing-address)
        shipping-address           (get-in app-state keypaths/checkout-shipping-address)
        phone-marketing-opt-in     (get-in app-state keypaths/checkout-phone-marketing-opt-in)
        phone-transactional-opt-in (get-in app-state keypaths/checkout-phone-transactional-opt-in)
        update-addresses           (if guest-checkout? api/guest-update-addresses api/update-addresses)]
    (update-addresses
     (get-in app-state keypaths/session-id)
     (cond-> (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                    {:billing-address billing-address
                     :shipping-address shipping-address
                     :phone-marketing-opt-in phone-marketing-opt-in
                     :phone-txn-opt-in phone-transactional-opt-in})
       guest-checkout?
       (assoc :email (get-in app-state keypaths/checkout-guest-email))

       (get-in app-state keypaths/checkout-bill-to-shipping-address)
       (assoc :billing-address shipping-address)))))

(defmethod effects/perform-effects events/control-checkout-shipping-method-select [_ event args _ app-state]
  (api/update-shipping-method (get-in app-state keypaths/session-id)
                              (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                                     {:shipping-method-sku (get-in
                                                            app-state
                                                            keypaths/checkout-selected-shipping-method-sku)})))

(defmethod effects/perform-effects events/stripe-success-create-token [_ _ {:keys [token place-order?]} _ app-state]
  (let [order (get-in app-state keypaths/order)
        user  (get-in app-state keypaths/user)]
    (api/update-cart-payments
     (get-in app-state keypaths/session-id)
     {:order        (-> order
                        (select-keys [:token :number])
                        (assoc :cart-payments (merge {:stripe {:source         (:id token)
                                                               :idempotent-key (str (random-uuid))
                                                               :save?          (boolean (and (get-in app-state keypaths/user-id)
                                                                                             (get-in app-state keypaths/checkout-credit-card-save)))}}
                                                     (when (orders/can-use-store-credit? order user)
                                                       {:store-credit {}}))))
      :navigate     events/navigate-checkout-confirmation
      :place-order? place-order?})))

(defn create-stripe-token [app-state args]
  ;; create stripe token (success handler commands waiter w/ payment methods (success navigates to confirm))
  (stripe/create-token (get-in app-state keypaths/stripe-card-element)
                       (get-in app-state keypaths/checkout-credit-card-name)
                       (get-in app-state (conj keypaths/order :billing-address))
                       args))

(defmethod effects/perform-effects events/order-remove-promotion [_ _ {:keys [code hide-success]} _ app-state]
  (api/remove-promotion-code
   (get-in app-state keypaths/session-id)
   (get-in app-state keypaths/order)
   code
   #(messages/handle-message events/api-success-update-order-remove-promotion-code
                             {:order        %
                              :hide-success hide-success
                              :promo-code   code})))

(defmethod effects/perform-effects events/order-remove-freeinstall-line-item [_ _ _ _ app-state]
  (api/remove-freeinstall-line-item (get-in app-state keypaths/session-id) (get-in app-state keypaths/order)))

(defmethod effects/perform-effects events/control-checkout-remove-promotion [_ _ {:as args :keys [code]} _ app-state]
  (messages/handle-message events/order-remove-promotion args))

(defmethod effects/perform-effects events/control-checkout-confirmation-submit [_ event {:keys [place-order?] :as args} _ app-state]
  (let [order (get-in app-state keypaths/order)]
    (if place-order?
      (create-stripe-token app-state args)
      (api/place-order (get-in app-state keypaths/session-id)
                       order
                       (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie))
                       (stylists/retrieve-parsed-affiliate-id app-state)))))

(defmethod effects/perform-effects events/save-order
  [_ _ {:keys [order]} _ state]
  ;; Clear the order if it is submitted or frozen
  (if (and order
           (orders/incomplete? order))
    (do
      (let [sku-ids (->> (:shipments order)
                         (mapcat :storefront/all-line-items)
                         (remove line-items/shipping-method?)
                         (map :sku))]
        (messages/handle-message events/ensure-sku-ids
                                 {:sku-ids sku-ids}))
      (messages/handle-message events/cache|current-stylist|requested)
      (cookie-jar/save-order (get-in state keypaths/cookie)
                             order)
      (apply-pending-promo-code state
                              order))
    (messages/handle-message events/clear-order))

  ;; TODO(corey) not sure what use case this is covering
  (when (-> order orders/service-line-items not-empty)
    (api/get-skus
     (get-in state keypaths/api-cache)
     {:catalog/department                 "service"
      :service/type                       "base"
      :service/category                   ["install"]
      :promo.mayvenn-install/discountable true}
     #(messages/handle-message events/api-success-get-skus %))))

(defmethod effects/perform-effects events/clear-order [_ _ _ _ app-state]
  (cookie-jar/clear-order (get-in app-state keypaths/cookie)))

(defmethod effects/perform-effects events/cart-cleared [_ _ _ _ app-state]
  ;; HACK: By adding a freeinstall sku to the cart without specifying the order token
  ;; the service gets automatically removed and we create a new cart.
  (let [removed-items (orders/product-items (get-in app-state keypaths/order))]
    (api/add-sku-to-bag
     (get-in app-state keypaths/session-id)
     {:sku                {:catalog/sku-id                     "SV2-LBI-X"
                           :promo.mayvenn-install/discountable true}
      :quantity           1
      :stylist-id         (get-in app-state keypaths/store-stylist-id)
      :heat-feature-flags (keys (filter second (get-in app-state keypaths/features)))}
     (fn success-handler [updated-order]
       (doseq [{:keys [sku quantity]} removed-items]
         (messages/handle-message events/order-line-item-removed {:sku-id   sku
                                                                  :quantity quantity
                                                                  :order    updated-order}))
       ;; Why must we continue this add-sku-to-bag charade?
       (messages/handle-message events/api-success-add-sku-to-bag
                                {:order    updated-order
                                 :quantity 1
                              ;; NOTE: Nil sku prevents the tracking behavior on this handler
                                 :sku      nil})))))


(defmethod effects/perform-effects events/api-success-auth [_ _ {:keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (messages/handle-message events/biz|hard-session|begin {:token "true"})
  (messages/handle-message events/user-identified {:user (get-in app-state keypaths/user)})
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user))
  (redirect-to-return-navigation app-state))

(defmethod effects/perform-effects events/api-success-account [_ _event _args _prev-app-state app-state]
  ;; NOTE(ellie+andres, 2022-02-09): In the future this could actually get a whole new session token from diva
  (messages/handle-later events/biz|hard-session|refresh))

(defmethod effects/perform-effects events/api-success-auth-sign-in
  [_ _ _ _ app-state]
  (messages/handle-message events/flash-later-show-success
                           {:message "Logged in successfully"}))

(defmethod effects/perform-effects events/api-success-auth-sign-up
  [dispatch event args _ app-state]
  (messages/handle-message events/flash-later-show-success
                           {:message (str "Please click the link we've emailed to "
                                          (-> args :user :email)
                                          " to verify your email address.")}))

(defmethod effects/perform-effects events/api-success-auth-reset-password [dispatch event args _ app-state]
  (messages/handle-message events/flash-later-show-success {:message "Your password was changed successfully. You are now signed in."}))

(defmethod effects/perform-effects events/api-success-forgot-password [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-home)
  (messages/handle-message events/flash-later-show-success {:message "You will receive an email with instructions on how to reset your password in a few minutes."}))

(defmethod effects/perform-effects events/api-success-manage-account [_ event args _ app-state]
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user))
  (history/enqueue-navigate events/navigate-home)
  (messages/handle-message events/flash-later-show-success {:message "Account updated"}))

(defmethod effects/perform-effects events/poll-stylist-portrait [_ event args _ app-state]
  (api/refresh-stylist-portrait (get-in app-state keypaths/user-id)
                                (get-in app-state keypaths/user-token)
                                (get-in app-state keypaths/user-store-id)))

(defmethod effects/perform-effects events/api-success-stylist-account
  [_ event {:keys [stylist]} previous-app-state app-state]
  ;; Portrait becomes pending either when the user navigates to an account page
  ;; or when they change their portrait.
  ;; In either case, we start the poll loop.
  (when-let [became-pending? (and
                              (changed? previous-app-state app-state keypaths/stylist-portrait-status)
                              (= "pending" (get-in app-state keypaths/stylist-portrait-status)))]
    (messages/handle-later events/poll-stylist-portrait {} 5000))
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user)))

(defmethod effects/perform-effects events/api-success-stylist-account-profile [_ event args _ app-state]
  (messages/handle-message events/flash-show-success {:message "Profile updated"}))

(defmethod effects/perform-effects events/api-success-stylist-account-password [_ event args _ app-state]
  (messages/handle-message events/flash-show-success {:message "Password updated"}))

(defmethod effects/perform-effects events/api-success-stylist-account-commission [_ event args _ app-state]
  (messages/handle-message events/flash-show-success {:message "Commission settings updated"}))

(defmethod effects/perform-effects events/api-success-stylist-account-social [_ event args _ app-state]
  (messages/handle-message events/flash-show-success {:message "Social settings updated"}))

(defmethod effects/perform-effects events/api-success-stylist-account-portrait [_ event {:keys [updated?]} previous-app-state app-state]
  (when updated?
    (messages/handle-message events/flash-show-success {:message "Photo updated"}))
  (when-let [still-pending? (= "pending"
                               (get-in previous-app-state keypaths/stylist-portrait-status)
                               (get-in app-state keypaths/stylist-portrait-status))]
    (messages/handle-later events/poll-stylist-portrait {} 5000)))

(defmethod effects/perform-effects events/api-success-update-order-place-order
  [_ _ {:keys [order]} _ _]
  (history/enqueue-navigate events/navigate-order-complete order)
  (messages/handle-message events/order-completed order)
  (messages/handle-message events/order-placed order))

(defmethod effects/perform-effects events/order-completed
  [_ _ order _ app-state]
  (cookie-jar/save-completed-order (get-in app-state keypaths/cookie)
                                   (get-in app-state keypaths/completed-order))
  (messages/handle-message events/clear-order)
  (when (= :shop (sites/determine-site app-state))
    (when-let [stylist-id (:servicing-stylist-id order)]
      (messages/handle-message events/cache|stylist|requested
                               {:stylist/id stylist-id}))))

(defmethod effects/perform-effects events/api-success-update-order-update-cart-payments [_ event {:keys [order place-order?]} _ app-state]
  (when place-order?
    (api/place-order (get-in app-state keypaths/session-id)
                     order
                     (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie))
                     (stylists/retrieve-parsed-affiliate-id app-state))))

(defmethod effects/perform-effects events/api-success-update-order
  [_ _ {:keys [order navigate event] :on/keys [success]} _ _]
  (messages/handle-message events/save-order {:order order})
  (when event
    (messages/handle-message event {:order order}))
  ;; This is a roll-forward shim
  ;; navigate is an event name
  ;; on/success is a follow up [event args], that we merge args with this events args
  (if-let [[success-event success-args] success]
    (history/enqueue-navigate success-event
                              (merge
                               success-args
                               {:number (:number order)}))
    (when navigate
      (history/enqueue-navigate navigate {:number (:number order)}))))

(defmethod effects/perform-effects events/api-success-get-order
  [_ _ order _ _]
  (messages/handle-message events/save-order {:order order}))

(defmethod effects/perform-effects events/api-failure-no-network-connectivity [_ event response _ app-state]
  (messages/handle-message events/flash-show-failure {:message "Something went wrong. Please refresh and try again or contact customer service."}))

(defmethod effects/perform-effects events/api-failure-bad-server-response [_ event response _ app-state]
  (messages/handle-message events/flash-show-failure {:message "Uh oh, an error occurred. Reload the page and try again."}))

(defmethod effects/perform-effects events/flash-show [_ event {:keys [scroll?] :or {scroll? true}} _ app-state]
  (when scroll?
    (scroll/snap-to-top)))

(defmethod effects/perform-effects events/snap [_ _ {:keys [top]} _ app-state]
  (scroll/snap-to top))

(defmethod effects/perform-effects events/api-failure-pending-promo-code [_ event args _ app-state]
  (cookie-jar/clear-pending-promo-code (get-in app-state keypaths/cookie)))

(defmethod effects/perform-effects events/api-failure-order-not-created-from-shared-cart [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-home))

(defmethod effects/perform-effects events/api-failure-errors [_ event {:keys [error-code scroll-selector] :as errors} _ app-state]
  (condp = error-code
    "stripe-card-failure"           (when (= (get-in app-state keypaths/navigation-event)
                                             events/navigate-checkout-confirmation)
                                      (effects/redirect events/navigate-checkout-payment)
                                      (messages/handle-later events/api-failure-errors errors)
                                      (scroll/snap-to-top))
    "promotion-not-found"           (scroll-promo-field-to-top)
    "stylist-wrong-store-promotion" (scroll-promo-field-to-top)
    "ineligible-for-promotion"      (scroll-promo-field-to-top)
    "invalid-input"                 (if scroll-selector
                                      (scroll/scroll-selector-to-top scroll-selector)
                                      (scroll/snap-to-top))
    "ineligible-for-free-install"   (when (= (get-in app-state keypaths/navigation-event)
                                             events/navigate-checkout-confirmation)
                                      (api/remove-promotion-code (get-in app-state keypaths/session-id)
                                                                 (get-in app-state keypaths/order)
                                                                 "freeinstall"
                                                                 #(messages/handle-message events/api-success-update-order
                                                                                           {:order      %
                                                                                            :promo-code "freeinstall"}))
                                      (effects/redirect events/navigate-cart)
                                      (messages/handle-later events/api-failure-errors errors)
                                      (scroll/snap-to-top))
    (scroll/snap-to-top)))

(defmethod effects/perform-effects events/api-success-decrease-quantity 
  [_dispatch _event {:as args :keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (apply-pending-promo-code app-state order)
  (messages/handle-later events/control-scroll-to-selector {:selector "[data-ref=start-checkout-button]"})
  (messages/handle-message events/order-line-item-removed args))

(defmethod effects/perform-effects events/api-success-remove-from-bag 
  [_dispatch _event {:as args :keys [order]} _ _app-state]
  (messages/handle-message events/save-order {:order order})
  (messages/handle-message events/order-line-item-removed args))

(defmethod effects/perform-effects events/api-success-add-sku-to-bag [dispatch event {:keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (messages/handle-later events/control-scroll-to-selector {:selector "[data-ref=start-checkout-button]"}))


(defmethod effects/perform-effects events/api-success-add-multiple-skus-to-bag [dispatch event {:keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (messages/handle-later events/control-scroll-to-selector {:selector "[data-ref=start-checkout-button]"}))

(defmethod effects/perform-effects events/checkout-address-component-mounted
  [_ event {:keys [address-elem address-keypath]} _ app-state]
  (google-maps/attach "address" address-elem address-keypath))

(defmethod effects/perform-effects events/api-success-update-order-remove-promotion-code
  [_ _ {:keys [hide-success]} _ app-state]
  (when-not hide-success
    (messages/handle-message events/flash-show-success
                             {:message "The coupon code was successfully removed from your order."
                              :scroll? false})))

(defmethod effects/perform-effects events/api-success-update-order-add-promotion-code 
  [_ _ {:keys [allow-dormant? order promo-code]} _ app-state]
  (when-not allow-dormant?
    (messages/handle-message events/flash-show-success
                             {:message "The coupon code was successfully applied to your order."
                              :scroll? false}))
  (api/get-promotions (get-in app-state keypaths/api-cache)
                      (first (get-in app-state keypaths/order-promotion-codes)))
  (messages/handle-message events/order-promo-code-added {:order-number (:number order) 
                                                          :promo-code   promo-code}))

(defmethod effects/perform-effects events/sign-out [_ event args app-state-before app-state]
  (messages/handle-message events/clear-order)
  (cookie-jar/clear-account (get-in app-state keypaths/cookie))
  (messages/handle-message events/control-menu-collapse-all)
  (email-capture/start-long-timer-if-unstarted (get-in app-state keypaths/cookie))
  (abort-pending-requests (get-in app-state keypaths/api-requests))
  (if (= events/navigate-home (get-in app-state keypaths/navigation-event))
    (messages/handle-message events/flash-show-success {:message "Logged out successfully"})
    (do
      (history/enqueue-navigate events/navigate-home)
      (messages/handle-message events/flash-later-show-success {:message "Logged out successfully"})))
  (api/sign-out (get-in app-state keypaths/session-id)
                (get-in app-state keypaths/stringer-browser-id)
                (get-in app-state-before keypaths/user-id)
                (get-in app-state-before keypaths/user-token)))

(defmethod effects/perform-effects events/navigate-voucher [_ event args app-state-before app-state]
  (api/fetch-user-stylist-service-menu (get-in app-state keypaths/api-cache)
                                       {:user-id    (get-in app-state keypaths/user-id)
                                        :user-token (get-in app-state keypaths/user-token)
                                        :stylist-id (get-in app-state keypaths/user-store-id)}))

(defmethod effects/perform-effects events/module-loaded [_ _ {:keys [module-name for-navigation-event]} app-state-before app-state]
  (let [already-loaded-module? (= (get-in app-state-before keypaths/modules)
                                  (get-in app-state keypaths/modules))
        [evt args]             (get-in app-state keypaths/navigation-message)]
    (when (and (not already-loaded-module?)
               (= for-navigation-event evt))
      (messages/handle-message evt (assoc args :navigate/caused-by :module-load)))))

(defmethod effects/perform-effects events/external-redirect-typeform-recommend-stylist
  [_ _ _ _ _ _]
  (set! (.-location js/window) "https://mayvenn.typeform.com/to/J2Y1cC"))

(defmethod effects/perform-effects events/browser-back
  [_ _ _ _ _ _]
  (js/history.back))

(defmethod effects/perform-effects events/order-placed
  [_ _ order _ _]
  (messages/handle-message events/user-identified {:user {:id    (-> order :user :id)
                                                          :email (-> order :user :email)}}))

(defmethod effects/perform-effects events/api-success-update-order-update-guest-address
  [_ _ {:keys [order]} _ _]
  (messages/handle-message events/set-user-ecd 
                           (assoc (:shipping-address order) 
                                  :email 
                                  (-> order :user :email)))
  (messages/handle-message events/user-identified {:user (:user order)}))

(defmethod effects/perform-effects events/user-identified
  [_ _ _ _ app-state]
  (email-capture/start-long-timer-if-unstarted (get-in app-state keypaths/cookie)))

(defmethod effects/perform-effects events/api-success-update-order-update-address
  [_ event {:keys [order]} _ app-state]
  (messages/handle-message events/set-user-ecd 
                           (assoc (:shipping-address order) 
                                  :email
                                  (-> order :user :email))))