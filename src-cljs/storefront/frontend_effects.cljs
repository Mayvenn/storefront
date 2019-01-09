(ns storefront.frontend-effects
  (:require [ajax.core :as ajax]
            [clojure.set :as set]
            [clojure.string :as string]
            [spice.maps :as maps]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.credit-cards :refer [filter-cc-number-format parse-expiration]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.pixlee :as accessors.pixlee]
            [storefront.accessors.stylists :as stylists]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.events :as browser-events]
            [storefront.browser.scroll :as scroll]
            [storefront.config :as config]
            [storefront.effects :as effects :refer [page-not-found perform-effects redirect]]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.hooks.apple-pay :as apple-pay]
            [storefront.hooks.convert :as convert]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.hooks.facebook :as facebook]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.lucky-orange :as lucky-orange]
            [storefront.hooks.pinterest :as pinterest]
            [storefront.hooks.pixlee :as pixlee]
            [storefront.hooks.places-autocomplete :as places-autocomplete]
            [storefront.hooks.reviews :as reviews]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.seo :as seo]
            [storefront.hooks.stringer :as stringer]
            [storefront.hooks.stripe :as stripe]
            [storefront.hooks.svg :as svg]
            [storefront.hooks.talkable :as talkable]
            [storefront.hooks.uploadcare :as uploadcare]
            [storefront.hooks.wistia :as wistia]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages :refer [handle-later handle-message]]
            [storefront.routes :as routes]
            [storefront.accessors.nav :as nav]
            [storefront.components.share-links :as share-links]))

(defn- email-capture-session [app-state]
  (cookie-jar/retrieve-email-capture-session (get-in app-state keypaths/cookie)))

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
        order        (get-in app-state keypaths/order)
        order-number (get-in app-state keypaths/order-number)
        order-token  (get-in app-state keypaths/order-token)]
    (cond
      (and user-id user-token stylist-id (not order-number))
      (api/get-current-order user-id
                             user-token
                             stylist-id)

      (and order-number order-token)
      (api/get-order order-number order-token))))

(defn touch-email-capture-session [app-state]
  (when-let [value (email-capture-session app-state)]
    (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) value)))

