(ns storefront.components.forgot-password
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defcomponent component [{:keys [email facebook-loaded? field-errors focused]} owner opts]
  (ui/narrow-container
   [:div.p2
    [:h1.h2.center.my2.mb3 "Reset your password"]

    [:form.col-12.flex.flex-column.items-center
     {:on-submit (utils/send-event-callback events/control-forgot-password-submit)}
     (ui/text-field {:errors    (get field-errors ["email"])
                     :data-test "forgot-password-email"
                     :keypath   keypaths/forgot-password-email
                     :focused   focused
                     :label     "Email"
                     :name      "email"
                     :required  true
                     :type      "email"
                     :value     email})

     [:div.col-12.col-8-on-tb-dt.mx-auto
      (ui/submit-button "Reset my password" {:data-test "forgot-password-submit"})]]

    [:div.h5.center.light.my2 "OR"]

    [:div.col-12.col-8-on-tb-dt.mx-auto
     (facebook/sign-in-button facebook-loaded?)]]))

(defn query [data]
  {:facebook-loaded? (get-in data keypaths/loaded-facebook)
   :email            (get-in data keypaths/forgot-password-email)
   :field-errors     (get-in data keypaths/field-errors)
   :focused          (get-in data keypaths/ui-focus)})

(defn built-component [data opts]
  (component/build component (query data) nil))
