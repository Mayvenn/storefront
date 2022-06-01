(ns storefront.components.reset-password
  (:require [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.api :as api]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.platform.messages :as messages]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [ui.legal :as legal]))

(defcomponent component [{:keys [focused reset-password show-password? field-errors show-optins? present-tos? copy-type] :as data} owner _]
  (ui/narrow-container
   [:div.p2
    [:h2.center.my2.mb3 "Resetting Your Password"]
    [:form.col-12
     {:on-submit (utils/send-event-callback events/control-reset-password-submit)}

     [:h2.col-12.my1.pt2.proxima.title-3.shout.bold "Enter a new password to access your account"]
     (ui/text-field {:errors       (get field-errors ["password"])
                     :data-test    "reset-password-password"
                     :keypath      keypaths/reset-password-password
                     :focused      focused
                     :label        "New Password"
                     :min-length   6
                     :required     true
                     :type         "password"
                     :autocomplete "new-password"
                     :value        reset-password
                     :hint         (when show-password? reset-password)})
     [:div.col-12.left
      (ui/check-box {:label   "Show password"
                     :keypath keypaths/account-show-password?
                     :focused focused
                     :value   show-password?})]

     (when show-optins?
       (component/build legal/opt-in-section data))

     (when present-tos?
       [:div
        [:h2.col-12.my1.pt2.proxima.title-3.shout.bold
         {:style {:clear "both"}}
         "Terms & Conditions"]
        
        (component/build legal/tos-form-footnote {:copy-type copy-type})])

     [:div.col-12.col-6-on-tb-dt.mx-auto.mt4
      (ui/submit-button (if present-tos?
                          "Agree & Submit"
                          "Save & Continue")
                        {:data-test "reset-password-submit"})]]]))

(defn query [data]
  (let [copy-type (get-in data keypaths/reset-password-copy-type)]
    (merge
     {:reset-password (get-in data keypaths/reset-password-password)
      :show-password? (get-in data keypaths/account-show-password? true)
      :field-errors   (get-in data keypaths/field-errors)
      :focused        (get-in data keypaths/ui-focus)
      :show-optins?   (get-in data keypaths/reset-password-needs-optin-prompts?)
      :present-tos?   (get-in data keypaths/reset-password-present-tos?)
      :copy-type      copy-type}
     (legal/opt-in-query copy-type
                         (get-in data keypaths/reset-password-sms-marketing-optin)
                         (get-in data keypaths/reset-password-sms-transactional-optin)))))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/navigate-reset-password [_ event {:keys [reset-token]} app-state]
  (assoc-in app-state keypaths/reset-password-token reset-token))

(defmethod effects/perform-effects events/navigate-reset-password [_ event {:keys [reset-token]} _ app-state]
  (api/token-requirements (get-in app-state keypaths/session-id)
                          (get-in app-state keypaths/stringer-browser-id)
                          reset-token))

(defmethod effects/perform-effects events/control-reset-password-submit [_ event args _ app-state]
  (if (empty? (get-in app-state keypaths/reset-password-password))
    (messages/handle-message events/flash-show-failure {:message "Your password cannot be blank."})
    (let [wants-sms? (get-in app-state keypaths/reset-password-needs-optin-prompts?)]
      (api/reset-password (get-in app-state keypaths/session-id)
                          (get-in app-state keypaths/stringer-browser-id)
                          (get-in app-state keypaths/reset-password-password)
                          (get-in app-state keypaths/reset-password-token)
                          (get-in app-state keypaths/order-number)
                          (get-in app-state keypaths/order-token)
                          (get-in app-state keypaths/store-stylist-id)
                          (when wants-sms?
                            (boolean (get-in app-state keypaths/reset-password-sms-marketing-optin)))
                          (when wants-sms?
                            (boolean (get-in app-state keypaths/reset-password-sms-transactional-optin)))
                          nil))))

(defmethod transitions/transition-state events/api-success-token-requirements [_ event args app-state]
  (assoc-in app-state keypaths/reset-password-token-requirements args))
