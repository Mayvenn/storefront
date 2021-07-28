(ns appointment-booking.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]
                       [storefront.accessors.experiments :as experiments]])
            api.orders
            api.stylist
            storefront.keypaths
            [spice.date :as date]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [appointment-booking.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            clojure.set
            [storefront.transitions :as t]))

(defn ^:private start-of-day [date]
  #?(:cljs
     (doto date
       (.setHours 0)
       (.setMinutes 0)
       (.setSeconds 0)
       (.setMilliseconds 0))))

(defmethod t/transition-state e/navigate-adventure-appointment-booking
  [_ _event {:keys [] :as _args} state]
  state)

(defmethod fx/perform-effects e/navigate-adventure-appointment-booking
  [_ _ _ _ state]
  #?(:cljs
     (if (experiments/easy-booking? state)
       (publish e/flow|appointment-booking|initialized {})
       (history/enqueue-redirect e/navigate-home {}))))

(defmethod t/transition-state e/flow|appointment-booking|initialized
  [_ _event _args state]
  (-> state
      (assoc-in k/booking-selected-date nil)
      (assoc-in k/booking-selected-time-slot nil)))

(defmethod fx/perform-effects e/flow|appointment-booking|initialized
  [_ _ _ _ state]
  (publish e/flow|appointment-booking|date-selected {:date (-> (date/now)
                                                               start-of-day
                                                               (date/add-delta {:days 2}))}))

(defmethod t/transition-state e/flow|appointment-booking|date-selected
  [_ _event {:keys [date] :as _args} state]
  (assoc-in state k/booking-selected-date date))

(defmethod t/transition-state e/flow|appointment-booking|time-slot-selected
  [_ _event {:keys [time-slot] :as _args} state]
  (assoc-in state k/booking-selected-time-slot time-slot))

(defmethod fx/perform-effects e/control-appointment-booking-week-chevron-clicked
  [_ _event {:keys [date] :as _args} _prev-state _state]
  (publish e/flow|appointment-booking|date-selected {:date date}))

(defmethod fx/perform-effects e/control-appointment-booking-time-clicked
  [_ _event {:keys [slot-id] :as _args} _prev-state _state]
  (publish e/flow|appointment-booking|time-slot-selected {:time-slot slot-id}))

(defmethod fx/perform-effects e/control-appointment-booking-submit-clicked
  [_ _event _args _prev-state _state]
  (publish e/flow|appointment-booking|done))

(defmethod fx/perform-effects e/flow|appointment-booking|done
  [_ _event _args _prev-state state]
  #?(:cljs
     (let [{:keys [number token]} (get-in state storefront.keypaths/order)
           slot-id                (get-in state k/booking-selected-time-slot)
           date                   (get-in state k/booking-selected-date)]
       (api/set-appointment-time-slot {:number  number
                                       :token   token
                                       :slot-id slot-id
                                       :date    date}))))

(defmethod fx/perform-effects e/control-appointment-booking-skip-clicked
  [_ _event _args _prev-state _state]
  (publish e/flow|appointment-booking|skipped))

(defmethod fx/perform-effects e/flow|appointment-booking|skipped
  [_ _event _args _prev-state state]
  #?(:cljs
     (history/enqueue-navigate (if (experiments/shopping-quiz-unified-fi? state)
                                 e/navigate-shopping-quiz-unified-freeinstall-match-success
                                 e/navigate-adventure-match-success))))

(defmethod fx/perform-effects e/api-success-set-appointment-time-slot
  [_ _event {:keys [order] :as _args} _prev-state state]
  (publish e/save-order {:order order})
  #?(:cljs
     (history/enqueue-navigate (if (experiments/shopping-quiz-unified-fi? state)
                                 e/navigate-shopping-quiz-unified-freeinstall-match-success
                                 e/navigate-adventure-match-success))))

(def time-slots
  [{:slot/id "08-to-11"
    :slot.picker/copy "8:00am - 11:00am"
    :slot.card/copy "8:00am"}
   {:slot/id "11-to-14"
    :slot.picker/copy "11:00am - 2:00pm"
    :slot.card/copy "11:00am"}
   {:slot/id "14-to-17"
    :slot.picker/copy "2:00pm - 5:00pm"
    :slot.card/copy "2:00pm"}])
