(ns adventure.checkout.sign-in
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.facebook :as facebook]

            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

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
                    :value   show-password?})]]
   [:div.col-12.col-6-on-tb-dt
    (ui/submit-button "Sign In"
                      {:data-test "user-submit"})]])

(defn query [data]
  {:email                   (get-in data keypaths/sign-in-email)
   :password                (get-in data keypaths/sign-in-password)
   :show-password?          (get-in data keypaths/account-show-password? true)
   :facebook-loaded?        (get-in data keypaths/loaded-facebook)
   :focused                 (get-in data keypaths/ui-focus)
   :field-errors            (get-in data keypaths/field-errors)})

(defcomponent component [{:keys [facebook-loaded?] :as sign-in-form-data} owner _]
  (ui/narrow-container
   [:div.p2
    [:h1.center.my2.mb3 "Sign in to your account"]
    (component/build password-component sign-in-form-data nil)
    [:div.my2
     [:div.dark-gray.center
      "Forgot Password? "
      [:a.teal (utils/route-to events/navigate-checkout-returning-or-guest) "Continue As Guest"]]
     [:div.dark-gray.center "OR"]]
    [:div.col-12.col-6-on-tb-dt.mx-auto
     (facebook/sign-in-button facebook-loaded?)]]))

(defn built-component [data opts]
  (component/build component (query data) opts))
