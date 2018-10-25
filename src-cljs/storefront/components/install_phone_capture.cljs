(ns storefront.components.install-phone-capture
  (:require [i18n.phonenumbers.PhoneNumberFormat :as phone-format]
            [i18n.phonenumbers.PhoneNumberUtil :as phone]
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.hooks.stringer :as stringer]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.transitions :as transitions]))

(def phone-utils (phone/getInstance))

(defn ->e164-phone [^String phone]
  (try
    (let [parsed (.parse phone-utils phone "US")]
      (when (.isValidNumber phone-utils parsed)
        (.format phone-utils parsed phone-format/E164)))
    (catch :default _
      nil)))

(defn message-sent-confirmation [close-attrs]
  [:div.bg-black.white.px4.py8.center
   [:div.mx6
    [:div.h2.bold "Text Message Sent"]
    [:div.h6.medium.py2.line-height-3
     "Tap the link to take a short survey."
     " Qualified participants will be contacted"
     " by phone and given a $25 Amazon gift card. "]]
   [:a.teal.bold close-attrs "Okay"]] )

(defn component [{:keys [phone-captured? captured-install-phone field-errors]} owner {:keys [close-attrs]}]
  (om/component
    (html
      (ui/modal {:col-class   "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt"
                 :bg-class    "bg-darken-1"
                 :close-attrs close-attrs}
                [:div.fixed.z4.bottom-0.left-0.right-0
                 (if phone-captured?
                   (message-sent-confirmation close-attrs)
                   [:div.border-top.border-dark-gray.border-width-2
                    [:div.border-top.border-dark-gray.border-width-2
                     [:a.h2.bold.white.bg-purple.px3.py2.flex.items-center
                      (utils/fake-href events/control-popup-hide)
                      [:div.flex-auto.center "Get $25 – Share Your Opinion"]
                      [:div.stroke-white.ml1
                       (svg/dropdown-arrow {:height "16" :width "16"})]]]
                    [:div.bg-light-gray.px3.py4
                     [:div.h6.mb3.mx1.line-height-3
                      "Enter your phone number to apply. Selected participants will be "
                      "interviewed by phone and sent $25."]
                     (let [valid-phone? (boolean (->e164-phone captured-install-phone))]
                       (ui/input-group
                         {:keypath       keypaths/captured-install-phone
                          :wrapper-class "col-8 flex bg-white pl3 items-center circled-item"
                          :class         ""
                          :data-test     ""
                          :name          "phone"
                          :focused       true
                          :placeholder   "(xxx) xxx - xxxx"
                          :type          "tel"
                          :value         captured-install-phone
                          :errors        (when (and (seq captured-install-phone) (not valid-phone?))
                                           [{:long-message "Please enter a valid US phone number"}])}
                         {:ui-element ui/teal-button
                          :content    "Get Survey"
                          :args       {:on-click       (utils/send-event-callback events/control-install-phone-captured-submit {:phone-number captured-install-phone})
                                       :class          "flex justify-center medium items-center circled-item"
                                       :size-class     "col-4"
                                       :height-class   "py2"
                                       :data-test      ""
                                       :disabled-class "disabled bg-teal"
                                       :disabled?      (not valid-phone?)}}))]])]))))

(defn query
  [data]
  {:field-errors           (get-in data keypaths/field-errors)
   :captured-install-phone (get-in data keypaths/captured-install-phone)
   :phone-captured?        (get-in data keypaths/phone-capture-session)})

(defmethod effects/perform-effects events/control-install-phone-captured-submit
  [_ _ {:keys [phone-number]} _ app-state]
  (stringer/track-event "phone_captured" {:phone-number phone-number}
                        events/phone-capture-tracked))

(defmethod effects/perform-effects events/phone-capture-tracked
  [_ _ _ _ app-state]
  (cookie-jar/save-phone-capture-session (get-in app-state keypaths/cookie)))

(defmethod transitions/transition-state events/phone-capture-tracked
  [_ _ _ app-state]
  (assoc-in app-state keypaths/phone-capture-session true))
