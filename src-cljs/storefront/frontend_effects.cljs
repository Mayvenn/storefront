(ns storefront.frontend-effects
  (:require [ajax.core :as ajax]
            [storefront.accessors.contentful :as contentful]
            [clojure.set :as set]
            [clojure.string :as string]
            [spice.maps :as maps]
            [lambdaisland.uri :as uri]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.events :as browser-events]
            [storefront.browser.scroll :as scroll]
            [storefront.community :as community]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.ugc :as ugc]
            [storefront.hooks.convert :as convert]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.hooks.facebook :as facebook]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.lucky-orange :as lucky-orange]
            [storefront.hooks.pinterest :as pinterest]
            [storefront.hooks.pixlee :as pixlee]
            [storefront.hooks.google-maps :as google-maps]
            [storefront.hooks.reviews :as reviews]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.quadpay :as quadpay]
            [storefront.hooks.seo :as seo]
            [storefront.hooks.stringer :as stringer]
            [storefront.hooks.stripe :as stripe]
            [storefront.hooks.svg :as svg]
            [storefront.hooks.talkable :as talkable]
            [storefront.hooks.twitter-analytics :as twitter-analytics]
            [storefront.hooks.uploadcare :as uploadcare]
            [storefront.hooks.spreedly :as spreedly]
            [storefront.hooks.wistia :as wistia]
            [storefront.keypaths :as keypaths]
            [adventure.keypaths :as adv-keypaths]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [storefront.components.share-links :as share-links]
            [storefront.components.popup :as popup]
            [spice.core :as spice]))

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

