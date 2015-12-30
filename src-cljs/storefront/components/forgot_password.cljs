(ns storefront.components.forgot-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.facebook :as facebook]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.validation-errors :refer [validation-errors-component]]))

(defn forgot-password-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "Reset Your Forgotten Password"]
     (om/build validation-errors-component data)
     [:div#forgot-password.new_spree_user
      [:form.simple_form
       {:on-submit (utils/send-event-callback data events/control-forgot-password-submit)}
       [:div.input.email
        [:label {:for "email"} "Enter your email:"]
        [:input.string.email
         (merge (utils/change-text data owner keypaths/forgot-password-email)
                {:autofocus "autofocus"
                 :type "email"
                 :name "email"})]]
       [:p
        [:input.button.primary.mb-l {:type "submit"
                                     :value "Reset my password"}]]]
      (when (experiments/facebook? data)
        (list
         [:div.or-divider.my-0 [:span "or"]]
         (facebook/button data)))]])))
