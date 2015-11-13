(ns storefront.components.reset-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.validation-errors :refer [validation-errors-component]]))

(defn reset-password-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "Update Your Password"]
     (om/build validation-errors-component data)
     [:div#change-password
      [:form.new_spree_user
       {:on-submit (utils/send-event-callback data events/control-reset-password-submit)}
       [:p
        [:label {:for "spree_user_password"} "Password"]
        [:br]
        [:input#spree_user_password
         (merge
          (utils/change-text data owner keypaths/reset-password-password)
          {:type "password"
           :name "password"})]]
       [:p
        [:label {:for "spree_user_email"} "Password Confirmation"]
        [:br]
        [:input#spree_user_email
         (merge
          (utils/change-text data owner keypaths/reset-password-password-confirmation)
          {:type "password"
           :name "password-confirmation"})]]
       [:p
        [:input.button.primary {:type "submit"
                                :value "Update"
                                :class (when (experiments/simplify-funnel? data) "bright")}]]]]])))
