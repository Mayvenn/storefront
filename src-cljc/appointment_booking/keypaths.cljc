(ns appointment-booking.keypaths)


(def booking [:booking])
(def booking-selected-date (conj booking :selected-date))
(def booking-selected-time-slot (conj booking :selected-time-slot))

(def booking-finish-target (conj booking :finish-target))

(def booking-state (conj booking :state))
(def booking-state-skipped (conj booking-state :skipped))