(defn scroll-promo-field-to-top []
  ;; In a timeout so that changes to the advertised promo aren't changing the scroll too.
  (js/setTimeout #(scroll/scroll-selector-to-top "[data-ref=promo-code]") 0))

(defmethod perform-effects events/app-start [dispatch event args _ app-state]
  (let [choices (-> (get-in app-state keypaths/cookie)
                    cookie-jar/retrieve-adventure
                    :choices
                    js/decodeURIComponent
                    js/JSON.parse
                    (js->clj :keywordize-keys true))]
    (handle-message events/control-adventure
                    {:choice choices}))
  (svg/insert-sprite)
  (stringer/insert-tracking (get-in app-state keypaths/store-slug))
  (google-analytics/insert-tracking)
  (convert/insert-tracking)
  (riskified/insert-tracking (get-in app-state keypaths/session-id))
  (facebook-analytics/insert-tracking)
  (pinterest/insert-tracking)
  (talkable/insert)
  (places-autocomplete/insert)
  (refresh-account app-state)
  (browser-events/attach-global-listeners)
  (lucky-orange/track-store-experience (get-in app-state keypaths/store-experience))
  (doseq [feature (get-in app-state keypaths/features)]
    ;; trigger GA analytics, even though feature is already enabled
    (handle-message events/enable-feature {:feature feature})))

(defmethod perform-effects events/app-stop [_ event args _ app-state]
  (convert/remove-tracking)
  (riskified/remove-tracking)
  (stringer/remove-tracking)
  (google-analytics/remove-tracking)
  (facebook-analytics/remove-tracking)
  (pinterest/remove-tracking)
  (lucky-orange/remove-tracking)
  (pixlee/remove-tracking))

(defmethod perform-effects events/determine-and-show-popup
  [_ event args previous-app-state app-state]
  (let [navigation-event (get-in app-state keypaths/navigation-event)

        the-ville-variation? (experiments/the-ville? app-state)
        v2-experience?       (experiments/v2-experience? app-state)

        on-non-minimal-footer-page? (not (nav/show-minimal-footer? navigation-event))

        is-on-homepage? (= navigation-event events/navigate-home)

        is-on-free-install-landing-page? (= navigation-event events/navigate-install-home)

        seen-email-capture?      (email-capture-session app-state)
        seen-fayetteville-offer? (get-in app-state keypaths/dismissed-free-install)

        signed-in? (get-in app-state keypaths/user-id)

        show-free-install-modal? (and the-ville-variation?
                                      (not seen-fayetteville-offer?)
                                      (not v2-experience?))

        classic-experience? (and (not v2-experience?)
                                 (not the-ville-variation?))

        show-email-capture? (and (not signed-in?)
                                 (not seen-email-capture?)
                                 on-non-minimal-footer-page?
                                 (or (and the-ville-variation? seen-fayetteville-offer?)
                                     classic-experience?
                                     v2-experience?))]
    (cond
      is-on-free-install-landing-page? nil

      show-free-install-modal?
      (handle-message events/popup-show-free-install)

      signed-in?
      (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "signed-in")

      show-email-capture?
      (handle-message events/popup-show-email-capture))))

(defmethod perform-effects events/enable-feature [_ event {:keys [feature]} _ app-state]
  (handle-message events/determine-and-show-popup)
  (let [nav-event (get-in app-state keypaths/navigation-event)]
    (when (and (routes/sub-page? [nav-event] [events/navigate-install-home])
               (= "adventure" feature))
      (history/enqueue-redirect events/navigate-adventure-home))
    (when (= "the-ville" feature)
      (pixlee/fetch-album-by-keyword :free-install))))

(defmethod perform-effects events/ensure-sku-ids
  [_ _ {:keys [sku-ids]} _ app-state]
  (let [ids-in-db   (keys (get-in app-state keypaths/v2-skus))
        missing-ids (seq (set/difference (set sku-ids)
                                         (set ids-in-db)))

        api-cache (get-in app-state keypaths/api-cache)
        handler   (partial messages/handle-message
                           events/api-success-v2-products)]
    (when missing-ids
      (api/search-v2-products api-cache
                              {:selector/sku-ids missing-ids}
                              handler))))

(defmethod perform-effects events/external-redirect-welcome [_ event args _ app-state]
  (set! (.-location js/window) (get-in app-state keypaths/welcome-url)))

(defmethod perform-effects events/external-redirect-sms [_ event {:keys [sms-message number]} _ app-state]
  (set! (.-location js/window) (share-links/sms-link sms-message number)))

(defmethod perform-effects events/external-redirect-paypal-setup [_ event args _ app-state]
  (set! (.-location js/window) (get-in app-state keypaths/order-cart-payments-paypal-redirect-url)))

(defmethod perform-effects events/external-redirect-telligent [_ event args _ app-state]
  (set! (.-location js/window) (or (get-in app-state keypaths/telligent-community-url)
                                   config/telligent-community-url)))

(defmethod perform-effects events/control-navigate [_ event {:keys [navigation-message]} _ app-state]
  ;; A user has clicked a link
  ;; The URL has already changed. Save scroll position on the page they are
  ;; leaving, and handle the nav message.
  (handle-message events/navigation-save (-> (get-in app-state keypaths/navigation-stashed-stack-item)
                                             (assoc :final-scroll js/document.body.scrollTop)))
  (apply handle-message navigation-message))

(defmethod perform-effects events/browser-navigate [_ _ {:keys [navigation-message]} _ app-state]
  ;; A user has clicked the forward/back button, or maybe a special link that
  ;; simulates the back button (utils/route-back). The browser already knows
  ;; about the URL, so all we have to do is manipulate the undo/redo stacks and
  ;; handle the nav message.
  (let [leaving-stack-item {:final-scroll js/document.body.scrollTop}
        back               (first (get-in app-state keypaths/navigation-undo-stack))
        forward            (first (get-in app-state keypaths/navigation-redo-stack))]
    (condp routes/exact-page? navigation-message
      (:navigation-message back)
      (do
        (handle-message events/navigation-undo leaving-stack-item)
        (apply handle-message (assoc-in navigation-message [1 :nav-stack-item] back)))

      (:navigation-message forward)
      (do
        (handle-message events/navigation-redo leaving-stack-item)
        (apply handle-message (assoc-in navigation-message [1 :nav-stack-item] forward)))

      (apply handle-message navigation-message))))

(defmethod perform-effects events/redirect [_ event {:keys [nav-message]} _ app-state]
  (apply history/enqueue-redirect nav-message))

(defn add-pending-promo-code [app-state {:keys [number token] :as order}]
  (when-let [pending-promo-code (get-in app-state keypaths/pending-promo-code)]
    (api/add-promotion-code (get-in app-state keypaths/session-id) number token pending-promo-code true)))

(defmethod perform-effects events/navigate [_ event {:keys [query-params nav-stack-item] :as args} prev-app-state app-state]
  (let [args               (dissoc args :nav-stack-item)]
    (handle-message events/control-menu-collapse-all)
    (handle-message events/save-order {:order (get-in app-state keypaths/order)})
    (cookie-jar/save-user (get-in app-state keypaths/cookie)
                          (get-in app-state keypaths/user))
    (refresh-account app-state)
    (api/get-promotions (get-in app-state keypaths/api-cache)
                        (or
                         (first (get-in app-state keypaths/order-promotion-codes))
                         (get-in app-state keypaths/pending-promo-code)))

    (seo/set-tags app-state)
    (when (or (not= (get-in prev-app-state keypaths/navigation-event)
                    (get-in app-state keypaths/navigation-event))
              (not= (not-empty (dissoc (get-in prev-app-state keypaths/navigation-args) :query-params))
                    (not-empty (dissoc (get-in app-state keypaths/navigation-args) :query-params))))
      (let [restore-scroll-top (:final-scroll nav-stack-item 0)]
        (if (zero? restore-scroll-top)
          ;; We can always snap to 0, so just do it immediately. (HEAT is unhappy if the page is scrolling underneath it.)
          (scroll/snap-to-top)
          ;; Otherwise give the screen some time to render before trying to restore scroll
          (handle-later events/snap {:top restore-scroll-top} 100))))

    (when-let [pending-promo-code (:sha query-params)]
      (cookie-jar/save-pending-promo-code
       (get-in app-state keypaths/cookie)
       pending-promo-code)
      (redirect event (update-in args [:query-params] dissoc :sha)))

    (handle-message events/determine-and-show-popup)

    (let [utm-params (some-> query-params
                             (select-keys [:utm_source :utm_medium :utm_campaign :utm_content :utm_term])
                             (set/rename-keys {:utm_source   :storefront/utm-source
                                               :utm_medium   :storefront/utm-medium
                                               :utm_campaign :storefront/utm-campaign
                                               :utm_content  :storefront/utm-content
                                               :utm_term     :storefront/utm-term})
                             (maps/remove-nils))]
      (when (seq utm-params)
        (cookie-jar/save-utm-params
         (get-in app-state keypaths/cookie)
         utm-params)))

    (when (get-in app-state keypaths/popup)
      (handle-message events/control-popup-hide))

    (when (and (get-in app-state keypaths/user-must-set-password)
               (not= event events/navigate-force-set-password))
      (redirect events/navigate-force-set-password))

    (exception-handler/refresh)

    (touch-email-capture-session app-state)))

(defmethod perform-effects events/navigate-home [_ _ {:keys [query-params]} _ app-state]
  (api/fetch-cms-data)
  (when (experiments/v2-homepage? app-state)
    (handle-message events/v2-show-home))
  (when (= config/install-subdomain (get-in app-state keypaths/store-slug))
    (redirect events/navigate-install-home)))

(defmethod perform-effects events/navigate-content [_ [_ _ & static-content-id :as event] _ _ app-state]
  (when-not (= static-content-id
               (get-in app-state keypaths/static-id))
    (api/get-static-content event)))

(defmethod perform-effects events/navigate-content-about-us [_ _ _ _ app-state]
  (wistia/load))

(defmethod perform-effects events/navigate-shop-by-look
  [dispatch event {:keys [album-keyword]} _ app-state]
  (let [actual-album-keyword (accessors.pixlee/determine-look-album app-state album-keyword)]
    (if (and (experiments/v2-experience? app-state)
             (= album-keyword :deals))
      (redirect events/navigate-home) ; redirect to home page from /shop/deals for v2-experience
      (cond (= :pixlee/unknown-album actual-album-keyword)
            (page-not-found)

            ;; Only fetch this album if you are viewing it (not it's look-details/specific photo)
            (= dispatch event)
            (pixlee/fetch-album-by-keyword actual-album-keyword)))))

(defmethod perform-effects events/navigate-shop-by-look-details [_ event {:keys [album-keyword look-id]} _ app-state]
  (if-let [shared-cart-id (:shared-cart-id (accessors.pixlee/selected-look app-state))]
    (api/fetch-shared-cart shared-cart-id)
    (pixlee/fetch-image album-keyword look-id)))

(defmethod perform-effects events/pixlee-api-success-fetch-image [_ event _ _ app-state]
  (when-let [shared-cart-id (:shared-cart-id (accessors.pixlee/selected-look app-state))]
    (api/fetch-shared-cart shared-cart-id)))

(defmethod perform-effects events/pixlee-api-failure-fetch-album [_ event resp _ app-state]
  (page-not-found))

(defmethod perform-effects events/navigate-account [_ event args _ app-state]
  (when-not (get-in app-state keypaths/user-token)
    (redirect events/navigate-sign-in)))

(defmethod perform-effects events/navigate-stylist [_ event args _ app-state]
  (cond
    (not (get-in app-state keypaths/user-token))
    (redirect events/navigate-sign-in)

    (not (stylists/own-store? app-state))
    (page-not-found)

    :else nil))

(defmethod perform-effects events/navigate-stylist-account [_ event args _ app-state]
  (let [user-token (get-in app-state keypaths/user-token)
        user-id    (get-in app-state keypaths/user-id)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (when (and user-token stylist-id)
      (uploadcare/insert)
      (api/get-states (get-in app-state keypaths/api-cache))
      (api/get-stylist-account user-id user-token stylist-id))))

(defmethod perform-effects events/navigate-gallery [_ event args _ app-state]
  (api/get-gallery (if (stylists/own-store? app-state)
                     {:user-id (get-in app-state keypaths/user-id)
                      :user-token (get-in app-state keypaths/user-token)}
                     {:stylist-id (get-in app-state keypaths/store-stylist-id)})))

(defmethod perform-effects events/navigate-gallery-image-picker [_ event args _ app-state]
  (if (stylists/own-store? app-state)
    (uploadcare/insert)
    (redirect events/navigate-gallery)))

(defmethod perform-effects events/control-delete-gallery-image [_ event {:keys [image-url]} _ app-state]
  (api/delete-gallery-image (get-in app-state keypaths/user-id)
                            (get-in app-state keypaths/user-token)
                            image-url))

(defmethod perform-effects events/api-success-gallery [_ event args _ app-state]
  (cond
    (not (stylists/gallery? app-state))
    (page-not-found)

    (and (stylists/own-store? app-state)
         (routes/exact-page? (get-in app-state keypaths/navigation-message) [events/navigate-gallery])
         (some (comp #{"pending"} :status) (get-in app-state keypaths/store-gallery-images)))
    (handle-later events/poll-gallery {} 5000)))

(defmethod perform-effects events/control [_ _ args _ app-state]
  (touch-email-capture-session app-state))

(defmethod perform-effects events/control-email-captured-submit [_ _ args _ app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (facebook-analytics/subscribe)
    (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "opted-in")))

(defmethod perform-effects events/app-restart [_ _ _ _]
  (.reload js/window.location))

(defmethod perform-effects events/api-end [_ event args previous-app-state app-state]
  (let [app-version    (get-in app-state keypaths/app-version)
        remote-version (:app-version args)
        needs-restart? (and app-version remote-version
                            (< app-version remote-version))]
    (when needs-restart?
      (handle-later events/app-restart))))

(defmethod perform-effects events/api-success-telligent-login [_ event {:keys [cookie max-age]} _ app-state]
  (cookie-jar/save-telligent-cookie (get-in app-state keypaths/cookie) cookie max-age)
  (handle-message events/external-redirect-telligent))

(defmethod perform-effects events/navigate-stylist-dashboard [_ event args _ app-state]
  (let [user-token (get-in app-state keypaths/user-token)
        user-id    (get-in app-state keypaths/user-id)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (when (and user-token stylist-id)
      (api/get-stylist-account user-id user-token stylist-id)
      (api/get-stylist-payout-stats
        events/api-success-stylist-payout-stats
        stylist-id user-id user-token))))

(defmethod perform-effects events/control-install-landing-page-look-back [_ event args _ app-state]
  (js/history.back))

(def cart-error-codes
  {"paypal-incomplete"           "We were unable to complete your order with PayPal. Please try again."
   "paypal-invalid-address"      (str "Unfortunately, Mayvenn products cannot be delivered to this address at this time. "
                                      "Please choose a new shipping destination. ")
   "ineligible-for-free-install" (str "The 'FreeInstall' promotion code has been removed from your order. "
                                      "This offer is only eligible for orders that ship to an address in Fayetteville, NC. "
                                      "To complete your purchase without the 'FreeInstall' promotion, "
                                      "please continue with your order below.")})

(defmethod perform-effects events/navigate-cart [_ event args _ app-state]
  (api/get-shipping-methods)
  (api/get-states (get-in app-state keypaths/api-cache))
  (stripe/insert)
  (refresh-current-order app-state)
  (when-let [error-msg (-> args :query-params :error cart-error-codes)]
    (handle-message events/flash-show-failure {:message error-msg})))

(defn ensure-bucketed-for [app-state experiment]
  (let [already-bucketed? (contains? (get-in app-state keypaths/experiments-bucketed) experiment)]
    (when-not already-bucketed?
      (when-let [variation (experiments/variation-for app-state experiment)]
        (handle-message events/bucketed-for {:experiment experiment :variation variation})
        (handle-message events/enable-feature {:experiment experiment :feature (:feature variation)})))))

(defmethod perform-effects events/navigate-checkout [_ event args _ app-state]
  (let [have-cart? (get-in app-state keypaths/order-number)]
    (when-not have-cart?
      (redirect events/navigate-cart))
    (when (and have-cart?
               (not (auth/signed-in-or-initiated-guest-checkout? app-state))
               (not (#{events/navigate-checkout-address
                       events/navigate-checkout-returning-or-guest
                       events/navigate-checkout-sign-in
                       events/navigate-checkout-processing} event)))
      (redirect events/navigate-checkout-address))))

(defmethod perform-effects events/navigate-checkout-sign-in [_ event args _ app-state]
  (facebook/insert))

(defmethod perform-effects events/navigate-checkout-returning-or-guest [_ event args _ app-state]
  (places-autocomplete/remove-containers)
  (api/get-states (get-in app-state keypaths/api-cache))
  (facebook/insert))

(defn- fetch-saved-cards [app-state]
  (when-let [user-id (get-in app-state keypaths/user-id)]
    (api/get-saved-cards user-id (get-in app-state keypaths/user-token))))

(defmethod perform-effects events/navigate-checkout-address [_ event args _ app-state]
  (when-not (get-in app-state keypaths/user-id)
    (redirect events/navigate-checkout-returning-or-guest))
  (places-autocomplete/remove-containers)
  (api/get-states (get-in app-state keypaths/api-cache))
  (fetch-saved-cards app-state))

(defmethod perform-effects events/navigate-checkout-payment [dispatch event args _ app-state]
  (when (empty? (get-in app-state keypaths/order-shipping-address))
    (redirect events/navigate-checkout-address))
  (fetch-saved-cards app-state)
  (stripe/insert))

(defmethod perform-effects events/navigate-checkout-confirmation [_ event args _ app-state]
  ;; TODO: get the credit card component to function correctly on direct page load
  (when (empty? (get-in app-state keypaths/order-cart-payments))
    (redirect events/navigate-checkout-payment))
  (stripe/insert)
  (api/get-shipping-methods))

(defmethod perform-effects events/navigate-order-complete [_ event {{:keys [paypal order-token]} :query-params number :number} _ app-state]
  (when (not (get-in app-state keypaths/user-id))
    (facebook/insert))
  (when (and number order-token)
    (api/get-completed-order number order-token))
  (when paypal
    (redirect events/navigate-order-complete {:number number})))

(defmethod perform-effects events/navigate-friend-referrals [_ event args _ app-state]
  (if (= event events/navigate-friend-referrals-freeinstall)
    (talkable/show-referrals app-state "fayetteville-offer")
    (talkable/show-referrals app-state)))

(defmethod perform-effects events/navigate-account-referrals [_ event args _ app-state]
  (talkable/show-referrals app-state))

(defmethod perform-effects events/api-success-get-completed-order [_ event order _ app-state]
  (handle-message events/order-completed order))

(defn redirect-to-return-navigation [app-state]
  (apply redirect
         (get-in app-state keypaths/return-navigation-message)))

(defn ^:private redirect-to-telligent-as-user [app-state]
  (api/telligent-sign-in (get-in app-state keypaths/session-id)
                         (get-in app-state keypaths/user-id)
                         (get-in app-state keypaths/user-token))
  (handle-message events/flash-later-show-success {:message "Redirecting to the stylist community"}) )

(defn redirect-when-signed-in [app-state]
  (when (get-in app-state keypaths/user-email)
    (if (get-in app-state keypaths/telligent-community-url)
      (redirect-to-telligent-as-user app-state)
      (do
        (redirect-to-return-navigation app-state)
        (handle-message events/flash-later-show-success {:message "You are already signed in."})))))

(defmethod perform-effects events/navigate-sign-in [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))

(defmethod perform-effects events/navigate-sign-out [_ _ {{:keys [telligent-url]} :query-params} _ app-state]
  (if telligent-url
    (do
      (cookie-jar/clear-telligent-session (get-in app-state keypaths/cookie))
      (handle-message events/external-redirect-telligent))
    (handle-message events/sign-out)))
(defmethod perform-effects events/navigate-sign-up [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-forgot-password [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))
(defmethod perform-effects events/navigate-reset-password [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))

(defmethod perform-effects events/navigate-not-found [_ event args _ app-state]
  (handle-message events/flash-show-failure
                  {:message "The page you were looking for could not be found."}))

(defmethod perform-effects events/control-sign-in-submit [_ event args _ app-state]
  (api/sign-in (get-in app-state keypaths/session-id)
               (stringer/browser-id)
               (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-number)
               (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/control-sign-up-submit [_ event _ _ app-state]
  (let [{:keys [number token]} (or (get-in app-state keypaths/order)
                                   (get-in app-state keypaths/completed-order))]
    (api/sign-up (get-in app-state keypaths/session-id)
                 (stringer/browser-id)
                 (get-in app-state keypaths/sign-up-email)
                 (get-in app-state keypaths/sign-up-password)
                 (get-in app-state keypaths/store-stylist-id)
                 number
                 token)))

(defmethod perform-effects events/control-facebook-sign-in [_ event args _ app-state]
  (facebook/start-log-in app-state))

(defmethod perform-effects events/control-facebook-reset [_ event args _ app-state]
  (facebook/start-reset app-state))

(defmethod perform-effects events/facebook-success-sign-in [_ _ {:keys [authResponse]} _ app-state]
  (let [{:keys [number token]} (or (get-in app-state keypaths/order)
                                   (get-in app-state keypaths/completed-order))]
    (api/facebook-sign-in (get-in app-state keypaths/session-id)
                          (stringer/browser-id)
                          (:userID authResponse)
                          (:accessToken authResponse)
                          (get-in app-state keypaths/store-stylist-id)
                          number
                          token)))

(defmethod perform-effects events/facebook-failure-sign-in [_ _ args _ app-state]
  (handle-message events/flash-show-failure
                  {:message "Could not sign in with Facebook.  Please try again, or sign in with email and password."}))

(defmethod perform-effects events/facebook-email-denied [_ _ args _ app-state]
  (handle-message events/flash-show-failure
                  {:message "We need your Facebook email address to communicate with you about your orders. Please try again."}))

(defn- abort-pending-requests [requests]
  (doseq [{xhr :xhr} requests] (when xhr (ajax/abort xhr))))

(defmethod perform-effects events/control-sign-out [_ event args _ app-state]
  (handle-message events/sign-out))

(defmethod perform-effects events/control-forgot-password-submit [_ event args _ app-state]
  (api/forgot-password (get-in app-state keypaths/session-id) (get-in app-state keypaths/forgot-password-email)))

(defmethod perform-effects events/control-reset-password-submit [_ event args _ app-state]
  (if (empty? (get-in app-state keypaths/reset-password-password))
    (handle-message events/flash-show-failure {:message "Your password cannot be blank."})
    (api/reset-password (get-in app-state keypaths/session-id)
                        (stringer/browser-id)
                        (get-in app-state keypaths/reset-password-password)
                        (get-in app-state keypaths/reset-password-token)
                        (get-in app-state keypaths/order-number)
                        (get-in app-state keypaths/order-token)
                        (get-in app-state keypaths/store-stylist-id))))

(defmethod perform-effects events/facebook-success-reset [_ event facebook-response _ app-state]
  (api/facebook-reset-password (get-in app-state keypaths/session-id)
                               (stringer/browser-id)
                               (-> facebook-response :authResponse :userID)
                               (-> facebook-response :authResponse :accessToken)
                               (get-in app-state keypaths/reset-password-token)
                               (get-in app-state keypaths/order-number)
                               (get-in app-state keypaths/order-token)
                               (get-in app-state keypaths/store-stylist-id)))

(defmethod perform-effects events/control-account-profile-submit [_ event args _ app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (api/update-account (get-in app-state keypaths/session-id)
                        (get-in app-state keypaths/user-id)
                        (get-in app-state keypaths/manage-account-email)
                        (get-in app-state keypaths/manage-account-password)
                        (get-in app-state keypaths/user-token))))

(defmethod perform-effects events/control-create-order-from-shared-cart [_ event {:keys [look-id shared-cart-id] :as args} _ app-state]
  (api/create-order-from-cart (get-in app-state keypaths/session-id)
                              shared-cart-id
                              look-id
                              (get-in app-state keypaths/user-id)
                              (get-in app-state keypaths/user-token)
                              (get-in app-state keypaths/store-stylist-id)))

(defmethod perform-effects events/control-stylist-account-profile-submit [_ _ args _ app-state]
  (let [session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/store-stylist-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (dissoc (get-in app-state keypaths/stylist-manage-account)
                                :green-dot-payout-attributes)]
    (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                events/api-success-stylist-account-profile)))

(defmethod perform-effects events/control-stylist-account-password-submit [_ _ args _ app-state]
  (let [session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/store-stylist-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (dissoc (get-in app-state keypaths/stylist-manage-account)
                                :green-dot-payout-attributes)]
    (when (empty? (get-in app-state keypaths/errors))
      (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                  events/api-success-stylist-account-password))))

(defn reformat-green-dot [greendot-attributes]
  (let [{:keys [expiration-date card-number] :as attributes}
        (select-keys greendot-attributes [:expiration-date
                                          :card-number
                                          :card-first-name
                                          :card-last-name
                                          :postalcode])]
    (when (seq card-number)
      (let [[month year] (parse-expiration (str expiration-date))]
        (-> attributes
            (dissoc :expiration-date)
            (assoc :expiration-month month)
            (assoc :expiration-year year)
            (update :card-number (comp string/join filter-cc-number-format str)))))))

(defmethod perform-effects events/control-stylist-account-commission-submit [_ _ args _ app-state]
  (let [session-id       (get-in app-state keypaths/session-id)
        stylist-id       (get-in app-state keypaths/store-stylist-id)
        user-id          (get-in app-state keypaths/user-id)
        user-token       (get-in app-state keypaths/user-token)
        stylist-account  (-> (get-in app-state keypaths/stylist-manage-account)
                             (update :green-dot-payout-attributes reformat-green-dot)
                             maps/deep-remove-nils)]
    (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                events/api-success-stylist-account-commission)))

(defmethod perform-effects events/control-stylist-account-social-submit [_ _ _ _ app-state]
  (let [session-id      (get-in app-state keypaths/session-id)
        stylist-id      (get-in app-state keypaths/store-stylist-id)
        user-id         (get-in app-state keypaths/user-id)
        user-token      (get-in app-state keypaths/user-token)
        stylist-account (dissoc (get-in app-state keypaths/stylist-manage-account)
                                :green-dot-payout-attributes)]
    (api/update-stylist-account session-id user-id user-token stylist-id stylist-account
                                events/api-success-stylist-account-social)))

(defmethod perform-effects events/uploadcare-api-failure [_ _ {:keys [error error-data]} _ app-state]
  (exception-handler/report error error-data))

(defmethod perform-effects events/image-picker-component-mounted [_ _ args _ app-state]
  (uploadcare/dialog args))

(defmethod perform-effects events/uploadcare-api-success-upload-portrait [_ _ {:keys [cdnUrl]} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id      (get-in app-state keypaths/store-stylist-id)
        session-id (get-in app-state keypaths/session-id)]
    (api/update-stylist-account-portrait session-id user-id user-token stylist-id {:portrait-url cdnUrl})
    (history/enqueue-navigate events/navigate-stylist-account-profile)))

(defmethod perform-effects events/uploadcare-api-success-upload-gallery [_ event {:keys [cdnUrl]} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (api/append-stylist-gallery user-id user-token {:gallery-urls [cdnUrl]})
    (history/enqueue-navigate events/navigate-gallery)))

(defmethod perform-effects events/control-checkout-update-addresses-submit [_ event args _ app-state]
  (let [guest-checkout? (get-in app-state keypaths/checkout-as-guest)
        billing-address (get-in app-state keypaths/checkout-billing-address)
        shipping-address (get-in app-state keypaths/checkout-shipping-address)
        update-addresses (if guest-checkout? api/guest-update-addresses api/update-addresses)]
    (update-addresses
     (get-in app-state keypaths/session-id)
     (cond-> (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                    {:billing-address billing-address :shipping-address shipping-address})
       guest-checkout?
       (assoc :email (get-in app-state keypaths/checkout-guest-email))

       (get-in app-state keypaths/checkout-bill-to-shipping-address)
       (assoc :billing-address shipping-address)))))

(defmethod perform-effects events/control-checkout-shipping-method-select [_ event args _ app-state]
  (api/update-shipping-method (get-in app-state keypaths/session-id)
                              (merge (select-keys (get-in app-state keypaths/order) [:number :token])
                                     {:shipping-method-sku (get-in
                                                            app-state
                                                            keypaths/checkout-selected-shipping-method-sku)})))

(defmethod perform-effects events/stripe-success-create-token [_ _ {:keys [token place-order?]} _ app-state]
  (let [order                  (get-in app-state keypaths/order)
        available-store-credit (or (get-in app-state keypaths/user-total-available-store-credit) 0.0)]
    (api/update-cart-payments
     (get-in app-state keypaths/session-id)
     {:order        (-> order
                        (select-keys [:token :number])
                        (assoc :cart-payments (merge {:stripe {:source         (:id token)
                                                               :idempotent-key (str (random-uuid))
                                                               :save?          (boolean (and (get-in app-state keypaths/user-id)
                                                                                             (get-in app-state keypaths/checkout-credit-card-save)))}}
                                                     (when (and (pos? available-store-credit)
                                                                (not (orders/applied-install-promotion order)))
                                                       {:store-credit {}}))))
      :navigate     events/navigate-checkout-confirmation
      :place-order? place-order?})))

(defn create-stripe-token [app-state args]
  ;; create stripe token (success handler commands waiter w/ payment methods (success navigates to confirm))
  (stripe/create-token (get-in app-state keypaths/stripe-card-element)
                       (get-in app-state keypaths/checkout-credit-card-name)
                       (get-in app-state (conj keypaths/order :billing-address))
                       args))

(defmethod perform-effects events/order-remove-promotion [_ _ {:keys [code hide-success]} _ app-state]
  (api/remove-promotion-code
   (get-in app-state keypaths/session-id)
   (get-in app-state keypaths/order)
   code
   #(handle-message events/api-success-update-order-remove-promotion-code
                    {:order        %
                     :hide-success hide-success
                     :promo-code   code})))

(defmethod perform-effects events/control-checkout-remove-promotion [_ _ args _ app-state]
  (handle-message events/order-remove-promotion args))

(defmethod perform-effects events/control-checkout-confirmation-submit [_ event {:keys [place-order?] :as args} _ app-state]
  (if place-order?
    (create-stripe-token app-state args)
    (api/place-order (get-in app-state keypaths/session-id)
                     (get-in app-state keypaths/order)
                     (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie)))))

(defmethod perform-effects events/save-order
  [_ _ {:keys [order]} _ app-state]
  (if (and order (orders/incomplete? order))
    (do
      (when-let [sku-ids (->> order orders/product-items (map :sku) seq)]
        (handle-message events/ensure-sku-ids {:sku-ids sku-ids}))
      (cookie-jar/save-order (get-in app-state keypaths/cookie) order)
      (add-pending-promo-code app-state order))
    (handle-message events/clear-order)))

(defmethod perform-effects events/clear-order [_ _ _ _ app-state]
  (cookie-jar/clear-order (get-in app-state keypaths/cookie)))

(defmethod perform-effects events/api-success-auth [_ _ {:keys [order]} _ app-state]
  (handle-message events/save-order {:order order})
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user))
  (redirect-to-return-navigation app-state))

(defmethod perform-effects events/api-success-auth-sign-in [_ _ _ _ app-state]
  (if (get-in app-state keypaths/telligent-community-url)
    (redirect-to-telligent-as-user app-state)
    (handle-message events/flash-later-show-success {:message "Logged in successfully"})))

(defmethod perform-effects events/api-success-auth-sign-up [dispatch event args _ app-state]
  (handle-message events/flash-later-show-success {:message "Welcome! You have signed up successfully."}))

(defmethod perform-effects events/api-success-auth-reset-password [dispatch event args _ app-state]
  (handle-message events/flash-later-show-success {:message "Your password was changed successfully. You are now signed in."}))

(defmethod perform-effects events/api-success-forgot-password [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-home)
  (handle-message events/flash-later-show-success {:message "You will receive an email with instructions on how to reset your password in a few minutes."}))

(defmethod perform-effects events/api-success-manage-account [_ event args _ app-state]
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user))
  (history/enqueue-navigate events/navigate-home)
  (handle-message events/flash-later-show-success {:message "Account updated"}))

(defmethod perform-effects events/poll-stylist-portrait [_ event args _ app-state]
  (api/refresh-stylist-portrait (get-in app-state keypaths/user-id)
                                (get-in app-state keypaths/user-token)
                                (get-in app-state keypaths/store-stylist-id)))

(defmethod perform-effects events/poll-gallery [_ event args _ app-state]
  (when (stylists/own-store? app-state)
    (api/get-gallery {:user-id    (get-in app-state keypaths/user-id)
                      :user-token (get-in app-state keypaths/user-token)})))

(defmethod perform-effects events/api-success-stylist-account
  [_ event {:keys [stylist]} previous-app-state app-state]
  ;; Portrait becomes pending either when the user navigates to an account page
  ;; or when they change their portrait.
  ;; In either case, we start the poll loop.
  (when-let [became-pending? (and
                              (changed? previous-app-state app-state keypaths/stylist-portrait-status)
                              (= "pending" (get-in app-state keypaths/stylist-portrait-status)))]
    (handle-later events/poll-stylist-portrait {} 5000))
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user)))

(defmethod perform-effects events/api-success-stylist-account-profile [_ event args _ app-state]
  (handle-message events/flash-show-success {:message "Profile updated"}))

(defmethod perform-effects events/api-success-stylist-account-password [_ event args _ app-state]
  (handle-message events/flash-show-success {:message "Password updated"}))

(defmethod perform-effects events/api-success-stylist-account-commission [_ event args _ app-state]
  (handle-message events/flash-show-success {:message "Commission settings updated"}))

(defmethod perform-effects events/api-success-stylist-account-social [_ event args _ app-state]
  (handle-message events/flash-show-success {:message "Social settings updated"}))

(defmethod perform-effects events/api-success-stylist-account-portrait [_ event {:keys [updated?]} previous-app-state app-state]
  (when updated?
    (handle-message events/flash-show-success {:message "Photo updated"}))
  (when-let [still-pending? (= "pending"
                               (get-in previous-app-state keypaths/stylist-portrait-status)
                               (get-in app-state keypaths/stylist-portrait-status))]
    (handle-later events/poll-stylist-portrait {} 5000)))

(defmethod perform-effects events/api-success-send-stylist-referrals [_ event args _ app-state]
  (handle-later events/control-popup-hide {} 2000))

(defmethod perform-effects events/api-success-update-order-place-order [_ event {:keys [order]} _ app-state]
  (handle-message events/order-completed order))

(defmethod perform-effects events/order-completed [dispatch event order _ app-state]
  (handle-message events/clear-order)
  (talkable/show-pending-offer app-state))

(defmethod perform-effects events/api-success-update-order-update-cart-payments [_ event {:keys [order place-order?]} _ app-state]
  (when place-order?
    (api/place-order (get-in app-state keypaths/session-id)
                     order
                     (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie)))))

