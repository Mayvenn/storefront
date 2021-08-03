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

(s/def ::selected-date (s/nilable date/date?))
(s/def ::selected-time-slot (->> (map :slot/id time-slots)
                                 set
                                 s/nilable))
;; HACK: This is to allow UFI to display appointment booking page
;;       even if the order already has the appointment set.
(s/def ::done (s/nilable boolean?))

(s/def ::model (s/keys
                :opt [::selected-date
                      ::selected-time-slot
                      ::done]))

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
   (conform! ::model (get-in state model-keypath)))
  ([state key]
   (conform! key (get-in state (conj model-keypath key)))))

;; NOTE: This is to meet the standard concept shape
(def <- read-model)

(defn write-model
  ([state model]
   (assoc-in state model-keypath (conform! ::model model)))
  ([state key model]
   (assoc-in state (conj model-keypath key) (conform! key model))))

(defn start-of-day [date]
  #?(:cljs
     (doto date
       (.setHours 0)
       (.setMinutes 0)
       (.setSeconds 0)
       (.setMilliseconds 0))))


(defmethod t/transition-state e/biz|appointment-booking|initialized
  [_ _event _args state]
  (write-model state
      {::selected-date nil
       ::selected-time-slot nil
       ::done false}))

(defmethod fx/perform-effects e/biz|appointment-booking|initialized
[_ _ _args _ _state]
(publish e/biz|appointment-booking|date-selected {:date (-> (date/now)
                                                               start-of-day
                                                               (date/add-delta {:days 2}))}))

(defmethod t/transition-state e/biz|appointment-booking|date-selected
  [_ _event {:keys [date] :as _args} state]
  (write-model state ::selected-date date))

(defmethod t/transition-state e/biz|appointment-booking|time-slot-selected
  [_ _event {:keys [time-slot] :as _args} state]
  (write-model state ::selected-time-slot time-slot))

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
       (stringer/track-event "appointment-requested"
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
  (let [target (spice.core/spy (or (get choices decision)
                                   (get choices :success)))]
    #?(:cljs
       (if (routes/sub-page? [target nil] [e/navigate nil])
         (history/enqueue-navigate target)
         (publish target)))))

(defmethod fx/perform-effects e/api-success-set-appointment-time-slot
  [_ _event {:keys [order] :as _args} _prev-state _state]
  (publish e/save-order {:order order})
  (publish e/biz|appointment-booking|done))
