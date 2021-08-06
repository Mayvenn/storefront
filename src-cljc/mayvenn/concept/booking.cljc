(ns mayvenn.concept.booking
  (:require
   api.orders
   [clojure.spec.alpha :as s]
   [storefront.effects :as fx]
   [storefront.transitions :as t]
   [storefront.events :as e]
   [storefront.routes :as routes]
   [storefront.keypaths :as keypaths]
   #?@(:cljs [[storefront.api :as api]
              [storefront.history :as history]
              [storefront.hooks.stringer :as stringer]
              storefront.frontend-trackings])
   [storefront.platform.messages
    :as messages
    :refer [handle-message] :rename {handle-message publish}]
   [mayvenn.concept.follow :as follow]


   [spice.date :as date]
   [spice.core :as spice]
   [storefront.trackings :as trk]
   [clojure.string :as string]))

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

(def model-keypath [:models :booking])
(def view-model-keypath [:ui :booking])

(s/def ::selected-date (s/nilable date/date?))
(s/def ::selected-time-slot (->> (map :slot/id time-slots)
                                 set
                                 s/nilable))
;; HACK: This is to allow UFI to display appointment booking page
;;       even if the order already has the appointment set.
(s/def ::done (s/nilable boolean?))

(s/def ::model (s/nilable (s/keys
                           :opt [::selected-date
                                 ::selected-time-slot
                                 ::done])))

(s/def ::week-idx (s/nilable int?))

(s/def ::earliest-available-date date/date?)
(s/def ::week (s/coll-of date/date?))
(s/def ::weeks (s/coll-of ::week))

;; TODO Find a better name and location for this.

(s/def ::view-model (s/nilable
                     (s/keys
                      :opt [::week-idx
                            ::weeks
                            ::earliest-available-date])))

