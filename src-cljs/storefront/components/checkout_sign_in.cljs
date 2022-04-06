(ns storefront.components.checkout-sign-in
  (:require [storefront.components.sign-in :as sign-in]
            [storefront.components.ui :as ui]
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component
  [sign-in-form-data owner _]
  (ui/narrow-container
   [:div.p2
    [:h1.center.my2.mb3 "Sign in to your account"]
    (component/build sign-in/password-component sign-in-form-data)]))

(defn ^:export built-component
  [data opts]
  (component/build component (sign-in/query data) opts))