(defmethod perform-effects events/api-success-update-order [_ event {:keys [order navigate event]} _ app-state]
  (handle-message events/save-order {:order order})
  (when event
    (handle-message event {:order order}))
  (when navigate
    (history/enqueue-navigate navigate {:number (:number order)})))

(defmethod perform-effects events/api-success-get-order [_ event order _ app-state]
  (handle-message events/save-order {:order order}))

(defmethod perform-effects events/api-failure-no-network-connectivity [_ event response _ app-state]
  (handle-message events/flash-show-failure {:message "Something went wrong. Please refresh and try again or contact customer service."}))

(defmethod perform-effects events/api-failure-bad-server-response [_ event response _ app-state]
  (handle-message events/flash-show-failure {:message "Uh oh, an error occurred. Reload the page and try again."}))

(defmethod perform-effects events/flash-show [_ event {:keys [scroll?] :or {scroll? true}} _ app-state]
  (when scroll?
    (scroll/snap-to-top)))

(defmethod perform-effects events/snap [_ _ {:keys [top]} _ app-state]
  (scroll/snap-to top))

(defmethod perform-effects events/api-failure-pending-promo-code [_ event args _ app-state]
  (cookie-jar/clear-pending-promo-code (get-in app-state keypaths/cookie)))

(defmethod perform-effects events/api-failure-order-not-created-from-shared-cart [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-home))

