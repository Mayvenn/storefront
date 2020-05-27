(ns storefront.components.checkout-returning-or-guest
  (:require [storefront.component :as component :refer [defcomponent]]
            [checkout.returning-or-guest-v2020-05 :as v2020-05]
            [storefront.components.facebook :as facebook]
            [storefront.components.checkout-address :as checkout-address]
            [storefront.components.ui :as ui]
            [ui.promo-banner :as promo-banner]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.auth :as auth]))

(def or-separator
  [:div.black.py1.flex.items-center.col-10.mx-auto
   [:div.flex-grow-1.border-bottom.border-gray]
   [:div.h4.mx2 "or"]
   [:div.flex-grow-1.border-bottom.border-gray]])

(defcomponent component [{:keys [facebook-loaded? address promo-banner]} owner _]
  [:div
   [:div.container ;; Tries to match what's going on in checkout-address/component
    [:div
     (component/build promo-banner/sticky-organism promo-banner nil)
     [:div.m-auto.col-8-on-tb-dt
      [:div.px2.pt2
       [:div.center
        [:h1.canela.title-1 "Secure checkout"]
        [:div.canela.content-2.col-10.mx-auto
         "Sign in or checkout as guest. You’ll have an opportunity to create an account after placing your order."]]

       [:div.my2.mx-auto.col-12.col-8-on-tb-dt
        [:div
         [:div.col-10.mx-auto.py1
          (ui/button-medium-primary (assoc (utils/route-to events/navigate-checkout-sign-in)
                                          :data-test "begin-password-sign-in-button")
                                   "Sign in")]
         [:div.col-10.mx-auto.py1
          (facebook/narrow-sign-in-button facebook-loaded?)]]]
       or-separator]]]]
   (component/build checkout-address/component address)])

(defn query [data]
  {:facebook-loaded? (get-in data keypaths/loaded-facebook)
   :promo-banner     (promo-banner/query data)
   :address          (-> (checkout-address/query data)
                         (assoc-in [:shipping-address-data :become-guest?] true))})

(defn ^:export built-component
  [data opts]
  (component/build v2020-05/template (v2020-05/query data) opts))

(defn requires-sign-in-or-initiated-guest-checkout
  [authorized-component data opts]
  (if (auth/signed-in-or-initiated-guest-checkout? data)
    (authorized-component data nil)
    ;; rely on redirects to get you to the right page... if they misfire, user will be stuck on this page.
    (component/html
     [:div.h1.my4 ui/spinner])))
