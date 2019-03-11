(ns storefront.components.popup
  (:require [adventure.components.email-capture :as adv.email-capture]
            [adventure.components.program-details-popup :as adventure-program-details]
            [storefront.accessors.nav :as nav]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.component :as component]
            [storefront.components.email-capture :as email-capture]
            [storefront.components.share-your-cart :as share-your-cart]
            [storefront.components.v2-homepage-popup :as v2-homepage-popup]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.experiments :as experiments]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(def popup-type->popups
  {:adv-email-capture      {:query     adv.email-capture/query
                            :component adv.email-capture/component}
   :adventure-free-install {:query     adventure-program-details/query
                            :component adventure-program-details/component}
   :v2-homepage            {:query     v2-homepage-popup/query
                            :component v2-homepage-popup/component}
   :email-capture          {:query     email-capture/query
                            :component email-capture/component}
   :share-cart             {:query     share-your-cart/query
                            :component share-your-cart/component}})

(defn query [data]
  (let [popup-type (get-in data keypaths/popup)
        query      (or (some-> popup-type popup-type->popups :query)
                       (constantly nil))]
    {:popup-type popup-type
     :popup-data (query data)}))

(defn built-component [{:keys [popup-type popup-data]} _]
  (let [opts {:opts {:close-attrs (utils/fake-href events/control-popup-hide)}}]
    (some-> popup-type popup-type->popups :component (component/build popup-data opts))))

(defmethod effects/perform-effects events/determine-and-show-popup
  [_ event args previous-app-state app-state]
  (let [navigation-event            (get-in app-state keypaths/navigation-event)
        v2-experience?              (experiments/v2-experience? app-state)
        on-non-minimal-footer-page? (not (nav/show-minimal-footer? navigation-event))
        is-adventure?               (routes/sub-page? (get-in app-state keypaths/navigation-message)
                                                      [events/navigate-adventure])
        seen-email-capture?         (email-capture-session app-state)
        seen-freeinstall-offer?     (get-in app-state keypaths/dismissed-free-install)
        signed-in?                  (get-in app-state keypaths/user-id)
        classic-experience?         (not v2-experience?)
        email-capture-showable?     (and (not signed-in?)
                                         (not seen-email-capture?)
                                         on-non-minimal-footer-page?)]
    (cond
      (and is-adventure?
           email-capture-showable?
           (contains? #{events/navigate-adventure-what-next}
                      navigation-event))
      (handle-message events/popup-show-adventure-emailcapture)

      signed-in?
      (cookie-jar/save-email-capture-session (get-in app-state keypaths/cookie) "signed-in")

      (and email-capture-showable?
           (not is-adventure?)
           (or seen-freeinstall-offer?
               classic-experience?
               v2-experience?))
      (handle-message events/popup-show-email-capture))))

(defmethod transition-state events/control-popup-hide
  [_ event args app-state]
  (-> app-state
      clear-flash
      (assoc-in keypaths/popup nil)))
