(ns adventure.keypaths)

(def adventure [:adventure])
(def adventure-choices (conj adventure :choices))
(def adventure-stylist-match-zipcode (conj adventure :stylist-match-zipcode))
(def adventure-stylist-match-location (conj adventure-choices :location))
