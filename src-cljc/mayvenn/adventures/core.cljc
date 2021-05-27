(ns mayvenn.adventures.core
  "
  Adventures - we got them

  Visual Meta-layer: Multi-screen experiences to arrive at a goal
  (Traditionally called a wizard or user flow)

  This ns is general to all adventures, nothing specific please.
  "
  (:require [storefront.events :as e]
            [storefront.transitions :as t]))

(def state-path [:models :adventures])

(defmethod t/transition-state e/flow|adventure|advanced
  [_ _ {:adventure/keys [id step]} state]
  (assoc-in state
            (conj state-path id :adventure/step)
            step))

(defn <-
  [state initial-model adventure-id]
  (or
   (-> state
       (get-in (conj state-path adventure-id))
       not-empty)
   initial-model))