(defmethod perform-effects events/api-failure-errors [_ event {:keys [error-code scroll-selector] :as errors} _ app-state]
  (condp = error-code
    "stripe-card-failure"         (when (= (get-in app-state keypaths/navigation-event)
                                           events/navigate-checkout-confirmation)
                                    (redirect events/navigate-checkout-payment)
                                    (handle-later events/api-failure-errors errors)
                                    (scroll/snap-to-top))
    "promotion-not-found"         (scroll-promo-field-to-top)
    "ineligible-for-promotion"    (scroll-promo-field-to-top)
    "invalid-input"               (if scroll-selector
                                    (scroll/scroll-selector-to-top scroll-selector)
                                    (scroll/snap-to-top))
    "ineligible-for-free-install" (when (= (get-in app-state keypaths/navigation-event)
                                           events/navigate-checkout-confirmation)
                                    (api/remove-promotion-code (get-in app-state keypaths/session-id)
                                                               (get-in app-state keypaths/order)
                                                               "freeinstall"
                                                               #(handle-message events/api-success-update-order
                                                                                {:order      %
                                                                                 :promo-code "freeinstall"}))
                                    (redirect events/navigate-cart)
                                    (handle-later events/api-failure-errors errors)
                                    (scroll/snap-to-top))
    (scroll/snap-to-top)))

