(ns storefront.components.checkout-returning-or-guest
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.facebook :as facebook]
            [storefront.components.checkout-address :as checkout-address]
            [storefront.components.checkout-sign-in :as checkout-sign-in]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [facebook-loaded? address]} owner]
  (om/component
   (html
    [:div.gray
     [:div.border-bottom.border-dark-silver
      [:div.container.py2
       [:div.col-8.mx-auto.mb1.center
        [:h1.h3.mb1 "Secure checkout"]
        [:p.h4.mb1 "Sign in or checkout as a guest."]]

       [:div.col-10.col-8-on-tb.col-4-on-dt.mx-auto
        [:div.clearfix.mxn1
         [:div.col.col-6.p1
          (ui/teal-button (assoc (utils/route-to events/navigate-checkout-sign-in)
                                 :data-test "begin-password-sign-in-button")
                          "Sign in")]
         [:div.col.col-6.p1
          (facebook/small-sign-in-button facebook-loaded?)]]]]]

     [:div.container.py2
      [:div.col-8.mx-auto
       [:h2.h3.center.mb1 "Checkout as a guest"]]]

     (om/build checkout-address/component address)])))

(defn query [data]
  {:facebook-loaded? (get-in data keypaths/loaded-facebook)
   :address          (-> (checkout-address/query data)
                         (assoc-in [:shipping-address-data :become-guest?] true))})

(defn built-component [data opts]
  (om/build component (query data) opts))


(defn requires-sign-in-or-guest [authorized-component data opts]
  (if (or (get-in data keypaths/user-id)
          (get-in data keypaths/checkout-as-guest))
    (authorized-component data nil)
    (if (experiments/address-login? data)
      ;; rely on redirects to get you to the right page... if they misfire, user will be stuck on this page.
      [:div.h1.my4 ui/spinner]
      (checkout-sign-in/built-full-component data nil))))
