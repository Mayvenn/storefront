(ns storefront.components.sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn form-component
  [{:keys [facebook-loaded?
           email
           password
           remember-me?
           get-satisfaction-login?]}]
  (om/component
   (html
    [:.flex.flex-column.items-center.black.sans-serif.col-12.mt1

     (facebook/sign-in-button facebook-loaded?)
     [:.h4.gray.extra-light.my2 "OR"]

     [:form.col-12.flex.flex-column.items-center
      {:on-submit (utils/send-event-callback events/control-sign-in-submit)}

      (ui/text-field "Email"
                     keypaths/sign-in-email
                     email
                     {:autofocus "autofocus"
                      :type      "email"
                      :name      "email"
                      :required  true})

      (ui/text-field "Password"
                     keypaths/sign-in-password
                     password
                     {:type     "password"
                      :name     "password"
                      :required true})

      (ui/submit-button "Sign In")

      [:.mt2.col-12.mb3
       [:label.col.col-6.left-align.gray
        [:input#remember-me.align-middle
         (merge (utils/toggle-checkbox keypaths/sign-in-remember remember-me?)
                {:type "checkbox" :name "remember-me"})]
        [:.inline.ml1 "Remember me"]]
       [:a.col.col-6.right-align.gray
        (utils/route-to events/navigate-forgot-password) "Forgot Password?"]]]

     (when-not get-satisfaction-login?
       [:.clearfix.center.gray.mb2 "Don't have an account? "
        [:a.green (utils/route-to events/navigate-sign-up) "Register Here"]])])))

(defn component [{:keys [get-satisfaction-login?] :as form-data} owner]
  (om/component
   (html
    (ui/narrow-container
     (when-not get-satisfaction-login?
       [:.h2.center.my2 "Sign in to your account"])
     (om/build form-component form-data)))))

(defn query [data]
  {:email                   (get-in data keypaths/sign-in-email)
   :password                (get-in data keypaths/sign-in-password)
   :remember-me?            (get-in data keypaths/sign-in-remember)
   :facebook-loaded?        (get-in data keypaths/loaded-facebook)
   :get-satisfaction-login? (get-in data keypaths/get-satisfaction-login?)})

(defn built-component [data owner]
  (om/component (html (om/build component (query data)))))

(defn requires-sign-in [app-state authorized-component]
  (let [sign-in-component component]
    (if (get-in app-state keypaths/user-id) authorized-component sign-in-component)))

(defn redirect-getsat-component [data owner]
  (om/component
   (html
    [:div.centered-content
     ;; effects injects GetSat JS that will redirect / close this window as needed
     (if (or (nil? (get-in data keypaths/user))
             (get-in data keypaths/user-store-slug))
       [:div.page-heading.center "Signing in to the Mayvenn Stylist Community..."]
       [:div.flash.error
        "The Mayvenn Stylist Community is only for Mayvenn stylists. Become a stylist at welcome.mayvenn.com!"])])))
