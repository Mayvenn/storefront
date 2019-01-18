(ns storefront.components.checkout-returning-or-guest
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.component :as component]
            [storefront.components.facebook :as facebook]
            [storefront.components.checkout-address :as checkout-address]
            [storefront.components.ui :as ui]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.auth :as auth]))

(defn component [{:keys [facebook-loaded? address promotion-banner adventure?]} owner]
  (om/component
   (html
    [:div
     [:div.container ;; Tries to match what's going on in checkout-address/component
      (when-not adventure? ;;If adventure causes this page to get any more complex, fork the component.
        [:div
         (component/build promotion-banner/sticky-component promotion-banner nil)
         [:div.m-auto.col-8-on-tb-dt
          [:div.p2
           [:div.center
            [:h1 "Secure checkout"]
            [:p
             "Sign in or checkout as a guest."
             [:br]
             "You’ll have an opportunity to create an account after placing your order."]]

           [:div.my2.mx-auto.col-12.col-6-on-tb-dt
            [:div.clearfix.mxn1
             [:div.col.col-6.p1
              (ui/teal-button (assoc (utils/route-to events/navigate-checkout-sign-in)
                                     :data-test "begin-password-sign-in-button")
                              "Sign in")]
             [:div.col.col-6.p1
              (facebook/narrow-sign-in-button facebook-loaded?)]]]]]

         [:h2.mt1.center "Checkout as a guest"]])]
     (om/build checkout-address/component address)])))

(defn query [data]
  {:facebook-loaded? (get-in data keypaths/loaded-facebook)
   :promotion-banner (promotion-banner/query data)
   :adventure?       true
   :address          (-> (checkout-address/query data)
                         (assoc-in [:shipping-address-data :become-guest?] true))})

(defn built-component [data opts]
  (om/build component (query data) opts))

(defn requires-sign-in-or-initiated-guest-checkout [authorized-component data opts]
  (if (auth/signed-in-or-initiated-guest-checkout? data)
    (authorized-component data nil)
    ;; rely on redirects to get you to the right page... if they misfire, user will be stuck on this page.
    [:div.h1.my4 ui/spinner]))
