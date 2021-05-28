(ns mayvenn.concept.progression
  (:require [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.transitions :as t]))

;; records

(defn <- [state id]
  (get-in state (conj k/models-progressions id)))

;; behavior

(defmethod t/transition-state e/flow|progression|reset
  [_ _ {:progression/keys [id value]} state]
  (assoc-in state (conj k/models-progressions id) value))

(defmethod t/transition-state e/flow|progression|progressed
  [_ _ {:progression/keys [id value]} state]
  (update-in state (conj k/models-progressions id) conj value))