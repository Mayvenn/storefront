(ns storefront.controllers.core
  (:require [storefront.events :as events]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [event arg app-state])
