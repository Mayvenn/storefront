(ns storefront.components.force-set-password
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn component
  [{:keys [focused password show-password? loaded-facebook? field-errors]} owner]
  (component/create
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
                         {:data-test "force-set-password-submit"})]]])))

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
