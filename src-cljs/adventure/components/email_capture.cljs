(ns adventure.components.email-capture
  (:require [adventure.components.answer-prompt]
            [clojure.string :as string]
            [storefront.accessors.experiments :as experiments]
            [storefront.browser.scroll :as scroll]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.frontend-trackings :as frontend-trackings]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.keypaths :as storefront.keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]))

(defn ^:private invalid-email? [email]
  (not (and (seq email)
            (< 3 (count email))
            (string/includes? email "@")
            (not (string/ends-with? email "@")))))

(defmethod popup/query :pick-a-stylist-email-capture
  [data]
  (let [email  (get-in data storefront.keypaths/captured-email)
        errors (get-in data storefront.keypaths/field-errors)]
    {:input-data           {:value             email
                            :id                "email"
                            :label             "Enter your email address"
                            :type              "email"
                            :on-change-keypath storefront.keypaths/captured-email
                            :disabled?         (invalid-email? email)}
     :errors               errors
     :prompt               [:div
                            "Welcome!" [:br]
                            "We can't wait for you to get a free install"]
     :title-image-uuid     "57fde383-01eb-494f-96e1-7070b8fc434d"
     :title-image-alt      "Mayvenn"
     :header-data          {:right-corner {:id    "dismiss-email-capture"
                                           :opts  (utils/fake-href events/control-pick-a-stylist-email-capture-dismiss)
                                           :value (svg/x-sharp {:class "black"
                                                                :style {:width  "18px"
                                                                        :height "18px"
                                                                        :fill   "currentColor"}})}}
     :on-submit            [events/control-pick-a-stylist-email-capture-submit {:email email}]}))

(defmethod popup/component :pick-a-stylist-email-capture
  [queried-data owner _]
  [:div (component/build adventure.components.answer-prompt/component queried-data nil)])

(defmethod effects/perform-effects events/control-pick-a-stylist-email-capture-submit
  [_ _ args _ app-state]
  (scroll/enable-body-scrolling)
  (messages/handle-message events/adventure-visitor-identified))

(defmethod effects/perform-effects events/control-pick-a-stylist-email-capture-dismiss
  [_ _ args _ app-state]
  (scroll/enable-body-scrolling)
  (cookie-jar/save-dismissed-pick-a-stylist-email-capture (get-in app-state storefront.keypaths/cookie)))

(defmethod transitions/transition-state events/control-pick-a-stylist-email-capture-dismiss
  [_ event args app-state]
  (-> app-state
      (assoc-in storefront.keypaths/dismissed-pick-a-stylist-email-capture "1")
      (assoc-in storefront.keypaths/popup nil)))

(defmethod transitions/transition-state events/popup-show-pick-a-stylist-email-capture
  [_ event args app-state]
  (assoc-in app-state storefront.keypaths/popup :pick-a-stylist-email-capture))

(defmethod transitions/transition-state events/control-pick-a-stylist-email-capture-submit
  [_ event args app-state]
  (assoc-in app-state storefront.keypaths/popup nil))

(defmethod trackings/perform-track events/control-pick-a-stylist-email-capture-submit
  [_ event {:keys [email]} app-state]
  (facebook-analytics/subscribe)
  (frontend-trackings/track-email-capture-capture app-state {:email email}))

(defmethod trackings/perform-track events/popup-show-pick-a-stylist-email-capture
  [_ event args app-state]
  (frontend-trackings/track-email-capture-deploy))
