(ns appointment-booking.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]])
            api.orders
            api.stylist
            storefront.keypaths
            [spice.date :as date]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [appointment-booking.keypaths :as k]
            [mayvenn.concept.follow :as follow]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            [storefront.accessors.experiments :as experiments]
            clojure.set
            [storefront.transitions :as t]))

(defn start-of-day [date]
  #?(:cljs
     (doto date
       (.setHours 0)
       (.setMinutes 0)
       (.setSeconds 0)
       (.setMilliseconds 0))))

(defmethod fx/perform-effects e/navigate-adventure-appointment-booking
  [_ _event _ _ _state]
  (publish e/biz|follow|defined
           {:follow/start    [e/biz|appointment-booking|initialized]
            :follow/after-id e/biz|appointment-booking|done
            :follow/then     [e/biz|appointment-booking|navigation-decided
                              {:choices {:cart    e/navigate-cart
                                         :success e/navigate-adventure-match-success}}]}))


(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-appointment-booking
  [_ _event _ _ _state]
  (publish e/biz|follow|defined
           {:follow/start    [e/biz|appointment-booking|initialized]
            :follow/after-id e/biz|appointment-booking|done
            :follow/then     [e/biz|appointment-booking|navigation-decided
                              {:choices {:success e/navigate-shopping-quiz-unified-freeinstall-match-success}}]}))

(defmethod t/transition-state e/biz|appointment-booking|initialized
  [_ _event _args state]
  (-> state
      (assoc-in k/booking-selected-date nil)
      (assoc-in k/booking-selected-time-slot nil)
      (assoc-in k/booking-done false)))

(defmethod fx/perform-effects e/biz|appointment-booking|initialized
  [_ _ _args _ _state]
  (publish e/biz|appointment-booking|date-selected {:date (-> (date/now)
                                                               start-of-day
                                                               (date/add-delta {:days 2}))}))

(defmethod t/transition-state e/biz|appointment-booking|date-selected
  [_ _event {:keys [date] :as _args} state]
  (assoc-in state k/booking-selected-date date))

(defmethod t/transition-state e/biz|appointment-booking|time-slot-selected
  [_ _event {:keys [time-slot] :as _args} state]
  (assoc-in state k/booking-selected-time-slot time-slot))

(defmethod fx/perform-effects e/biz|appointment-booking|submitted
  [_ _event _args _prev-state state]
  #?(:cljs
     (let [{:keys [number token]} (get-in state storefront.keypaths/order)
           slot-id                (get-in state k/booking-selected-time-slot)
           date                   (get-in state k/booking-selected-date)]
       (api/set-appointment-time-slot {:number  number
                                       :token   token
                                       :slot-id slot-id
                                       :date    date}))))

(defmethod fx/perform-effects e/biz|appointment-booking|skipped
  [_ _event _args _prev-state _state]
  (publish e/biz|appointment-booking|done))

(defmethod fx/perform-effects e/api-success-set-appointment-time-slot
  [_ _event {:keys [order] :as _args} _prev-state _state]
  (publish e/save-order {:order order})
  (publish e/biz|appointment-booking|done))

(defmethod fx/perform-effects e/biz|appointment-booking|done
  [_ event _args prev-state state]
  (let [waiter-order (:waiter/order (api.orders/current state))]
    (follow/publish-all prev-state event {:decision (if (->> waiter-order
                                                             (api.orders/free-mayvenn-service (:servicing-stylist waiter-order))
                                                             :free-mayvenn-service/discounted?)
                                                      :cart
                                                      :success)})))

(defmethod t/transition-state e/biz|appointment-booking|done
  [_ event _args state]
  (-> state
      (assoc-in k/booking-done true)
      (follow/clear event)))

(defmethod fx/perform-effects e/biz|appointment-booking|navigation-decided
  [_ _event {:keys [choices]
            {:keys [decision]} :follow/args} _prev-state _state]
  (let [target (or (get choices decision)
                   (get choices :success))]
    #?(:cljs (history/enqueue-navigate target))))

;; Move to concept
(defn <- [state]
  (get-in state k/booking))

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
