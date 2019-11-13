(ns storefront.components.sign-up
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn form
  [{:keys [focused
           field-errors
           email
           password
           show-password?]}
   {:keys [sign-up-text]}]
  [:form.col-12.flex.flex-column.items-center
   {:on-submit (utils/send-event-callback events/control-sign-up-submit)}

   (ui/text-field {:data-test  "user-email"
                   :errors     (get field-errors ["email"])
                   :keypath    keypaths/sign-up-email
                   :focused    focused
                   :label      "Email"
                   :name       "email"
                   :required   true
                   :type       "email"
                   :value      email})

   (ui/text-field {:data-test "user-password"
                   :errors    (get field-errors ["password"])
                   :keypath   keypaths/sign-up-password
                   :focused   focused
                   :label     "Password"
                   :name      "password"
                   :required  true
                   :type      "password"
                   :value     password
                   :hint      (when show-password? password)})

   [:div.dark-gray.mt2.mb2.col-12.left
    (ui/check-box {:label   "Show password"
                   :keypath keypaths/account-show-password?
                   :value   show-password?})]

   [:div.col-12.col-6-on-tb-dt
    (ui/submit-button sign-up-text
                      {:data-test "user-submit"})]])

(defcomponent component [{:keys [facebook-loaded?] :as data} _ _]
  (ui/narrow-container
   [:div.p2
    [:h1.h2.center.mt2.mb3.navy "Sign up for an account"]

    [:div
     [:div.col-12.col-6-on-tb-dt.mx-auto
      (facebook/sign-in-button facebook-loaded?)]

     [:div.h5.center.dark-gray.light.my2 "OR"]

     (form data {:sign-up-text "Sign Up"})]

    [:div.center.dark-gray.mt2.mb2 "Already have an account? "
     [:a.teal (utils/route-to events/navigate-sign-in) "Log In"]]]))

(defn query [data]
  {:email            (get-in data keypaths/sign-up-email)
   :password         (get-in data keypaths/sign-up-password)
   :show-password?   (get-in data keypaths/account-show-password? true)
   :facebook-loaded? (get-in data keypaths/loaded-facebook)
   :field-errors     (get-in data keypaths/field-errors)
   :focused          (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (component/build component (query data) nil))
