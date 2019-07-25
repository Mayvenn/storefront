(ns storefront.components.checkout-address-auth-required
  (:require [om.core :as om]
            [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
            [storefront.components.checkout-address :as checkout-address]))

(defn ^:export built-component [data opts]
  (checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout
   checkout-address/built-component
   data
   opts))

