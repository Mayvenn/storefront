(ns storefront.components.checkout-sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.ui :as ui]
            [storefront.components.facebook :as facebook]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [facebook-loaded?] :as sign-in-form-data} owner]
  (om/component
   (html
    (ui/narrow-container
     [:div.p2
      [:h1.center.my2.mb3 "Sign in to your account"]
      (om/build sign-in/password-component sign-in-form-data)
      [:div.dark-gray.center.mb2 "OR"]
      [:div.col-12.col-6-on-tb-dt.mx-auto
       (facebook/sign-in-button facebook-loaded?)]]))))

(defn built-component [data opts]
  (om/build component (sign-in/query data) opts))