(defn parse-date-in-client-tz [date-str]
  (let [[year idx-1-month day] (map spice/parse-int (string/split date-str "-"))
        idx-0-month            (dec idx-1-month)]
    #?(:cljs (js/Date. year idx-0-month day))))

(defn ^:private conform!
  [spec value]
  (let [result (s/conform spec value)]
    (if (= ::s/invalid result)
      (do
        #?(:cljs (js/console.log (s/explain-str spec value)))
        (throw (ex-info "Failing spec!" {:explaination (s/explain-str spec value)
                                         :value value
                                         :spec spec})))
      result)))

(defn read-model
  ([state]
   (get-in state model-keypath))
  ([state key]
   (get-in state (conj model-keypath key))))

(defn assoc-and-conform! [model spec key value]
  (conform! spec (assoc model key value)))

;;TODO Check order of operations on conform
(defn write-model
  ([state model]
   (assoc-in state model-keypath (conform! ::model model)))
  ([state key value]
   (update-in state model-keypath
              assoc-and-conform! ::model key value)))

;; NOTE: This is to meet the standard concept shape
(def <- read-model)

(defn read-view-model
  ([state]
   (get-in state view-model-keypath))
  ([state key]
   (get-in state (conj view-model-keypath key))))

(defn write-view-model
  ([state model]
   (assoc-in state view-model-keypath (conform! ::model model)))
  ([state key value]
   (update-in state view-model-keypath
              assoc-and-conform! ::view-model key value)))



(defn start-of-day [date]
  #?(:cljs
     (doto date
       (.setHours 0)
       (.setMinutes 0)
       (.setSeconds 0)
       (.setMilliseconds 0))))

(defn find-previous-or-this-sunday [today]
  (let [i     (date/weekday-index today)]
    (if (= 7 i) ;;if today is sunday
      today
      (date/add-delta today {:days (- i)}))))

(defn week-contains-date? [selected-date week]
  (first (filter #(= selected-date %) week)))

(defn get-week-idx-for-date [shown-weeks selected-date]
  (->> shown-weeks
       (keep-indexed (fn [idx week]
                       (when (week-contains-date? selected-date week)
                         idx)))
       first))

(defn get-weeks [starting-sunday]
  (let [num-of-shown-weeks 5 ;; NOTE: We want to show 4 weeks *after* the current date,
        ;;       for a total of 5 weeks.
        length-of-week     7]
    (partition length-of-week
               (map #(date/add-delta starting-sunday {:days %})
                    (range (* num-of-shown-weeks length-of-week))))))


(defmethod t/transition-state e/biz|appointment-booking|initialized
  [_ _event _args state]
  (let [earliest-available-date (-> (date/now)
                                    (start-of-day)
                                    (date/add-delta {:days 2}))
        weeks                   (-> earliest-available-date
                                    find-previous-or-this-sunday
                                    get-weeks)]
    (-> state
        (write-model {::selected-date      nil
                      ::selected-time-slot nil
                      ::done               false})
        (write-view-model {::weeks                   weeks
                           ::week-idx                0
                           ::earliest-available-date earliest-available-date}))))

(defmethod fx/perform-effects e/biz|appointment-booking|initialized
[_ _ _args _ state]
  (let [{:keys [date slot-id]} (some-> state
                                       api.orders/current
                                       :waiter/order
                                       :appointment-time-slot)
        order-date             (some-> date parse-date-in-client-tz)]
    (publish e/biz|appointment-booking|date-selected {:date order-date})
    (when slot-id
      (publish e/biz|appointment-booking|time-slot-selected {:time-slot slot-id}))))

(defmethod trk/perform-track e/biz|appointment-booking|initialized
  [_ _ _args state]
  (let [{::keys [selected-time-slot selected-date]} (read-model state)
        date-without-time (-> selected-date date/to-iso (string/split "T") first)]
    #?(:cljs
       (stringer/track-event "appointment_request-displayed"
                             {:date-requested date-without-time
                              :time-requested selected-time-slot}))))

(defmethod t/transition-state e/biz|appointment-booking|date-selected
  [_ _event {:keys [date] :as _args} state]
  (let [weeks (read-view-model state ::weeks)]
    (-> state
        (write-model ::selected-date date)
        (write-view-model ::week-idx (get-week-idx-for-date weeks date)))))

(defmethod t/transition-state e/biz|appointment-booking|time-slot-selected
  [_ _event {:keys [time-slot] :as _args} state]
  (-> state
      (write-model ::selected-time-slot time-slot)))

(defmethod fx/perform-effects e/biz|appointment-booking|submitted
  [_ _event _args _prev-state state]
  #?(:cljs
     (let [{:keys [number token]}                      (get-in state keypaths/order)
           {::keys [selected-time-slot selected-date]} (read-model state)]
       (api/set-appointment-time-slot {:number  number
                                       :token   token
                                       :slot-id selected-time-slot
                                       :date    selected-date}))))

(defmethod trk/perform-track e/biz|appointment-booking|submitted
  [_ _ _args state]
  (let [{::keys [selected-time-slot selected-date]} (read-model state)
        date-without-time (-> selected-date date/to-iso (string/split "T") first)]
    #?(:cljs
       (stringer/track-event "appointment_request-requested"
                             {:date-requested date-without-time
                              :time-requested selected-time-slot}))))

(defmethod fx/perform-effects e/biz|appointment-booking|skipped
  [_ _event _args _prev-state _state]
  (publish e/biz|appointment-booking|done))

(defmethod trk/perform-track e/biz|appointment-booking|skipped
  [_ _ _args _state]
  #?(:cljs
     (stringer/track-event "appointment_request-skipped")))



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
      (write-model ::done true)
      (follow/clear event)))

(defmethod fx/perform-effects e/biz|appointment-booking|navigation-decided
  [_ _event {:keys              [choices]
             {:keys [decision]} :follow/args} _prev-state _state]
  (let [target (or (get choices decision)
                   (get choices :success))]
    #?(:cljs
       (if (routes/sub-page? [target nil] [e/navigate nil])
         (history/enqueue-navigate target)
         (publish target)))))

(defmethod t/transition-state e/control-appointment-booking-caret-clicked
  [_ event {:keys [week-idx]} state]
  (-> state
      (write-view-model ::week-idx week-idx)))

(defmethod fx/perform-effects e/api-success-set-appointment-time-slot
  [_ _event {:keys [order] :as _args} _prev-state _state]
  (publish e/save-order {:order order})
  (publish e/biz|appointment-booking|done))

(defmethod fx/perform-effects e/api-success-remove-appointment-time-slot
  [_ _event {:keys [order] :as _args} _prev-state _state]
  (publish e/save-order {:order order}))
