(ns storefront.components.popup
  (:require [storefront.accessors.nav :as nav]
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
(defmethod component :default [_ _ _] (component/create [:div]))

(defn query-with-popup-type [data]
  (assoc (query data)
         :popup-type (get-in data keypaths/popup :default)))

(defn built-component
  [data _]
  (component/build component
                   (query-with-popup-type data)
                   {:opts {:close-attrs (utils/fake-href events/control-popup-hide)}}))

(defn determine-site
  [app-state]
  (cond
    (= "mayvenn-classic" (get-in app-state keypaths/store-experience)) :classic
    (= "aladdin" (get-in app-state keypaths/store-experience))         :aladdin
    (= "shop" (get-in app-state keypaths/store-slug))                  :shop))

(defmethod effects/perform-effects events/determine-and-show-popup
  [_ _ args _ app-state]
  (let [navigation-event    (get-in app-state keypaths/navigation-event)
        signed-in?          (get-in app-state keypaths/user-id)
        generally-showable? (not (or signed-in?
                                     (nav/show-minimal-footer? navigation-event)
                                     (routes/sub-page? [navigation-event]
                                                       [events/navigate-design-system])))
        email-capture-state (email-capture-session app-state)]
    (when signed-in?
      ;; TODO: This probably belongs in navigate or auth success?
      (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "signed-in"))

    ;; Resist the urge to merge, it is better to explicitly enumerate the sites
    (case (determine-site app-state)
      :classic
      (when (and generally-showable? (nil? email-capture-state))
        (messages/handle-message events/popup-show-email-capture))

      :aladdin
      (when (and generally-showable? (nil? email-capture-state))
        (messages/handle-message events/popup-show-email-capture))

      :shop
      (let [dismissed-pick-a-stylist-email-capture?
            (get-in app-state keypaths/dismissed-pick-a-stylist-email-capture)

            pick-a-stylist-page?
            (routes/sub-page? [navigation-event] [events/navigate-adventure])]

        ;; Caveat, show pick-a-stylist capture after regular capture
        ;; however, never show regular capture after pick-a-stylist capture (i.e. direct load)
        (when (and generally-showable? (not dismissed-pick-a-stylist-email-capture?))
          (cond
            ;; pick-a-stylist
            (and pick-a-stylist-page?
                 (not= "opted-in" email-capture-state))
            (messages/handle-message events/popup-show-pick-a-stylist-email-capture)

            ;; Standard
            (nil? email-capture-state)
            (messages/handle-message events/popup-show-email-capture))))
      nil)))

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