(defmethod perform-effects events/api-success-add-to-bag [dispatch event {:keys [order]} _ app-state]
  (handle-message events/save-order {:order order})
  (add-pending-promo-code app-state order)
  (handle-later events/added-to-bag))

(defmethod perform-effects events/api-success-remove-from-bag [dispatch event {:keys [order]} _ app-state]
  (handle-message events/save-order {:order order}))

(defmethod perform-effects events/api-success-add-sku-to-bag [dispatch event {:keys [order]} _ app-state]
  (handle-message events/save-order {:order order})
  (add-pending-promo-code app-state order)
  (handle-later events/added-to-bag))

(defmethod perform-effects events/added-to-bag [_ _ _ _ app-state]
  (when-let [el (.querySelector js/document "[data-ref=cart-button]")]
    (scroll/scroll-to-elem el)))

(defmethod perform-effects events/reviews-component-mounted [_ event args _ app-state]
  (let [expected-reviews-count 2
        actual-reviews-count (get-in app-state keypaths/review-components-count)]
    (when (= expected-reviews-count actual-reviews-count)
      (reviews/start))))

(defmethod perform-effects events/checkout-address-component-mounted
  [_ event {:keys [address-elem address-keypath]} _ app-state]
  (places-autocomplete/attach address-elem address-keypath))