(defn scroll-promo-field-to-top []
  ;; In a timeout so that changes to the advertised promo aren't changing the scroll too.
  (js/setTimeout #(scroll/scroll-selector-to-top "[data-ref=promo-code]") 0))

(defmethod effects/perform-effects events/app-start [dispatch event args _ app-state]
  (let [choices (-> (get-in app-state keypaths/cookie)
                    cookie-jar/retrieve-adventure
                    :choices
                    js/decodeURIComponent
                    js/JSON.parse
                    (js->clj :keywordize-keys true))]
    (messages/handle-message events/control-adventure-choice {:choice {:value choices}}))
  (quadpay/insert)
  (svg/insert-sprite)
  (stringer/insert-tracking (get-in app-state keypaths/store-slug))
  (google-analytics/insert-tracking)
  (convert/insert-tracking)
  (riskified/insert-tracking (get-in app-state keypaths/session-id))
  (facebook-analytics/insert-tracking)
  (twitter-analytics/insert-tracking)
  (pinterest/insert-tracking)
  (talkable/insert)
  (refresh-account app-state)
  (browser-events/attach-global-listeners)
  (browser-events/attach-esc-key-listener)
  (browser-events/attach-capture-late-readystatechange-callbacks)
  (lucky-orange/track-store-experience (get-in app-state keypaths/store-experience))
  (when-let [stringer-distinct-id (cookie-jar/get-stringer-distinct-id (get-in app-state keypaths/cookie))]
    (messages/handle-message events/stringer-distinct-id-available {:stringer-distinct-id stringer-distinct-id}))
  (doseq [feature (get-in app-state keypaths/features)]
    ;; trigger GA analytics, even though feature is already enabled
    (messages/handle-message events/enable-feature {:feature feature})))

(defmethod effects/perform-effects events/app-stop [_ event args _ app-state]
  (convert/remove-tracking)
  (riskified/remove-tracking)
  (stringer/remove-tracking)
  (google-analytics/remove-tracking)
  (facebook-analytics/remove-tracking)
  (twitter-analytics/remove-tracking)
  (pinterest/remove-tracking)
  (lucky-orange/remove-tracking)
  (pixlee/remove-tracking)
  (browser-events/unattach-capture-late-readystatechange-callbacks)
  (browser-events/detach-esc-key-listener))

(defmethod effects/perform-effects events/enable-feature [_ event {:keys [feature]} _ app-state]
  (messages/handle-message events/determine-and-show-popup))

(def popup-dismiss-events
  {:email-capture-quadpay  events/control-email-captured-dismiss
   :email-capture          events/control-email-captured-dismiss
   :adv-email-capture      events/control-email-captured-dismiss
   :adventure-free-install events/control-adventure-free-install-dismiss
   :v2-homepage            events/control-v2-homepage-popup-dismiss
   :share-cart             events/control-popup-hide
   :design-system          events/control-design-system-popup-dismiss})

(defmethod effects/perform-effects events/escape-key-pressed [_ event args _ app-state]
  (when-let [message-to-handle (get popup-dismiss-events (get-in app-state keypaths/popup))]
    (messages/handle-message message-to-handle)))

(defmethod effects/perform-effects events/ensure-sku-ids
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

(defmethod effects/perform-effects events/external-redirect-welcome [_ event args _ app-state]
  (set! (.-location js/window) (get-in app-state keypaths/welcome-url)))

(defmethod effects/perform-effects events/external-redirect-freeinstall
  [_ event {:keys [query-string path]} _ app-state]
  (cookie-jar/save-from-shop-to-freeinstall (get-in app-state keypaths/cookie))
  (let [on-homepage? (= events/navigate-home
                        (get-in app-state keypaths/navigation-event))
        host         (if (experiments/adventure-on-shop? app-state)
                       "shop."
                       "freeinstall.")]
    (set! (.-location js/window)
          (-> (.-location js/window)
              uri/uri
              (assoc :host (str host (routes/environment->hostname (get-in app-state keypaths/environment)))
                     :path (or path
                               (if on-homepage? "/adv/match-stylist" "/"))
                     :query query-string)
              str))))

(defmethod effects/perform-effects events/initiate-redirect-freeinstall-from-menu
  [_ event {:keys [utm-source]} _ app-state]
  (messages/handle-message events/external-redirect-freeinstall
                  {:query-string (string/join "&"
                                              ["utm_campaign=ShoptoFreeInstall"
                                               "utm_medium=referral"
                                               (str "utm_source=" utm-source)])}))

(defmethod effects/perform-effects events/external-redirect-sms [_ event {:keys [sms-message number]} _ app-state]
  (set! (.-location js/window) (share-links/sms-link sms-message number)))

(defmethod effects/perform-effects events/external-redirect-paypal-setup [_ event args _ app-state]
  (set! (.-location js/window) (get-in app-state keypaths/order-cart-payments-paypal-redirect-url)))

(defmethod effects/perform-effects events/external-redirect-quadpay-checkout [_ event {:keys [quadpay-redirect-url]} _ app-state]
  (set! (.-location js/window) quadpay-redirect-url))

(defmethod effects/perform-effects events/control-navigate [_ event {:keys [navigation-message]} _ app-state]
  ;; A user has clicked a link
  ;; The URL has already changed. Save scroll position on the page they are
  ;; leaving, and handle the nav message.
  (messages/handle-message events/navigation-save (-> (get-in app-state keypaths/navigation-stashed-stack-item)
                                                      (assoc :final-scroll js/document.body.scrollTop)))
  (apply messages/handle-message navigation-message))

(defmethod effects/perform-effects events/browser-navigate [_ _ {:keys [navigation-message]} _ app-state]
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
        (messages/handle-message events/navigation-undo leaving-stack-item)
        (apply messages/handle-message (assoc-in navigation-message [1 :nav-stack-item] back)))

      (:navigation-message forward)
      (do
        (messages/handle-message events/navigation-redo leaving-stack-item)
        (apply messages/handle-message (assoc-in navigation-message [1 :nav-stack-item] forward)))

      (apply messages/handle-message navigation-message))))

(defmethod effects/perform-effects events/redirect [_ event {:keys [nav-message]} _ app-state]
  (let [[event args] nav-message]
    (history/enqueue-redirect event args)))

(defn add-pending-promo-code [app-state {:keys [number token]}]
  (when-let [pending-promo-code (get-in app-state keypaths/pending-promo-code)]
    (api/add-promotion-code {:shop?              (= "shop" (get-in app-state keypaths/store-slug))
                             :session-id         (get-in app-state keypaths/session-id)
                             :number             number
                             :token              token
                             :promo-code         pending-promo-code
                             :allow-dormant?     true
                             :consolidated-cart? (experiments/consolidated-cart? app-state)})))

