(ns storefront.components.reset-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.facebook :as facebook]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.validation-errors :refer [validation-errors-component redesigned-validation-errors-component]]))

(defn redesigned-reset-password-component [{:keys [reset-password reset-password-confirmation errors loaded-facebook?]} owner]
  (om/component
   (html
    (ui/narrow-container
     [:.h2.center.my2 "Update Your Password"]
     (om/build redesigned-validation-errors-component errors)
     [:form.col-12
      {:on-submit (utils/send-event-callback events/control-reset-password-submit)}
      (ui/text-field "Password" keypaths/reset-password-password reset-password
                     {:type "password"
                      :required true
                      :min-length 6})
      (ui/text-field "Password Confirmation" keypaths/reset-password-password-confirmation reset-password-confirmation
                     {:type "password"
                      :required true
                      :min-length 6})

      (ui/submit-button "Update")]
     [:.h4.center.gray.extra-light.my2 "OR"]
     (facebook/redesigned-reset-button loaded-facebook?)))))

(defn old-reset-password-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "Update Your Password"]
     (om/build validation-errors-component data)
     [:div#change-password.new_spree_user
      [:form.simple_form
       {:on-submit (utils/send-event-callback events/control-reset-password-submit)}
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
        [:input.button.primary.mb3 {:type "submit"
                                    :value "Update"}]]]
      [:div.or-divider.my0 [:span "or"]]
      (facebook/reset-button data)]])))

(defn query [data]
  {:reset-password              (get-in data keypaths/reset-password-password)
   :reset-password-confirmation (get-in data keypaths/reset-password-password-confirmation)
   :errors                      (get-in data keypaths/validation-errors-details)
   :loaded-facebook?            (get-in data keypaths/loaded-facebook)})

(defn reset-password-component [data owner]
  (om/component
   (html
    (if (experiments/three-steps-redesign? data)
      (om/build redesigned-reset-password-component (query data))
      (om/build old-reset-password-component data)))))
