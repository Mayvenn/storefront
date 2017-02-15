(ns storefront.components.checkout-complete
  (:require [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.sign-up :as sign-up]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(def ^:private order-complete-check
  (svg/circled-check {:class "stroke-navy"
                      :style {:width "80px" :height "80px"}}))

(defn old-component [data _]
  (component/create
   (ui/narrow-container
    [:div.p2
     [:div.my2.center order-complete-check]

     [:div.mx3
      [:div.h2.center
       {:data-test "checkout-success-message"}
       "Thank you for your order!"]

      [:div.py2
       [:p.my2.dark-gray
        "We've received your order and will be processing it right away. Once your order ships we will send you an email confirmation."]]]

     [:div.mb2.mx-auto.col-12.col-6-on-tb-dt
      (ui/teal-button (utils/route-to events/navigate-home) "Return to Homepage")]])))

(defn address-login-component [{:keys [address-login? guest? sign-up-data]} _]
  (component/create
   (ui/narrow-container
    [:div.p2
     [:section.py2.mx-auto.center
      [:img {:src (assets/path "/images/icons/success.png")
             :height "55px"
             :width "55px"}]
      [:h1 {:data-test "checkout-success-message"} "Thank you for your order!"]
      [:p "We've received your order and will be processing it right away. Once your order ships we will send you an email confirmation."]]
     (when guest?
       [:div
        [:section.py2.mx-auto.center
         [:img {:src (assets/path "/images/icons/profile.png")
                :height "55px"
                :width "55px"}]
         [:h1 "Create an account"]
         [:p "Take advantage of express checkout, order tracking, and more when you sign up."]

         [:p.py2 "Sign in with Facebook to link your account."]]
        (sign-up/form sign-up-data {:sign-up-text "Create my account"})])])))

(defn component [{:keys [address-login?] :as data} opts]
  (if address-login?
    (address-login-component data opts)
    (old-component data opts)))

(defn query [data]
  {:address-login? (experiments/address-login? data)
   :guest?         (not (get-in data keypaths/user-id))
   :sign-up-data   (sign-up/query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
