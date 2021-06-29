(ns mayvenn.concept.follow
  (:require [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.transitions :as t]
            [storefront.platform.messages
             :refer [handle-message]
             :rename {handle-message publish}]))

(defn <-
  [state event-id]
  (get-in state (conj k/models-follows event-id)))

(defn clear
  [state event-id]
  (cond-> state
    (get-in state (conj k/models-follows event-id))
    (update-in k/models-follows dissoc event-id)))

(defn publish-all
  [state event-id event-args]
  (doseq [[f-event f-args]
          (some-> state
                  (get-in (conj k/models-follows event-id))
                  vector
                  not-empty)]
    (publish f-event
             (assoc f-args
                    :follow/args event-args))))

(defmethod t/transition-state e/biz|follow|defined
  [_ _ {:follow/keys [after-id then]} state]
  (-> state
      (assoc-in (conj k/models-follows after-id)
       then)))

(defmethod fx/perform-effects e/biz|follow|defined
  [_ _ {:follow/keys [start]} _ _]
  (apply publish start))

