(ns storefront.components.sign-in
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defcomponent password-component [{:keys [email
                                          password
                                          focused
                                          show-password?
                                          field-errors]} _ _]
  [:form.col-12.flex.flex-column.items-center
   {:on-submit (utils/send-event-callback events/control-sign-in-submit)}

   (ui/text-field {:data-test  "user-email"
                   :errors     (get field-errors ["email"])
                   :keypath    keypaths/sign-in-email
                   :focused    focused
                   :label      "Email"
                   :name       "email"
                   :required   true
                   :type       "email"
                   :value      email})

   (ui/text-field {:data-test "user-password"
                   :errors    (get field-errors ["password"])
                   :keypath   keypaths/sign-in-password
                   :focused   focused
                   :label     "Password"
                   :name      "password"
                   :required  true
                   :hint      (when show-password? password)
                   :type      "password"
                   :value     password})

   [:div.col-12.mt2
    [:div.left.col-6
     (ui/check-box {:label   "Show password"
                    :keypath keypaths/account-show-password?
                    :value   show-password?})]

    [:div.right.col-6.right-align
     [:a (merge {:data-test "forgot-password"}
                (utils/route-to events/navigate-forgot-password))
      "Forgot Password?"]]]
   [:div.col-12.col-8-on-tb-dt
    (ui/submit-button "Sign In"
                      {:data-test "user-submit"})]])

(defcomponent form-component
  [data _ _]
  [:div.flex.flex-column.items-center.col-12.mt1
   (component/build password-component data nil)

   [:div.clearfix.center.my2 "Don't have an account? "
    [:a.p-color (utils/route-to events/navigate-sign-up) "Register Here"]]])

(defcomponent component [form-data owner opts]
  (ui/narrow-container
   [:div.p2
    [:h1.h2.center.my2.mb3 "Sign in to your account"]
    (component/build form-component form-data nil)]))

(defn query [data]
  {:email                   (get-in data keypaths/sign-in-email)
   :password                (get-in data keypaths/sign-in-password)
   :show-password?          (get-in data keypaths/account-show-password? true)
   :focused                 (get-in data keypaths/ui-focus)
   :field-errors            (get-in data keypaths/field-errors)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn requires-sign-in [authorized-component data opts]
  (if (get-in data keypaths/user-id)
    (authorized-component data nil)
    (built-component data nil)))
