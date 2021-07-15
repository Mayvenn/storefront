(ns mayvenn.concept.progression
  (:require [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.transitions :as t]))

;; records

(defn <- [state id]
  (get-in state (conj k/models-progressions id)))

;; behavior

;; TODO(corey) rename flow->biz
(defmethod t/transition-state e/biz|progression|reset
  [_ _ {:progression/keys [id value]} state]
  (assoc-in state (conj k/models-progressions id) value))

(defmethod t/transition-state e/biz|progression|progressed
  [_ _ {:progression/keys [id value regress]} state]
  (-> state
      (update-in (conj k/models-progressions id) #(conj (or %1 #{}) %2) value)
      (update-in (conj k/models-progressions id) #(apply disj %1 %2) regress)))
