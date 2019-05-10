(ns adventure.components.email-capture
  (:require
   [adventure.components.answer-prompt]
   [clojure.string :as string]
   [storefront.accessors.experiments :as experiments]
   [storefront.browser.scroll :as scroll]
   [storefront.component :as component]
   [storefront.components.popup :as popup]
   [storefront.components.svg :as svg]
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

(defmethod popup/query :adv-email-capture
  [data]
  (let [email               (get-in data storefront.keypaths/captured-email)
        errors              (get-in data storefront.keypaths/field-errors)]
    {:input-data   {:value             email
                    :id                "email"
                    :label             "E-mail address"
                    :type              "email"
                    :on-change-keypath storefront.keypaths/captured-email
                    :disabled?         (invalid-email? email)}
     :errors       errors
     :prompt-image "https://ucarecdn.com/03957478-feac-4e0c-aedf-e8e4a7123d69/-/format/auto/"
     :prompt       "Welcome! We can't wait for you to get a free install."
     :mini-prompt  "Enter your e-mail to get started!"
     :header-data  {:header-attrs nil
                    :right-corner {:id    "dismiss-email-capture"
                                   :opts  (utils/fake-href events/control-email-captured-dismiss)
                                   :value (svg/simple-x {:class        "stroke-white"
                                                         :stroke-width "8"
                                                         :style        {:width  "20px"
                                                                        :height "20px"}})}
                    :logo?        true
                    :title        nil
                    :subtitle     nil}
     :on-submit    [events/control-adventure-emailcapture-submit {:email email}]}))

(defmethod popup/component :adv-email-capture
  [queried-data owner _]
  (component/create
   [:div
    (component/build adventure.components.answer-prompt/component queried-data nil)]))

(defmethod effects/perform-effects events/control-adventure-emailcapture-submit
  [_ _ args _ app-state]
  (scroll/enable-body-scrolling)
  (messages/handle-message events/adventure-visitor-identified))

(defmethod transitions/transition-state events/popup-show-adventure-emailcapture
  [_ event args app-state]
  (assoc-in app-state storefront.keypaths/popup :adv-email-capture))

(defmethod transitions/transition-state events/control-adventure-emailcapture-submit
  [_ event args app-state]
  (assoc-in app-state storefront.keypaths/popup nil))

(defmethod trackings/perform-track events/control-adventure-emailcapture-submit
  [_ event {:keys [email]} app-state]
  (facebook-analytics/subscribe)
  (frontend-trackings/track-email-capture-capture app-state {:email email}))

(defmethod trackings/perform-track events/popup-show-adventure-emailcapture
  [_ event args app-state]
  (frontend-trackings/track-email-capture-deploy))
