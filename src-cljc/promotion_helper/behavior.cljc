(ns promotion-helper.behavior
  (:require [storefront.transitions :as t]
            [promotion-helper.keypaths :as k]))

;; Promotion Helper events
(def ui-promotion-helper-closed [:ui :promotion-helper :closed])
(def ui-promotion-helper-opened [:ui :promotion-helper :opened])

(defmethod t/transition-state ui-promotion-helper-opened
  [_ event args app-state]
  (-> app-state
      (assoc-in k/ui-promotion-helper-opened true)))

(defmethod t/transition-state ui-promotion-helper-closed
  [_ event args app-state]
  (-> app-state
      (assoc-in k/ui-promotion-helper-opened false)))