(defmethod effects/perform-effects events/navigate [_ event {:keys [navigate/caused-by query-params nav-stack-item]} prev-app-state app-state]
  (let [freeinstall? (= "freeinstall" (get-in app-state keypaths/store-slug))]

    (messages/handle-message events/control-menu-collapse-all)
    (messages/handle-message events/save-order {:order (get-in app-state keypaths/order)})

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
          (messages/handle-later events/snap {:top restore-scroll-top} 100))))

    (when-not freeinstall?
      (when-let [pending-promo-code (:sha query-params)]
        (cookie-jar/save-pending-promo-code
         (get-in app-state keypaths/cookie)
         pending-promo-code)))

    (when-let [affiliate-stylist-id (:affiliate_stylist_id query-params)]
      (cookie-jar/save-affiliate-stylist-id (get-in app-state keypaths/cookie)
                                            {:affiliate-stylist-id affiliate-stylist-id}))

    (messages/handle-message events/determine-and-show-popup)

    (let [utm-params (some-> query-params
                             (select-keys [:utm_source :utm_medium :utm_campaign :utm_content :utm_term])
                             (set/rename-keys {:utm_source   :storefront/utm-source
                                               :utm_medium   :storefront/utm-medium
                                               :utm_campaign :storefront/utm-campaign
                                               :utm_content  :storefront/utm-content
                                               :utm_term     :storefront/utm-term})
                             (maps/deep-remove-nils))]
      (when (seq utm-params)
        (cookie-jar/save-utm-params
         (get-in app-state keypaths/cookie)
         utm-params)))

    (when (and (get-in app-state keypaths/user-must-set-password)
               (not= event events/navigate-force-set-password))
      (effects/redirect events/navigate-force-set-password))

    (when-not (= caused-by :module-load)
      (when (get-in app-state keypaths/popup)
        (messages/handle-message events/popup-hide))

      (quadpay/hide-modal))

    (exception-handler/refresh)

    (popup/touch-email-capture-session app-state)))

(defmethod effects/perform-effects events/navigate-home [_ _ _ _ app-state]
  (api/fetch-cms-data))

(defmethod effects/perform-effects events/navigate-content [_ [_ _ & static-content-id :as event] _ _ app-state]
  (when-not (= static-content-id
               (get-in app-state keypaths/static-id))
    (api/get-static-content event)))

(defmethod effects/perform-effects events/navigate-content-about-us [_ _ _ _ app-state]
  (wistia/load))

