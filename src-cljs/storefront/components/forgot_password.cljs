(ns storefront.components.forgot-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.facebook :as facebook]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.validation-errors :refer [validation-errors-component]]))

(defn redesigned-forgot-password-component [{:keys [email facebook-loaded?]} owner]
  (om/component
   (html
    (ui/container
     [:.h2.center.my2 "Reset your forgotten password"]

     [:form.col-12.flex.flex-column.items-center
      {:on-submit (utils/send-event-callback events/control-forgot-password-submit)}

      (ui/text-field "Email" keypaths/forgot-password-email email
                     {:autofocus "autofocus"
                      :type "email"
                      :name "email"
                      :required true})

      (ui/submit-button "Reset my password")]

     [:.h4.center.gray.extra-light.my2 "OR"]

     (facebook/redesigned-sign-in-button facebook-loaded?)))))

(defn old-forgot-password-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "Reset Your Forgotten Password"]
     (om/build validation-errors-component data)
     [:div#forgot-password.new_spree_user
      [:form.simple_form
       {:on-submit (utils/send-event-callback events/control-forgot-password-submit)}
       [:div.input.email
        [:label {:for "email"} "Enter your email:"]
        [:input.string.email
         (merge (utils/change-text data owner keypaths/forgot-password-email)
                {:autofocus "autofocus"
                 :type "email"
                 :name "email"})]]
       [:p
        [:input.button.primary.mb3 {:type "submit"
                                    :value "Reset my password"}]]]
      [:div.or-divider.my0 [:span "or"]]
      (facebook/sign-in-button (get-in data keypaths/loaded-facebook))]])))

(defn query [data]
  {:facebook-loaded?  (get-in data keypaths/loaded-facebook)
   :email             (get-in data keypaths/forgot-password-email)})

(defn forgot-password-component [data owner]
  (om/component
   (html
    [:div
     (if (experiments/three-steps-redesign? data)
       (om/build redesigned-forgot-password-component (query data))
       (om/build old-forgot-password-component data))])))

