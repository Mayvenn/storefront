(ns mayvenn.concept.persona
  (:require #?@(:cljs
                [[storefront.hooks.stringer :as stringer]
                 [storefront.api :as api]
                 [storefront.hooks.reviews :as review-hooks]
                 storefront.frontend-trackings])
            api.orders
            [clojure.string :refer [join]]
            [mayvenn.concept.booking :as booking]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.visual.tools :refer [with within]]
            [spice.selector :as selector]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.trackings :as trk]
            [storefront.transitions :as t]))

#_
(def product-annotations
  {"120" {:customer/persona :customer.persona/p1}
   "236" {:customer/persona :customer.persona/p1}
   "9"   {:customer/persona :customer.persona/p1}
   "353" {:customer/persona :customer.persona/p1}
   "335" {:customer/persona :customer.persona/p2}
   "354" {:customer/persona :customer.persona/p2}
   "268" {:customer/persona :customer.persona/p2}
   "249" {:customer/persona :customer.persona/p2}
   "352" {:customer/persona :customer.persona/p3}
   #_#_"354" {:customer/persona :customer.persona/p3}
   "235" {:customer/persona :customer.persona/p3}
   "313" {:customer/persona :customer.persona/p3}
   #_#_"354" {:customer/persona :customer.persona/p4}
   "128" {:customer/persona :customer.persona/p4}
   "252" {:customer/persona :customer.persona/p4}
   "15" {:customer/persona :customer.persona/p4}})
#_
(def look-annotations
  {:7e4F9lGZHmTphADJxRFL6a {:customer/goals :customer.goals/enhance-natural}
   :2LcC986WiwwveAsh9HwEKZ {:customer/goals :customer.goals/protect-natural}
   :2KKdNG4k1JlFQWtOIXWHWB {:customer/goals :customer.goals/save-money}
   :74i7UgWjKVrAtV1mqMVMoh {:customer/goals :customer.goals/easy-maintenance}})

;;;; Models

(defn <-
  "Get the results model of a look suggestion"
  [state]
  (get-in state k/models-persona))

;;;; Behavior

;; Reset

(defmethod t/transition-state e/persona|reset
  [_ _ _ state]
  (-> state
      (assoc-in k/models-persona nil)))

;; Selected
;; Assume the event id or query the questionings for an answer

(defn ^:private calculate-persona-from-quiz
  [state]
  (let [{:keys [answers]} (questioning/<- state :crm/persona)]
    (case (:customer/styles answers)
      :customer.styles/everyday-look    :p1
      :customer.styles/work             :p2
      :customer.styles/switch-it-up     :p3
      :unsure                           :p3
      :customer.styles/special-occasion :p4
      :customer.styles/vacation         :p4
      :p3)))

(defmethod t/transition-state e/persona|selected
  [_ _ {:persona/keys [id]} state]
  (-> state
      (assoc-in k/models-persona (or id
                                     (calculate-persona-from-quiz state)))))

(defmethod fx/perform-effects e/persona|selected
  [_ _ {:keys [on/success-fn]} _ state]
  (when (fn? success-fn)
    (when-let [persona-id (name (get-in state k/models-persona))]
      (success-fn persona-id))))
