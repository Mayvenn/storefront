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

(defmethod effects/perform-effects events/navigate-adventure
  [_ event args app-state-before app-state]
  (when (and (not= events/navigate-adventure-home event)
             (empty? (get-in app-state keypaths/adventure-choices)))
    #?(:cljs
       (history/enqueue-navigate events/navigate-adventure-home nil))))

(defmethod transitions/transition-state events/control-adventure
  [_ event {:keys [choice]} app-state]
  (-> app-state
      (update-in keypaths/adventure-choices
                 merge choice)))

(defmethod transitions/transition-state events/navigate-adventure-home
  [_ event args app-state]
  (-> app-state
      (update-in keypaths/adventure-choices
                 merge {:adventure :started})))
