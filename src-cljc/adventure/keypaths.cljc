(ns adventure.keypaths)

(def adventure [:adventure])
(def adventure-choices (conj adventure :choices))
(def adventure-matched-stylists (conj adventure :matched-stylists))
(def adventure-stylist-match-zipcode (conj adventure :stylist-match-zipcode))
(def adventure-stylist-match-location (conj adventure-choices :location))
(def adventure-matching-stylists-timer (conj adventure-choices :matching-stylists-timer))

(def adventure-selected-stylist-id (conj adventure :selected-stylist-id))

(def adventure-stylist-gallery (conj adventure :stylist-gallery))
(def adventure-stylist-gallery-index (conj adventure-stylist-gallery :index))
(def adventure-stylist-gallery-image-index (conj adventure-stylist-gallery :image-index))
(def adventure-stylist-gallery-open? (conj adventure-stylist-gallery :open?))

(def adventure-servicing-stylist (conj adventure :servicing-stylist))
