(ns mayvenn.concept.wait
  (:require [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.transitions :as t]
            [storefront.platform.messages
             :refer [handle-message handle-later]
             :rename {handle-message publish
                      handle-later   publish-later}]))

(defn <-
  [state id]
  (get-in state (conj k/models-wait id)))

(defmethod t/transition-state e/flow|wait|begun
  [_ _ {wait-id :wait/id} state]
  (assoc-in state (conj k/models-wait wait-id) true))

(defmethod fx/perform-effects e/flow|wait|begun
  [_ _ {wait-id :wait/id} _ _]
  (publish-later e/flow|wait|elapsed
                 {:wait/id wait-id
                  :wait/ms 3000}
                 3000))

(defmethod t/transition-state e/flow|wait|elapsed
  [_ _ {wait-id :wait/id} state]
  (assoc-in state (conj k/models-wait wait-id) false))