(defmethod effects/perform-effects events/navigate-shop-by-look
  [dispatch event {:keys [album-keyword]} _ app-state]
  (let [actual-album-kw (ugc/determine-look-album app-state album-keyword)]
    (cond
      (and (#{:wavy-curly-looks :straight-looks} album-keyword)
           (not= (get-in app-state keypaths/store-slug) "shop")
           (not= "aladdin" (get-in app-state keypaths/store-experience)))
      (effects/redirect events/navigate-shop-by-look {:album-keyword :look})

      (and (experiments/aladdin-experience? app-state)
           (= album-keyword :deals))
      (effects/redirect events/navigate-home) ; redirect to home page from /shop/deals for v2-experience

      (= :ugc/unknown-album actual-album-kw)
      (effects/page-not-found))))

(defmethod effects/perform-effects events/navigate-shop-by-look-details [_ event {:keys [album-keyword]} _ app-state]
  (if-let [shared-cart-id (contentful/shared-cart-id (contentful/selected-look app-state))]
    (do
      (reviews/insert-reviews)
      (api/fetch-shared-cart shared-cart-id))
    (effects/redirect events/navigate-shop-by-look {:album-keyword album-keyword})))

(defmethod effects/perform-effects events/navigate-account [_ event args _ app-state]
  (when-not (get-in app-state keypaths/user-token)
    (effects/redirect events/navigate-sign-in)))

(defmethod effects/perform-effects events/navigate-stylist [_ event args _ app-state]
  (when (not (and (get-in app-state keypaths/user-token)
                (get-in app-state keypaths/user-store-id)))
    (effects/redirect events/navigate-sign-in)))

(defmethod effects/perform-effects events/navigate-gallery [_ event args _ app-state]
  (api/get-gallery (if (stylists/own-store? app-state)
                     {:user-id (get-in app-state keypaths/user-id)
                      :user-token (get-in app-state keypaths/user-token)}
                     {:stylist-id (get-in app-state keypaths/store-stylist-id)})))

(defmethod effects/perform-effects events/navigate-gallery-image-picker [_ event args _ app-state]
  (if (stylists/own-store? app-state)
    (uploadcare/insert)
    (effects/redirect events/navigate-gallery)))

(defmethod effects/perform-effects events/control-delete-gallery-image [_ event {:keys [image-url]} _ app-state]
  (api/delete-gallery-image (get-in app-state keypaths/user-id)
                            (get-in app-state keypaths/user-token)
                            image-url))

(defmethod effects/perform-effects events/api-success-gallery [_ event args _ app-state]
  (cond
    (not (stylists/gallery? app-state))
    (effects/page-not-found)

    (and (stylists/own-store? app-state)
         (routes/exact-page? (get-in app-state keypaths/navigation-message) [events/navigate-gallery])
         (some (comp #{"pending"} :status) (get-in app-state keypaths/store-gallery-images)))
    (messages/handle-later events/poll-gallery {} 5000)))

(defmethod effects/perform-effects events/control [_ _ args _ app-state]
  (popup/touch-email-capture-session app-state))

(defmethod effects/perform-effects events/control-email-captured-submit [_ _ args _ app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "opted-in")))

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
  {"quadpay"                     "There was an issue authorizing your QuadPay payment. Please check out again or use a different payment method."
   "paypal-incomplete"           "We were unable to complete your order with PayPal. Please try again."
   "paypal-invalid-address"      (str "Unfortunately, Mayvenn products cannot be delivered to this address at this time. "
                                      "Please choose a new shipping destination. ")
   "ineligible-for-free-install" (str "The 'FreeInstall' promotion code has been removed from your order."
                                      " Please visit freehairinstall.com to complete your order.")})

(defmethod effects/perform-effects events/navigate-cart [_ event args _ app-state]
  (api/get-shipping-methods)
  (api/get-states (get-in app-state keypaths/api-cache))
  (google-maps/insert) ;; for address screen on the next page
  (stripe/insert)
  (quadpay/insert)
  (refresh-current-order app-state)
  (when-let [error-msg (-> args :query-params :error cart-error-codes)]
    (messages/handle-message events/flash-show-failure {:message error-msg})))

(defn ensure-bucketed-for [app-state experiment]
  (let [already-bucketed? (contains? (get-in app-state keypaths/experiments-bucketed) experiment)]
    (when-not already-bucketed?
      (when-let [variation (experiments/variation-for app-state experiment)]
        (messages/handle-message events/bucketed-for {:experiment experiment :variation variation})
        (messages/handle-message events/enable-feature {:experiment experiment :feature (:feature variation)})))))

(defmethod effects/perform-effects events/navigate-checkout [_ event args _ app-state]
  (google-maps/insert)
  (let [have-cart? (get-in app-state keypaths/order-number)]
    (cond
      (and (not have-cart?)
           (= "freeinstall" (get-in app-state keypaths/store-slug))) (effects/redirect events/navigate-adventure-home)

      (not have-cart?)                                               (effects/redirect events/navigate-cart))

    (when (and have-cart?
               (not (auth/signed-in-or-initiated-guest-checkout? app-state))
               (not (#{events/navigate-checkout-address
                       events/navigate-checkout-returning-or-guest
                       events/navigate-checkout-sign-in
                       events/navigate-checkout-processing} event)))
      (effects/redirect events/navigate-checkout-address))))

(defmethod effects/perform-effects events/navigate-checkout-sign-in [_ event args _ app-state]
  (facebook/insert))

(defmethod effects/perform-effects events/navigate-checkout-returning-or-guest [_ event args _ app-state]
  (google-maps/remove-containers)
  (api/get-states (get-in app-state keypaths/api-cache))
  (facebook/insert))

(defn- fetch-saved-cards [app-state]
  (when-let [user-id (get-in app-state keypaths/user-id)]
    (api/get-saved-cards user-id (get-in app-state keypaths/user-token))))

(defmethod effects/perform-effects events/navigate-checkout-address [_ event args _ app-state]
  (when-not (get-in app-state keypaths/user-id)
    (effects/redirect events/navigate-checkout-returning-or-guest))
  (google-maps/remove-containers)
  (api/get-states (get-in app-state keypaths/api-cache))
  (fetch-saved-cards app-state))

(defmethod effects/perform-effects events/navigate-checkout-payment [dispatch event args _ app-state]
  (when (empty? (get-in app-state keypaths/order-shipping-address))
    (effects/redirect events/navigate-checkout-address))
  (fetch-saved-cards app-state)
  (stripe/insert)
  (quadpay/insert))

(defmethod effects/perform-effects events/navigate-checkout-confirmation [_ event args _ app-state]
  ;; TODO: get the credit card component to function correctly on direct page load
  (when (empty? (get-in app-state keypaths/order-cart-payments))
    (effects/redirect events/navigate-checkout-payment))
  (stripe/insert)
  (api/get-shipping-methods))

(defmethod effects/perform-effects events/navigate-order-complete [_ event {{:keys [paypal order-token]} :query-params number :number} _ app-state]
  (when (not (get-in app-state keypaths/user-id))
    (facebook/insert))
  (when (and number order-token)
    (api/get-completed-order number order-token))
  (when paypal
    (effects/redirect events/navigate-order-complete {:number number}))
  (let [servicing-stylist    (get-in app-state adv-keypaths/adventure-servicing-stylist)
        servicing-stylist-id (get-in app-state adv-keypaths/adventure-choices-selected-stylist-id)]
    (when (and servicing-stylist-id (not servicing-stylist))
      (api/fetch-matched-stylist (get-in app-state keypaths/api-cache) servicing-stylist-id))))

(defmethod effects/perform-effects events/navigate-need-match-order-complete
  [_ event {{:keys [paypal]} :query-params} _ app-state]
  (let [{:keys [number token] :as order} (get-in app-state keypaths/completed-order)]
    (when (not (get-in app-state keypaths/user-id))
      (facebook/insert))
    (when (and number token)
      (api/get-completed-order number token))
    (when (not (get-in app-state adventure.keypaths/adventure-matched-stylists))
      (messages/handle-message events/api-fetch-stylists-within-radius-post-purchase))
    (when paypal
      (effects/redirect events/navigate-need-match-order-complete {:number number}))))

(defmethod effects/perform-effects events/navigate-friend-referrals [_ event args _ app-state]
  (talkable/show-referrals app-state))

(defmethod effects/perform-effects events/navigate-account-referrals [_ event args _ app-state]
  (talkable/show-referrals app-state))

(defmethod effects/perform-effects events/api-success-get-completed-order [_ event order _ app-state]
  (messages/handle-message events/order-completed order))

(defn redirect-to-return-navigation [app-state]
  (apply effects/redirect
         (get-in app-state keypaths/return-navigation-message)))

(defn redirect-when-signed-in [app-state]
  (when (get-in app-state keypaths/user-email)
    (if (get-in app-state keypaths/telligent-community-url)
      (community/redirect-to-telligent-as-user app-state)
      (do
        (redirect-to-return-navigation app-state)
        (messages/handle-message events/flash-later-show-success
                        {:message "You are already signed in."})))))

(defmethod effects/perform-effects events/navigate-sign-in [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))

(defmethod effects/perform-effects events/navigate-sign-out [_ _ {{:keys [telligent-url]} :query-params} _ app-state]
  (if telligent-url
    (do
      (cookie-jar/clear-telligent-session (get-in app-state keypaths/cookie))
      (messages/handle-message events/external-redirect-telligent))
    (messages/handle-message events/sign-out)))
(defmethod effects/perform-effects events/navigate-sign-up [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))
(defmethod effects/perform-effects events/navigate-forgot-password [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))
(defmethod effects/perform-effects events/navigate-reset-password [_ event args _ app-state]
  (facebook/insert)
  (redirect-when-signed-in app-state))

(defmethod effects/perform-effects events/navigate-not-found [_ event args _ app-state]
  (messages/handle-message events/flash-show-failure
                  {:message "The page you were looking for could not be found."}))

(defmethod effects/perform-effects events/control-sign-in-submit [_ event args _ app-state]
  (api/sign-in (get-in app-state keypaths/session-id)
               (stringer/browser-id)
               (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-number)
               (get-in app-state keypaths/order-token)))

(defmethod effects/perform-effects events/control-sign-up-submit [_ event _ _ app-state]
  (let [{:keys [number token]} (or (get-in app-state keypaths/order)
                                   (get-in app-state keypaths/completed-order))]
    (api/sign-up (get-in app-state keypaths/session-id)
                 (stringer/browser-id)
                 (get-in app-state keypaths/sign-up-email)
                 (get-in app-state keypaths/sign-up-password)
                 (get-in app-state keypaths/store-stylist-id)
                 number
                 token)))

(defmethod effects/perform-effects events/control-facebook-sign-in [_ event args _ app-state]
  (facebook/start-log-in app-state))

(defmethod effects/perform-effects events/control-facebook-reset [_ event args _ app-state]
  (facebook/start-reset app-state))

(defmethod effects/perform-effects events/facebook-success-sign-in [_ _ {:keys [authResponse]} _ app-state]
  (let [{:keys [number token]} (or (get-in app-state keypaths/order)
                                   (get-in app-state keypaths/completed-order))]
    (api/facebook-sign-in (get-in app-state keypaths/session-id)
                          (stringer/browser-id)
                          (:userID authResponse)
                          (:accessToken authResponse)
                          (get-in app-state keypaths/store-stylist-id)
                          number
                          token)))

(defmethod effects/perform-effects events/facebook-failure-sign-in [_ _ args _ app-state]
  (messages/handle-message events/flash-show-failure
                  {:message "Could not sign in with Facebook.  Please try again, or sign in with email and password."}))

(defmethod effects/perform-effects events/facebook-email-denied [_ _ args _ app-state]
  (messages/handle-message events/flash-show-failure
                  {:message "We need your Facebook email address to communicate with you about your orders. Please try again."}))

(defn- abort-pending-requests [requests]
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

(defmethod effects/perform-effects events/control-reset-password-submit [_ event args _ app-state]
  (if (empty? (get-in app-state keypaths/reset-password-password))
    (messages/handle-message events/flash-show-failure {:message "Your password cannot be blank."})
    (api/reset-password (get-in app-state keypaths/session-id)
                        (stringer/browser-id)
                        (get-in app-state keypaths/reset-password-password)
                        (get-in app-state keypaths/reset-password-token)
                        (get-in app-state keypaths/order-number)
                        (get-in app-state keypaths/order-token)
                        (get-in app-state keypaths/store-stylist-id))))

(defmethod effects/perform-effects events/facebook-success-reset [_ event facebook-response _ app-state]
  (api/facebook-reset-password (get-in app-state keypaths/session-id)
                               (stringer/browser-id)
                               (-> facebook-response :authResponse :userID)
                               (-> facebook-response :authResponse :accessToken)
                               (get-in app-state keypaths/reset-password-token)
                               (get-in app-state keypaths/order-number)
                               (get-in app-state keypaths/order-token)
                               (get-in app-state keypaths/store-stylist-id)))

(defmethod effects/perform-effects events/uploadcare-api-success-upload-gallery [_ event {:keys [cdnUrl]} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (api/append-stylist-gallery user-id user-token {:gallery-urls [cdnUrl]})
    (history/enqueue-navigate events/navigate-gallery)))

(defmethod effects/perform-effects events/control-checkout-update-addresses-submit [_ event args _ app-state]
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

(defmethod effects/perform-effects events/control-checkout-remove-promotion [_ _ args _ app-state]
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
  [_ _ {:keys [order]} _ app-state]
  (if (and order (orders/incomplete? order))
    (do
      (when-let [sku-ids (->> order orders/product-items (map :sku) seq)]
        (messages/handle-message events/ensure-sku-ids {:sku-ids sku-ids}))
      (cookie-jar/save-order (get-in app-state keypaths/cookie) order)
      (add-pending-promo-code app-state order))
    (messages/handle-message events/clear-order)))

(defmethod effects/perform-effects events/clear-order [_ _ _ _ app-state]
  (cookie-jar/clear-order (get-in app-state keypaths/cookie)))

(defmethod effects/perform-effects events/api-success-auth [_ _ {:keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user))
  (redirect-to-return-navigation app-state))

(defmethod effects/perform-effects events/api-success-auth-sign-in
  [_ _ _ _ app-state]
  (if (get-in app-state keypaths/telligent-community-url)
    (community/redirect-to-telligent-as-user app-state)
    (messages/handle-message events/flash-later-show-success
                    {:message "Logged in successfully"})))

(defmethod effects/perform-effects events/api-success-auth-sign-up [dispatch event args _ app-state]
  (messages/handle-message events/flash-later-show-success {:message "Welcome! You have signed up successfully."}))

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

(defmethod effects/perform-effects events/poll-gallery [_ event args _ app-state]
  (when (stylists/own-store? app-state)
    (api/get-gallery {:user-id    (get-in app-state keypaths/user-id)
                      :user-token (get-in app-state keypaths/user-token)})))

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

(defmethod effects/perform-effects events/api-success-send-stylist-referrals [_ event args _ app-state]
  (messages/handle-later events/popup-hide {} 2000))

(defmethod effects/perform-effects events/api-success-update-order-place-order [_ event {:keys [order]} _ app-state]
  ;; TODO: rather than branching behavior within a single event handler, consider
  ;;       firing seperate events (with and without matching stylists).
  (if (= "freeinstall" (get-in app-state keypaths/store-slug))
    (history/enqueue-navigate events/navigate-adventure-checkout-wait)
    (history/enqueue-navigate events/navigate-order-complete order))
  (messages/handle-message events/order-completed order)
  (messages/handle-message events/order-placed order))

(defmethod effects/perform-effects events/order-completed [dispatch event order _ app-state]
  (cookie-jar/save-completed-order (get-in app-state keypaths/cookie)
                                   (get-in app-state keypaths/completed-order))
  (messages/handle-message events/clear-order)
  (let [freeinstall? (= "freeinstall" (get-in app-state keypaths/store-slug))]
    (when (or (not freeinstall?)
              (:servicing-stylist-id order))
      (talkable/show-pending-offer app-state))

    (when (and freeinstall?
               (:servicing-stylist-id order))
      (api/fetch-matched-stylist (get-in app-state keypaths/api-cache)
                                 (:servicing-stylist-id order)))))

(defmethod effects/perform-effects events/api-success-update-order-update-cart-payments [_ event {:keys [order place-order?]} _ app-state]
  (when place-order?
    (api/place-order (get-in app-state keypaths/session-id)
                     order
                     (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie))
                     (stylists/retrieve-parsed-affiliate-id app-state))))

(defmethod effects/perform-effects events/api-success-update-order [_ event {:keys [order navigate event]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (when event
    (messages/handle-message event {:order order}))
  (when navigate
    (history/enqueue-navigate navigate {:number (:number order)})))

(defmethod effects/perform-effects events/api-success-get-order [_ event order _ app-state]
  (when-let [servicing-stylist-id (:servicing-stylist-id order)]
    (api/fetch-matched-stylist (get-in app-state keypaths/api-cache)
                               servicing-stylist-id))
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
    "stripe-card-failure"         (when (= (get-in app-state keypaths/navigation-event)
                                           events/navigate-checkout-confirmation)
                                    (effects/redirect events/navigate-checkout-payment)
                                    (messages/handle-later events/api-failure-errors errors)
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
                                                               #(messages/handle-message events/api-success-update-order
                                                                                {:order      %
                                                                                 :promo-code "freeinstall"}))
                                    (effects/redirect events/navigate-cart)
                                    (messages/handle-later events/api-failure-errors errors)
                                    (scroll/snap-to-top))
    (scroll/snap-to-top)))

(defmethod effects/perform-effects events/api-success-add-to-bag [dispatch event {:keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (add-pending-promo-code app-state order)
  (messages/handle-later events/added-to-bag))

(defmethod effects/perform-effects events/api-success-remove-from-bag [dispatch event {:keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order}))

(defmethod effects/perform-effects events/api-success-add-sku-to-bag [dispatch event {:keys [order]} _ app-state]
  (messages/handle-message events/save-order {:order order})
  (add-pending-promo-code app-state order)
  (messages/handle-later events/added-to-bag))

(defmethod effects/perform-effects events/added-to-bag [_ _ _ _ app-state]
  (when-let [el (.querySelector js/document "[data-ref=cart-button]")]
    (scroll/scroll-to-elem el)))

(defmethod effects/perform-effects events/reviews-component-mounted [_ event args _ app-state]
  (let [expected-reviews-count 2
        actual-reviews-count   (get-in app-state keypaths/review-components-count)]
    (when (= expected-reviews-count actual-reviews-count)
      (reviews/start))))

(defmethod effects/perform-effects events/checkout-address-component-mounted
  [_ event {:keys [address-elem address-keypath]} _ app-state]
  (google-maps/attach "address" address-elem address-keypath))

(defmethod effects/perform-effects events/api-success-update-order-remove-promotion-code
  [_ _ {:keys [hide-success]} _ app-state]
  (when-not hide-success
    (messages/handle-message events/flash-show-success {:message "The coupon code was successfully removed from your order."
                                               :scroll? false})))

(defmethod effects/perform-effects events/api-success-update-order-add-promotion-code [_ _ {allow-dormant? :allow-dormant?} _ app-state]
  (when-not allow-dormant?
    (messages/handle-message events/flash-show-success {:message "The coupon code was successfully applied to your order."
                                                        :scroll? false})
    (scroll-promo-field-to-top))
  (api/get-promotions (get-in app-state keypaths/api-cache)
                      (first (get-in app-state keypaths/order-promotion-codes))))

(defmethod effects/perform-effects events/inserted-talkable [_ event args _ app-state]
  (talkable/show-pending-offer app-state)
  (let [nav-event (get-in app-state keypaths/navigation-event)]
    (when (#{events/navigate-friend-referrals events/navigate-account-referrals} nav-event)
      (talkable/show-referrals app-state))))

(defmethod effects/perform-effects events/control-email-captured [_ event args _ app-state]
  (scroll/enable-body-scrolling))

(defmethod effects/perform-effects events/control-email-captured-dismiss [_ event args _ app-state]
  (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "dismissed"))

(defmethod effects/perform-effects events/sign-out [_ event args app-state-before app-state]
  (when (not= "opted-in" (popup/email-capture-session app-state))
    (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "dismissed"))
  (messages/handle-message events/clear-order)
  (cookie-jar/clear-account (get-in app-state keypaths/cookie))
  (messages/handle-message events/control-menu-collapse-all)
  (abort-pending-requests (get-in app-state keypaths/api-requests))
  (if (= events/navigate-home (get-in app-state keypaths/navigation-event))
    (messages/handle-message events/flash-show-success {:message "Logged out successfully"})
    (do
      (history/enqueue-navigate events/navigate-home)
      (messages/handle-message events/flash-later-show-success {:message "Logged out successfully"})))
  (api/sign-out (get-in app-state keypaths/session-id)
                (stringer/browser-id)
                (get-in app-state-before keypaths/user-id)
                (get-in app-state-before keypaths/user-token)))

(defmethod effects/perform-effects events/browser-fullscreen-exit [_ event args app-state-before app-state]
  (when (= events/navigate-adventure-home (get-in app-state keypaths/navigation-event))
    (history/enqueue-navigate events/navigate-adventure-home {:query-params {:video "0"}})))

(defmethod effects/perform-effects events/navigate-voucher [_ event args app-state-before app-state]
  (api/fetch-user-stylist-service-menu (get-in app-state keypaths/api-cache)
                                       {:user-id    (get-in app-state keypaths/user-id)
                                        :user-token (get-in app-state keypaths/user-token)
                                        :stylist-id (get-in app-state keypaths/user-store-id)}))

(defmethod effects/perform-effects events/inserted-stringer
  [_ event args app-state-before app-state]
  (messages/handle-message events/stringer-distinct-id-available {:stringer-distinct-id (stringer/browser-id)}))

(defmethod effects/perform-effects events/module-loaded [_ _ {:keys [module-name for-navigation-event]} app-state-before app-state]
  (let [already-loaded-module? (= (get-in app-state-before keypaths/modules)
                                  (get-in app-state keypaths/modules))
        [evt args]             (get-in app-state keypaths/navigation-message)]
    (when (and (not already-loaded-module?)
               (= for-navigation-event evt))
      (messages/handle-message evt (assoc args :navigate/caused-by :module-load)))))
