(ns storefront.components.reset-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.facebook :as facebook]
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
     [:div#change-password.new_spree_user
      [:form.simple_form
       {:on-submit (utils/send-event-callback data events/control-reset-password-submit)}
       [:div.input.password
        [:label {:for "spree_user_password"} "Password"]
        [:input#spree_user_password.string.password
         (merge
          (utils/change-text data owner keypaths/reset-password-password)
          {:type "password"
           :name "password"})]]
       [:div.input.password
        [:label {:for "spree_user_email"} "Password Confirmation"]
        [:input#spree_user_email.string.password
         (merge
          (utils/change-text data owner keypaths/reset-password-password-confirmation)
          {:type "password"
           :name "password-confirmation"})]]
       [:p
        [:input.button.primary.mb-l {:type "submit"
                                     :value "Update"}]]]
      (when (get-in data keypaths/facebook-loaded)
        (list
         [:div.or-divider.my-0 [:span "or"]]
         (facebook/reset-button data)))]])))
