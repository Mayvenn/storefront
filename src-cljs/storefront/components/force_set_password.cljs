(ns storefront.components.force-set-password
  (:require [storefront.api :as api]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [ui.legal :as legal]))

(defcomponent component
  [{:keys [focused password show-password? field-errors copy-type] :as data} owner _]
  (ui/narrow-container
   [:div.p2
    [:h2.center.my2.mb3
     "Just a few more things to set up your account"]
    [:form.col-12
     {:on-submit (utils/send-event-callback events/control-force-set-password-submit)}

     (component/build legal/opt-in-section data)

     [:h2.col-12.my1.pt2.proxima.title-3.shout.bold "Enter a new password to access your account"]

     (ui/text-field {:errors     (get field-errors ["password"])
                     :data-test  "force-set-password-password"
                     :keypath    keypaths/force-set-password-password
                     :focused    focused
                     :label      "New Password"
                     :min-length 6
                     :required   true
                     :type       "password"
                     :value      password
                     :hint       (when show-password? password)})
     [:div.col-12.left
      (ui/check-box {:label   "Show password"
                     :keypath keypaths/account-show-password?
                     :focused focused
                     :value   show-password?})]

     [:div
      [:h2.col-12.my1.pt2.proxima.title-3.shout.bold
       {:style {:clear "both"}}
       "Terms & Conditions"]
      (component/build legal/tos-form-footnote {:copy-type copy-type})]

     [:div.col-12.col-6-on-tb-dt.mx-auto.mt4
      (ui/submit-button "Agree & Submit"
                        {:data-test "force-set-password-submit"})]]]))

(defn query
  [data]
  (merge
   {:password       (get-in data keypaths/force-set-password-password)
    :show-password? (get-in data keypaths/account-show-password? true)
    :field-errors   (get-in data keypaths/field-errors)
    :focused        (get-in data keypaths/ui-focus)
    :show-optins?   (get-in data keypaths/reset-password-needs-optin-prompts?)
    :present-tos?   (get-in data keypaths/reset-password-present-tos?)
    :copy-type      (get-in data keypaths/reset-password-copy-type)}
   (legal/opt-in-query "stylist"
                       (get-in data keypaths/reset-password-sms-marketing-optin)
                       (get-in data keypaths/reset-password-sms-transactional-optin))))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/navigate-force-set-password
  [_ _ _ _ app-state]
  (if (get-in app-state keypaths/user-must-set-password)
    (assoc-in app-state keypaths/reset-password-token-requirements
              {:needs-optin-prompts true
               :present-tos         (boolean (get-in app-state keypaths/user-store-id))
               :copy-type           (if (get-in app-state keypaths/user-store-id)
                                      "stylist"
                                      "user")})
    app-state))

(defmethod effects/perform-effects events/navigate-force-set-password
  [_ _ _ _ app-state]
  (when-not (get-in app-state keypaths/user-must-set-password)
    (effects/redirect events/navigate-home)))

(defmethod transitions/transition-state events/api-success-force-set-password
  [_ _ updated-user app-state]
  (-> app-state
      (transitions/sign-in-user updated-user)
      (transitions/clear-fields keypaths/force-set-password-password)))

(defmethod effects/perform-effects events/api-success-force-set-password
  [_ _ _ _ app-state]
  (when-not (get-in app-state keypaths/user-must-set-password)
    (effects/redirect events/navigate-home)))

(defmethod effects/perform-effects events/control-force-set-password-submit
  [_ _ _ _ app-state]
  (api/force-set-password {:session-id        (get-in app-state keypaths/session-id)
                           :id                (get-in app-state keypaths/user-id)
                           :password          (get-in app-state keypaths/force-set-password-password)
                           :token             (get-in app-state keypaths/user-token)
                           :wants-smsable-mkt (boolean (get-in app-state keypaths/reset-password-sms-marketing-optin))
                           :wants-smsable-txn (boolean (get-in app-state keypaths/reset-password-sms-transactional-optin))}
                          #(messages/handle-message events/api-success-force-set-password
                                                    (api/select-user-keys %))))
