(ns storefront.components.force-reset-password
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [focused reset-password show-password? loaded-facebook? field-errors]} owner]
  (om/component
   (html
    (ui/narrow-container
     [:div.p2
      [:h2.center.my2.navy.mb3 "You must reset Your Password"]
      [:form.col-12
       {:on-submit (utils/send-event-callback events/control-reset-password-submit)}
       (ui/text-field {:errors     (get field-errors ["password"])
                       :data-test "reset-password-password"
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
        (ui/submit-button "Save & Continue" {:data-test "reset-password-submit"})]]]))))

(defn query [data]
  {:reset-password   (get-in data keypaths/reset-password-password)
   :show-password?   (get-in data keypaths/account-show-password? true)
   :field-errors     (get-in data keypaths/field-errors)
   :focused          (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (om/build component (query data) opts))
