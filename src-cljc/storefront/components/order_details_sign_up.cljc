(ns storefront.components.order-details-sign-up
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as e]
            [storefront.components.ui :as ui]
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
   {:on-submit (utils/send-event-callback e/control-sign-up-submit)}

   (ui/text-field {:data-test    "user-email"
                   :autocomplete "username"
                   :errors       (get field-errors ["email"])
                   :keypath      keypaths/sign-up-email
                   :focused      focused
                   :label        "Email"
                   :name         "email"
                   :required     true
                   :type         "email"
                   :value        email})

   (ui/text-field {:data-test    "user-password"
                   :autocomplete "current-password"
                   :errors       (get field-errors ["password"])
                   :keypath      keypaths/sign-up-password
                   :focused      focused
                   :label        "Password"
                   :name         "password"
                   :required     true
                   :type         "password"
                   :value        password
                   :hint         (when show-password? password)})

   [:div.mt2.mb2.col-12.left
    (ui/check-box {:label         "Show password"
                   :keypath       keypaths/account-show-password?
                   :value         show-password?
                   :label-classes "proxima content-1"})]

   [:div.col-12.col-8-on-tb-dt
    (ui/submit-button sign-up-text
                      {:data-test "user-submit"})]])

(defcomponent component [data _ _]
  (ui/narrow-container
   [:div.p2
    [:div.h1.center.mt2.mb3 "Sign up for order details"]
    [:div.h4.center.my4 "Please sign up with the email associated with your order to view your order details."]

     (form data {:sign-up-text "Sign Up"})]))

(defn query [data]
  {:email            (get-in data keypaths/sign-up-email)
   :password         (get-in data keypaths/sign-up-password)
   :show-password?   (get-in data keypaths/account-show-password? true)
   :field-errors     (get-in data keypaths/field-errors)
   :focused          (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (component/build component (query data) nil))
