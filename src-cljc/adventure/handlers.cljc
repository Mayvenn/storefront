(ns adventure.handlers
  (:require [storefront.effects :as effects]
            [storefront.events :as events]
            #?(:cljs [storefront.history :as history])
            [adventure.keypaths :as keypaths]
            [storefront.transitions :as transitions]))

(defmethod effects/perform-effects events/control-adventure
  [_ event {:keys [destination]} app-state-before app-state]
  #?(:cljs
     (history/enqueue-navigate destination nil)))

(defmethod transitions/transition-state events/control-adventure
  [_ event {:keys [choice]} app-state]
  (-> app-state
      (update-in keypaths/adventure-choices
                 merge choice)))
