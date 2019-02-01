(ns storefront.trackings
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            #?(:cljs [storefront.frontend-trackings])
            [spice.core :as spice]))

(defmulti perform-track identity)

(defmethod perform-track :default
  [dispatch event args app-state])
