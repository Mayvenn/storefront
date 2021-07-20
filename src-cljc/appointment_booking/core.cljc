(ns appointment-booking.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.frontend-effects :as ffx]
                       [storefront.frontend-trackings :as ftx]
                       [storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.request-keys :as request-keys]])
            [api.catalog :refer [select ?base ?service ?physical ?discountable-install]]
            api.orders
            api.stylist
            [mayvenn.concept.follow :as follow]
            storefront.keypaths
            [spice.maps :as maps]
            [spice.date :as date]
            [spice.selector :as selector]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [appointment-booking.keypaths :as k]
            [stylist-matching.search.accessors.filters :as filters]
            [storefront.accessors.orders :as accessors.orders]
            [storefront.trackings :as trackings]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            clojure.set
            [clojure.string :as string]
            [storefront.transitions :as t]
            [storefront.accessors.experiments :as experiments]))

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

(defmethod fx/perform-effects e/flow|appointment-booking|initialized
  [_ _ _ _ state]
  (publish e/flow|appointment-booking|date-selected {:date (-> (date/now)
                                                               start-of-day
                                                               (date/add-delta {:days 2}))}))

(defmethod t/transition-state e/flow|appointment-booking|date-selected
  [_ _event {:keys [date] :as _args} state]
  (assoc-in state k/booking-selected-date date))

(defmethod fx/perform-effects e/control-appointment-booking-week-chevron-clicked
  [_ _event {:keys [date] :as _args} _prev-state state]
  (publish e/flow|appointment-booking|date-selected {:date date}))

(defmethod fx/perform-effects e/control-appointment-booking-time-clicked
  [_ _event {:keys [slot-id] :as _args} _prev-state state]
  (publish e/flow|appointment-booking|time-selected {:time-slot slot-id}))
