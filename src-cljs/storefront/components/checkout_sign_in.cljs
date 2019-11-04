(ns storefront.components.checkout-sign-in
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [adventure.checkout.sign-in :as adventure-sign-in]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.ui :as ui]
            [storefront.components.facebook :as facebook]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component :refer [defcomponent]]
            ))

(defcomponent component [{:keys [facebook-loaded?] :as sign-in-form-data} owner _]
  (ui/narrow-container
     [:div.p2
      [:h1.center.my2.mb3 "Sign in to your account"]
      (component/build sign-in/password-component sign-in-form-data)
      [:div.dark-gray.center.my2 "OR"]
      [:div.col-12.col-6-on-tb-dt.mx-auto
       (facebook/sign-in-button facebook-loaded?)]]))

(defn ^:export built-component [data opts]
  (if (= "freeinstall" (get-in data keypaths/store-slug))
    (adventure-sign-in/built-component data opts)
    (component/build component (sign-in/query data) opts)))
