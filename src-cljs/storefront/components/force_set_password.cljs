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

            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component
  [{:keys [focused password show-password? loaded-facebook? field-errors]} owner _]
  (ui/narrow-container
   [:div.p2
    [:h2.center.my2.navy.mb3
     "Please set your password"]
    [:form.col-12
     {:on-submit (utils/send-event-callback events/control-force-set-password-submit)}
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
     [:div.dark-gray.col-12.left
      (ui/check-box {:label   "Show password"
                     :keypath keypaths/account-show-password?
                     :focused focused
                     :value   show-password?})]

     [:div.col-12.col-6-on-tb-dt.mx-auto
      (ui/submit-button "Save & Continue"
                        {:data-test "force-set-password-submit"})]]]))

(defn query
  [data]
  {:password       (get-in data keypaths/force-set-password-password)
   :show-password? (get-in data keypaths/account-show-password? true)
   :field-errors   (get-in data keypaths/field-errors)
   :focused        (get-in data keypaths/ui-focus)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

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
  (api/force-set-password {:session-id (get-in app-state keypaths/session-id)
                           :id         (get-in app-state keypaths/user-id)
                           :password   (get-in app-state keypaths/force-set-password-password)
                           :token      (get-in app-state keypaths/user-token)}
                          #(messages/handle-message events/api-success-force-set-password
                                                    (api/select-user-keys %))))
