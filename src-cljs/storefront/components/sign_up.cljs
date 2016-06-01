(ns storefront.components.sign-up
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component
  [{:keys [facebook-loaded?
           email
           password
           password-confirmation]}]
  (om/component
   (html
    (ui/narrow-container
     [:.h2.center.my2 "Sign up for an account"]

     (facebook/sign-in-button facebook-loaded?)

     [:.h4.center.gray.light.my2 "OR"]

     [:form.col-12.flex.flex-column.items-center
      {:on-submit (utils/send-event-callback events/control-sign-up-submit)}

      (ui/text-field "Email"
                     keypaths/sign-up-email
                     email
                     {:autofocus "autofocus"
                      :type      "email"
                      :name      "email"
                      :data-test "user-email"
                      :required  true})

      (ui/text-field "Password"
                     keypaths/sign-up-password
                     password
                     {:type      "password"
                      :name      "password"
                      :data-test "user-password"
                      :required  true})

      (ui/text-field "Password Confirmation"
                     keypaths/sign-up-password-confirmation
                     password-confirmation
                     {:type     "password"
                      :name     "password-confirmation"
                      :data-test "user-password-confirmation"
                      :required true})

      (ui/submit-button "Sign Up"
                        {:data-test "user-submit"})

      [:.center.gray.mt3.mb2 "Already have an account? "
       [:a.green (utils/route-to events/navigate-sign-in) "Log In"]]]))))

(defn query [data]
  {:email                 (get-in data keypaths/sign-up-email)
   :password              (get-in data keypaths/sign-up-password)
   :password-confirmation (get-in data keypaths/sign-up-password-confirmation)
   :facebook-loaded?      (get-in data keypaths/loaded-facebook)})

(defn built-component [app-state owner]
  (om/component (html (om/build component (query app-state)))))
