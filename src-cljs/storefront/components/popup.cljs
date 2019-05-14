(ns storefront.components.popup
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.component :as component]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.browser.scroll :as scroll]
            [storefront.routes :as routes]))

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
        v2-experience?              (experiments/v2-experience? app-state)
        on-non-minimal-footer-page? (not (nav/show-minimal-footer? navigation-event))
        freeinstall-store?          (= "freeinstall" (get-in app-state keypaths/store-slug))
        seen-email-capture?         (email-capture-session app-state)
        seen-freeinstall-offer?     (get-in app-state keypaths/dismissed-free-install)
        seen-to-adventure-modal?    (get-in app-state keypaths/dismissed-to-adventure)
        signed-in?                  (get-in app-state keypaths/user-id)
        classic-experience?         (not v2-experience?)
        on-shop?                    (= "shop" (get-in app-state keypaths/store-slug))
        on-homepage?                (= events/navigate-home navigation-event)
        nav-history-length          (count (get-in app-state keypaths/navigation-undo-stack))
        email-capture-showable?     (and (not signed-in?)
                                         (not seen-email-capture?)
                                         on-non-minimal-footer-page?
                                         (not (and on-shop? on-homepage?))
                                         (or
                                          (not (experiments/to-adventure-modal? app-state))
                                          (not on-shop?)
                                          (and (not on-homepage?)
                                               (= 1 nav-history-length))))
        to-adventure-showable?      (and (experiments/to-adventure-modal? app-state)
                                         (not seen-to-adventure-modal?)
                                         on-shop?
                                         (not on-homepage?)
                                         (zero? nav-history-length)
                                         on-non-minimal-footer-page?)]
    (cond
      to-adventure-showable?
      (messages/handle-message events/popup-show-to-adventure)

      (and freeinstall-store?
           email-capture-showable?
           (contains? #{events/navigate-adventure-install-type}
                      navigation-event))
      (messages/handle-message events/popup-show-adventure-emailcapture)

      signed-in?
      (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "signed-in")

      (and email-capture-showable?
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
