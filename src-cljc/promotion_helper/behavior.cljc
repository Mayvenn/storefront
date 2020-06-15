(ns promotion-helper.behavior
  (:require [storefront.transitions :as t]))

(defmethod t/transition-state [:ui :promotion-helper :opened]
  [_ event args app-state]
  (assoc-in app-state [:ui :promotion-helper :opened?] true))

(defmethod t/transition-state [:ui :promotion-helper :closed]
  [_ event args app-state]
  (assoc-in app-state [:ui :promotion-helper :opened?] false))
