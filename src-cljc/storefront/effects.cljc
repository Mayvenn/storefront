(ns storefront.effects
  #?(:cljs
     (:require
      [storefront.frontend-effects])))

(defmulti perform-effects identity)

(defmethod perform-effects :default [dispatch event args old-app-state app-state])
