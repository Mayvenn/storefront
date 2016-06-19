(ns storefront.components.forgot-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [email facebook-loaded?]} owner]
  (om/component
   (html
    (ui/narrow-container
     [:.h2.center.my2 "Reset your forgotten password"]

     [:form.col-12.flex.flex-column.items-center
      {:on-submit (utils/send-event-callback events/control-forgot-password-submit)}

      (ui/text-field "Email"
                     keypaths/forgot-password-email
                     email
                     {:autofocus "autofocus"
                      :type "email"
                      :name "email"
                      :required true})

      (ui/submit-button "Reset my password")]

     [:.h4.center.gray.light.my2 "OR"]

     (facebook/sign-in-button facebook-loaded?)))))

(defn query [data]
  {:facebook-loaded? (get-in data keypaths/loaded-facebook)
   :email            (get-in data keypaths/forgot-password-email)})

(defn built-component [data owner]
  (om/component (html (om/build component (query data)))))
