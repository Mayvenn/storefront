(ns storefront.components.reset-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.components.utils :as utils]
            [storefront.components.validation-errors :as validation-errors]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [reset-password reset-password-confirmation errors loaded-facebook?]} owner]
  (om/component
   (html
    (ui/narrow-container
     [:.h2.center.my2 "Update Your Password"]
     (om/build validation-errors/component errors)
     [:form.col-12
      {:on-submit (utils/send-event-callback events/control-reset-password-submit)}
      (ui/text-field "Password"
                     keypaths/reset-password-password
                     reset-password
                     {:type       "password"
                      :required   true
                      :min-length 6})
      (ui/text-field "Password Confirmation"
                     keypaths/reset-password-password-confirmation
                     reset-password-confirmation
                     {:type       "password"
                      :required   true
                      :min-length 6})

      (ui/submit-button "Update")]
     [:.h4.center.gray.light.my2 "OR"]
     (facebook/reset-button loaded-facebook?)))))

(defn query [data]
  {:reset-password              (get-in data keypaths/reset-password-password)
   :reset-password-confirmation (get-in data keypaths/reset-password-password-confirmation)
   :errors                      (get-in data keypaths/validation-errors-details)
   :loaded-facebook?            (get-in data keypaths/loaded-facebook)})

(defn built-component [data owner]
  (om/component (html (om/build component (query data)))))
