(ns adventure.handlers
  (:require [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as storefront.keypaths]
            #?@(:cljs
                [[storefront.history :as history]
                 [storefront.browser.cookie-jar :as cookie]])
            [adventure.keypaths :as keypaths]
            [storefront.transitions :as transitions]))

(defmethod transitions/transition-state events/control-adventure-choice
  [_ event {:keys [choice]} app-state]
  (-> app-state
      (update-in keypaths/adventure-choices
                 merge choice)))

(defmethod effects/perform-effects events/control-adventure-choice
  [_ _ {:keys [choice]} _ app-state]
  #?(:cljs
     (do
       (if (map? destination)
         (history/enqueue-navigate (:event destination) (:args destination))
         (history/enqueue-navigate destination nil))
       (let [cookie    (get-in app-state storefront.keypaths/cookie)
             adventure (get-in app-state keypaths/adventure)]
         (cookie/save-adventure cookie adventure)))))

(defmethod effects/perform-effects events/navigate-adventure
  [_ event args app-state-before app-state]
  (when (and (not= events/navigate-adventure-home event)
             (empty? (get-in app-state keypaths/adventure-choices)))
    #?(:cljs
       (history/enqueue-navigate events/navigate-adventure-home nil))))

;; Perhaps there is a better way to "start" the flow in the app-state
;;   e.g. {:flow/version 1}
;; Perhaps the basic_prompt and multi_prompt could both do control-adventure
(defmethod transitions/transition-state events/navigate-adventure-home
  [_ event args app-state]
  (-> app-state
      (update-in keypaths/adventure-choices
                 merge {:adventure :started})))
