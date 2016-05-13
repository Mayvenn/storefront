(ns storefront.components.sign-up
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.facebook :as facebook]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.keypaths :as keypaths]))

(defn redesigned-sign-up-component [{:keys [facebook-loaded?
                                            email
                                            password
                                            password-confirmation]}]
  (om/component
   (html
    (ui/container
     [:.h2.center.my2 "Sign up for an account"]
     (facebook/redesigned-sign-in-button facebook-loaded?)
     [:.h4.center.gray.extra-light.my2 "OR"]

     [:form.col-12.flex.flex-column.items-center
      {:on-submit (utils/send-event-callback events/control-sign-up-submit)}

      (ui/text-field "Email" keypaths/sign-up-email email
                     {:autofocus "autofocus"
                      :type "email"
                      :name "email"
                      :required true})

      (ui/text-field "Password" keypaths/sign-up-password password
                     {:type "password"
                      :name "password"
                      :required true})

      (ui/text-field "Password Confirmation" keypaths/sign-up-password-confirmation password-confirmation
                     {:type "password"
                      :name "password-confirmation"
                      :required true})

      (ui/submit-button "Sign Up")

      [:.center.gray.mt3.mb2 "Already have an account? "
       [:a.teal (utils/route-to events/navigate-sign-in) "Log In"]]]))))

(defn old-sign-up-component [data owner]
  (om/component
   (html
    [:div
     (om/build validation-errors-component data)
     [:div.centered-content
      [:div.page-heading.center "Sign Up For An Account"]
      [:div#existing-customer.new_spree_user
       (facebook/sign-in-button (get-in data keypaths/loaded-facebook))
       [:div.or-divider [:span "or"]]
       [:form.simple_form
        {:on-submit (utils/send-event-callback events/control-sign-up-submit)}
        [:div#password-credentials
         [:div.input.email
          [:label.email "Email"]
          [:input.string.email
           (merge (utils/change-text data owner keypaths/sign-up-email)
                  {:autofocus "autofocus"
                   :type "email"
                   :name "email"})]]
         [:div.input.password
          [:label.password "Password"]
          [:input.string.password
           (merge (utils/change-text data owner keypaths/sign-up-password)
                  {:type "password"
                   :name "password"})]]
         [:div.input.password
          [:label.password "Password Confirmation"]
          [:input.string.password
           (merge (utils/change-text data owner keypaths/sign-up-password-confirmation)
                  {:type "password"
                   :name "password-confirmation"})]]]
        [:p
         [:input.button.primary {:type "submit"
                                 :value "Create"}]]]]
      [:p.center "Already have an account? "
       [:a (utils/route-to events/navigate-sign-in) "Log In"]]]])))


(defn query [data]
  {:email (get-in data keypaths/sign-up-email)
   :password (get-in data keypaths/sign-up-password)
   :password-confirmation (get-in data keypaths/sign-up-password-confirmation)
   :facebook-loaded? (get-in data keypaths/loaded-facebook)})

(defn sign-up-component [app-state owner]
  (om/component
   (html
    [:div
     (if (experiments/three-steps-redesign? app-state)
       (om/build redesigned-sign-up-component (query app-state))
       (om/build old-sign-up-component app-state))])))
