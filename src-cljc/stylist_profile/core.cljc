(ns stylist-profile.core
  "Stylist profile"
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [stylist-profile.stylist-details :as v2]))

;; This should require stylist results and stylist details
;; and set module loaded

(defn ^:export page
  [state _]
  (v2/built-component state))
