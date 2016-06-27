(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]))

(defn display-variation [data variation]
  (contains? (get-in data keypaths/optimizely-variations)
             variation))

(defn color-option? [data]
  (display-variation data "color-option"))

(defn option-memory? [data]
  (display-variation data "option-memory"))
