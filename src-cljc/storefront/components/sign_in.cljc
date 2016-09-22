(ns storefront.components.sign-in
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.components.flash :as flash]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn form-component
  [{:keys [facebook-loaded?
           email
           password
           get-satisfaction-login?
           field-errors]} _ _]
  (component/create
   [:div.flex.flex-column.items-center.dark-gray.col-12.mt1

    (facebook/sign-in-button facebook-loaded?)
    [:div.h5.gray.light.my2 "OR"]

    [:form.col-12.flex.flex-column.items-center
     {:on-submit (utils/send-event-callback events/control-sign-in-submit)}

     (ui/text-field {:auto-focus "autofocus"
                     :data-test  "user-email"
                     :errors     (get field-errors ["email"])
                     :keypath    keypaths/sign-in-email
                     :label      "Email"
                     :name       "email"
                     :required   true
                     :type       "email"
                     :value      email})

     (ui/text-field {:data-test "user-password"
                     :errors    (get field-errors ["password"])
                     :keypath   keypaths/sign-in-password
                     :label     "Password"
                     :name      "password"
                     :required  true
                     :type      "password"
                     :value     password})

     (ui/submit-button "Sign In"
                       {:data-test "user-submit"})

     [:div.mt2.col-12.mb3.gray.right-align
      [:a
       (utils/route-to events/navigate-forgot-password) "Forgot Password?"]]]

    (when-not get-satisfaction-login?
      [:div.clearfix.center.gray.mb2 "Don't have an account? "
       [:a.teal (utils/route-to events/navigate-sign-up) "Register Here"]])]))

(defn component [{:keys [get-satisfaction-login?] :as form-data} owner opts]
  (component/create
   (ui/narrow-container
    (when-not get-satisfaction-login?
      [:div.h3.center.my2 "Sign in to your account"])
    (component/build form-component form-data nil))))

(defn query [data]
  {:email                   (get-in data keypaths/sign-in-email)
   :password                (get-in data keypaths/sign-in-password)
   :facebook-loaded?        (get-in data keypaths/loaded-facebook)
   :get-satisfaction-login? (get-in data keypaths/get-satisfaction-login?)
   :field-errors            (get-in data keypaths/field-errors)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn requires-sign-in [authorized-component data opts]
  (if (get-in data keypaths/user-id)
    (authorized-component data nil)
    (built-component data nil)))

(defn redirect-getsat-component [data owner opts]
  (component/create
   (ui/narrow-container
    ;; effects injects GetSat JS that will redirect / close this window as needed
    (if (or (nil? (get-in data keypaths/user))
            (get-in data keypaths/user-store-slug))
      (flash/success-box
       {:data-test "flash-notice"}
       [:div.px2 "Signing in to the Mayvenn Stylist Community..."])
      (flash/error-box
       {:data-test "flash-error"}
       [:div.px2 "The Mayvenn Stylist Community is only for Mayvenn stylists. Become a stylist at welcome.mayvenn.com!"])))))

(defn built-redirect-getsat-component [data opts]
  (component/build redirect-getsat-component data opts))
