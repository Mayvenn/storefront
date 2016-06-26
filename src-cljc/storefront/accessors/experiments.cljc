(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]))

(defn display-variation [data variation]
  (contains? (get-in data keypaths/optimizely-variations)
             variation))

(defn stylist-referrals? [data]
  (display-variation data "stylist-referrals"))

(defn color-option? [data]
  (display-variation data "color-option"))
