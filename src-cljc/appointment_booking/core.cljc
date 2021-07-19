(ns appointment-booking.core
  (:require #?@(:cljs [[adventure.keypaths]
                       [storefront.api :as api]
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
            [spice.selector :as selector]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [stylist-matching.keypaths :as k]
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

;; Appointment Booking TODO consider moving to own namespace
;; (defpath flow|appointment-booking|initialized)
;; (defpath flow|appointment-booking|date-selected)
;; (defpath flow|appointment-booking|time-selected)
;; (defpath flow|appointment-booking|done)
;; (defpath flow|appointment-booking|skipped)

;; (defpath biz|appointment-booking|requested)

;; (defpath navigate-appointment-booking)

;; (defpath control-appointment-booking-week-left-chevron-clicked)
;; (defpath control-appointment-booking-week-right-chevron-clicked)
;; (defpath control-appointment-booking-date-clicked)
;; (defpath control-appointment-booking-time-clicked)
;; (defpath control-appointment-booking-submit-clicked)
;; (defpath control-appointment-booking-skip-clicked)

(defmethod fx/perform-effects e/navigate-adventure-appointment-booking
  [_ _ _ _ state]

  )
