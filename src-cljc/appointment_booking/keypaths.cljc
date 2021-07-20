(ns appointment-booking.keypaths)


(def booking [:booking])
(def booking-selected-date (conj booking :selected-date))
(def booking-selected-time-slot (conj booking :selected-time-slot))
(def booking-earliest-available-date (conj booking :earliest-available-date))
