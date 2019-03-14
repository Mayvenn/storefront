(ns adventure.components.email-capture
  (:require
   [om.core :as om]
   [storefront.effects :as effects]
   [storefront.hooks.places-autocomplete :as places-autocomplete]
   [storefront.hooks.facebook-analytics :as facebook-analytics]
   [storefront.browser.cookie-jar :as cookie-jar]
   [storefront.history :as history]
   [storefront.frontend-trackings :as frontend-trackings]
   [storefront.hooks.stringer :as stringer]
   [sablono.core :as sablono]
   [storefront.components.popup :as popup]
   [clojure.string :as string]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [adventure.components.header :as header]
   [adventure.keypaths :as keypaths]
   [storefront.keypaths :as storefront.keypaths]
   [storefront.component :as component]
   [storefront.events :as events]
   [storefront.components.ui :as ui]
   [storefront.transitions :as transitions]
   [storefront.effects :as effects]
   [storefront.trackings :as trackings]
   [adventure.progress :as progress]))

(defmethod popup/query :adv-email-capture
  [data]
  (let [email               (get-in data storefront.keypaths/captured-email)
        errors              (get-in data storefront.keypaths/field-errors)
        session-identified? (= "opted-in" (get-in data storefront.keypaths/email-capture-session))]
    {:email      email
     :errors     errors
     :display?   (not session-identified?)}))

(defn ^:private handle-on-change [^js/Event e]
  (messages/handle-message events/control-change-state
                           {:keypath storefront.keypaths/captured-email
                            :value   (.. e -target -value)}))

(defn ^:private valid-email? [email]
  (and (seq email)
       (< 3 (count email))
       (string/includes? email "@")
       (not (string/ends-with? email "@"))))

(defmethod popup/component :adv-email-capture
  [{:keys [email errors display?]} owner _]
  (component/create
   [:div
    (when display?
      [:div.z5.fixed.overlay
       [:div.bg-lavender.white.center.flex.flex-auto.flex-column
        {:style {:background-image    (str "url(https://ucarecdn.com/03957478-feac-4e0c-aedf-e8e4a7123d69/)")
                 :background-position "bottom"
                 :background-repeat   "no-repeat"
                 :background-size     "contain"}}
        [:div.flex.flex-column.items-center
         {:style {:min-height "100vh"}}
         [:div.flex.items-center.center.mt3.pb3
          [:div.mr4
           (ui/ucare-img {:width "140"} "1970d88b-3798-4914-8a91-74288b09cc77")]]
         [:div.pt5
          [:div.h3.medium.mb2.col-8.mx-auto "Welcome! We can't wait for you to get a free install."]
          [:div.h5.light.mb2.col-8.mx-auto "Enter your e-mail to get started!"]
          [:div.col-12.mx-auto
           [:form.block.flex.justify-center {:on-submit
                                             (utils/send-event-callback events/control-adventure-emailcapture-submit {:email email})}
            [:input.h5.border-none.px3.bg-white.col-9
             {:key         "email"
              :label       "e-mail address"
              :data-test   "email-input"
              :name        "email"
              :id          "email-input"
              :type        "email"
              :value       (or email "")
              :autoFocus   true
              :required    true
              :placeholder "e-mail address"
              :on-change   handle-on-change}]
            (let [disabled? (not (valid-email? email))]
              [:button
               {:type      "submit"
                :disabled  (boolean disabled?)
                :style     {:width  "45px"
                            :height "43px"}
                :class     (ui/button-class :color/teal (merge {:class "flex items-center justify-center not-rounded x-group-item"}
                                                               (when disabled?
                                                                 {:disabled?      disabled?
                                                                  :disabled-class "bg-gray"})))
                :data-test "email-capture-submit"}
               (ui/forward-arrow {:width     "14"
                                  :disabled? disabled?})])]]]]]])]))

(defmethod effects/perform-effects events/control-adventure-emailcapture-submit [_ _ {:keys [email]} _ app-state]
  (facebook-analytics/subscribe)
  (messages/handle-message events/adventure-visitor-identified))

(defmethod transitions/transition-state events/popup-show-adventure-emailcapture [_ event args app-state]
  (assoc-in app-state storefront.keypaths/popup :adv-email-capture))

(defmethod trackings/perform-track events/control-adventure-emailcapture-submit
  [_ event {:keys [email]} app-state]
  (frontend-trackings/track-email-capture-capture app-state {:email email}))

(defmethod trackings/perform-track events/popup-show-adventure-emailcapture
  [_ event args app-state]
  (frontend-trackings/track-email-capture-deploy))
