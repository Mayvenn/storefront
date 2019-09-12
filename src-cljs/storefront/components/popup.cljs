(ns storefront.components.popup
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [storefront.transitions :as transitions]))

(defn email-capture-session
  [app-state]
  (-> (get-in app-state keypaths/cookie)
      cookie-jar/retrieve-email-capture-session))

(defn touch-email-capture-session
  [app-state]
  (when-let [value (email-capture-session app-state)]
    (-> (get-in app-state keypaths/cookie)
        (cookie-jar/save-email-capture-session value))))

(defmulti query (fn [data] (get-in data keypaths/popup :default)))
(defmethod query :default [_] nil)

(defmulti component (fn [query-data _ _] (:popup-type query-data)))
(defmethod component :default [_ _ _] (component/create nil))

(defn query-with-popup-type [data]
  (assoc (query data)
         :popup-type (get-in data keypaths/popup :default)))

(defn built-component
  [data _]
  (component/build component
                   (query-with-popup-type data)
                   {:opts {:close-attrs (utils/fake-href events/control-popup-hide)}}))

(defmethod effects/perform-effects events/determine-and-show-popup
  [_ _ args _ app-state]
  (let [navigation-event            (get-in app-state keypaths/navigation-event)
        v2-experience?              (experiments/aladdin-experience? app-state)
        on-minimal-footer-page?     (nav/show-minimal-footer? navigation-event)
        freeinstall-store?          (= "freeinstall" (get-in app-state keypaths/store-slug))
        shop?                       (= "shop" (get-in app-state keypaths/store-slug))
        email-capture-state         (email-capture-session app-state)
        seen-freeinstall-offer?     (get-in app-state keypaths/dismissed-free-install)
        signed-in?                  (get-in app-state keypaths/user-id)
        classic-experience?         (not v2-experience?)

        dismissed-pick-a-stylist-email-capture? (get-in app-state keypaths/dismissed-pick-a-stylist-email-capture)
        pick-a-stylist-page?                    (and (routes/sub-page? [navigation-event] [events/navigate-adventure])
                                                     ;; Don't show on post purchase pages
                                                     (not (contains?
                                                           #{events/navigate-adventure-matching-stylist-wait-post-purchase
                                                             events/navigate-adventure-stylist-results-post-purchase
                                                             events/navigate-adventure-match-success-post-purchase
                                                             events/control-adventure-select-stylist-post-purchase}
                                                           navigation-event)))]
    (cond
      ;; never show popup for style guide
      (routes/sub-page? [navigation-event] [events/navigate-design-system])
      nil

      ;; TODO: This probably belongs in navigate or auth success?
      signed-in?
      (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "signed-in")

      ;; pick-a-stylist
      (and (not signed-in?)
           (not dismissed-pick-a-stylist-email-capture?)
           (not= "opted-in" email-capture-state)
           (not on-minimal-footer-page?)
           shop?
           pick-a-stylist-page?)
      (messages/handle-message events/popup-show-pick-a-stylist-email-capture)

      ;; Standard
      (and (not signed-in?)
           (not dismissed-pick-a-stylist-email-capture?)
           (nil? email-capture-state)
           (not on-minimal-footer-page?)
           (not freeinstall-store?)
           (or seen-freeinstall-offer?
               classic-experience?
               v2-experience?))
      (messages/handle-message events/popup-show-email-capture))))

(defmethod effects/perform-effects events/control-popup-hide
  [_ _ _ _ _]
  (messages/handle-message events/popup-hide))

(defmethod transitions/transition-state events/popup-hide
  [_ _ args app-state]
  (-> app-state
      transitions/clear-flash
      (assoc-in keypaths/popup nil)))

(defmethod effects/perform-effects events/popup-hide
  [_ _ _ _ _]
  (scroll/enable-body-scrolling))

(defmethod effects/perform-effects events/popup-show
  [_ _ _ _ _]
  (scroll/disable-body-scrolling))
