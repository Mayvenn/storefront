(ns storefront.components.checkout-returning-or-new-customer
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.facebook :as facebook]
            [storefront.components.checkout-address :as checkout-address]
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
       [:div.col-8.mx-auto.mb1
        [:h1.h3.center.mb1 "Been here before?"]
        [:p.h4.center "Sign in and we'll speed you through a secure checkout."]]

       [:div.col-10.col-8-on-tb.col-4-on-dt.mx-auto
        [:div.clearfix.mxn1
         [:div.col.col-6.p1
          (ui/teal-button (utils/route-to events/navigate-checkout-sign-in)
                          "Sign in")]
         [:div.col.col-6.p1
          (facebook/small-sign-in-button facebook-loaded?)]]]]]

     [:div.container.py2
      [:div.col-8.mx-auto
       [:h2.h3.center.mb1 "I'm new here"]
       [:p.h4.center
        "Just 3 simple steps to securely checkout."
        [:br]
        "You'll have an opportunity to create an account after placing your order."]]]

     (om/build checkout-address/component address)])))

(defn query [data]
  {:facebook-loaded? (get-in data keypaths/loaded-facebook)
   :address          (-> (checkout-address/query data)
                         (assoc-in [:shipping-address-data :become-guest?] true))})

(defn built-component [data opts]
  (om/build component (query data) opts))
