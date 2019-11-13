(ns storefront.components.reset-password
  (:require [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component [{:keys [focused reset-password show-password? loaded-facebook? field-errors]} owner _]
  (ui/narrow-container
   [:div.p2
    [:h2.center.my2.navy.mb3 "Reset Your Password"]
    [:form.col-12
     {:on-submit (utils/send-event-callback events/control-reset-password-submit)}
     (ui/text-field {:errors     (get field-errors ["password"])
                     :data-test  "reset-password-password"
                     :keypath    keypaths/reset-password-password
                     :focused    focused
                     :label      "New Password"
                     :min-length 6
                     :required   true
                     :type       "password"
                     :value      reset-password
                     :hint       (when show-password? reset-password)})
     [:div.dark-gray.col-12.left
      (ui/check-box {:label   "Show password"
                     :keypath keypaths/account-show-password?
                     :focused focused
                     :value   show-password?})]

     [:div.col-12.col-6-on-tb-dt.mx-auto
      (ui/submit-button "Save & Continue"
                        {:data-test "reset-password-submit"})]]
    [:.h5.center.dark-gray.light.my2 "OR"]

    [:div.col-12.col-6-on-tb-dt.mx-auto
     (facebook/reset-button loaded-facebook?)]]))

(defn query [data]
  {:reset-password   (get-in data keypaths/reset-password-password)
   :show-password?   (get-in data keypaths/account-show-password? true)
   :loaded-facebook? (get-in data keypaths/loaded-facebook)
   :field-errors     (get-in data keypaths/field-errors)
   :focused          (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (component/build component (query data) opts))
