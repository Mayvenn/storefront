(ns stylist-profile.core
  "Stylist profile"
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [stylist-profile.stylist-details-v2021-10 :as v2]
            [stylist-profile.stylist-details :as v1]
            checkout.cart.swap))

;; This should require stylist results and stylist details
;; and set module loaded

(defn ^:export page
  [state _]
  (if (experiments/stylist-profile? state)
    (v2/built-component state)
    (v1/built-component state)))