(defmethod perform-effects events/api-success-update-order-remove-promotion-code
  [_ _ {:keys [hide-success]} _ app-state]
  (when-not hide-success
    (handle-message events/flash-show-success {:message "The coupon code was successfully removed from your order."
                                               :scroll? false})))

(defmethod perform-effects events/api-success-update-order-add-promotion-code [_ _ {allow-dormant? :allow-dormant?} _ app-state]
  (when-not allow-dormant?
    (handle-message events/flash-show-success {:message "The coupon code was successfully applied to your order."
                                               :scroll? false})
    (scroll-promo-field-to-top))
  (api/get-promotions (get-in app-state keypaths/api-cache)
                      (first (get-in app-state keypaths/order-promotion-codes))))

(defmethod perform-effects events/inserted-talkable [_ event args _ app-state]
  (talkable/show-pending-offer app-state)
  (let [nav-event (get-in app-state keypaths/navigation-event)]
    (cond
      (= events/navigate-friend-referrals-freeinstall nav-event)
      (talkable/show-referrals app-state "fayetteville-offer")

      (#{events/navigate-friend-referrals events/navigate-account-referrals} nav-event)
      (talkable/show-referrals app-state))))

(defmethod perform-effects events/inserted-stripe [_ event args _ app-state]
  (apple-pay/verify-eligible app-state))

(defmethod perform-effects events/control-email-captured-dismiss [_ event args _ app-state]
  (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "dismissed"))

(defmethod perform-effects events/control-stylist-community [_ event args _ app-state]
  (api/telligent-sign-in (get-in app-state keypaths/session-id)
                         (get-in app-state keypaths/user-id)
                         (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/sign-out [_ event args app-state-before app-state]
  (when (not= "opted-in" (email-capture-session app-state))
    (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "dismissed"))
  (handle-message events/clear-order)
  (cookie-jar/clear-account (get-in app-state keypaths/cookie))
  (handle-message events/control-menu-collapse-all)
  (abort-pending-requests (get-in app-state keypaths/api-requests))
  (if (= events/navigate-home (get-in app-state keypaths/navigation-event))
    (handle-message events/flash-show-success {:message "Logged out successfully"})
    (do
      (history/enqueue-navigate events/navigate-home)
      (handle-message events/flash-later-show-success {:message "Logged out successfully"})))
  (api/sign-out (get-in app-state keypaths/session-id)
                (stringer/browser-id)
                (get-in app-state-before keypaths/user-id)
                (get-in app-state-before keypaths/user-token)))

(defmethod perform-effects events/browser-fullscreen-exit [_ event args app-state-before app-state]
  (when (= events/navigate-install-home (get-in app-state keypaths/navigation-event))
    (history/enqueue-navigate events/navigate-install-home {:query-params {:video "0"}})))

(defmethod perform-effects events/navigate-voucher [_ event args app-state-before app-state]
  (api/fetch-stylist-service-menu (get-in app-state keypaths/api-cache)
                                  {:user-id    (get-in app-state keypaths/user-id)
                                   :user-token (get-in app-state keypaths/user-token)
                                   :stylist-id (get-in app-state keypaths/store-stylist-id)}))

