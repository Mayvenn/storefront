(ns adventure.keypaths)

(def adventure [:adventure])
(def adventure-random-sequence (conj adventure :random-sequence))
(def adventure-home-video (conj adventure [:home-video]))
(def adventure-choices (conj adventure :choices))
(def adventure-choices-how-shop (conj adventure-choices :how-shop))
(def adventure-choices-install-type (conj adventure-choices :install-type))
(def adventure-matched-stylists (conj adventure :matched-stylists))
(def adventure-stylist-match-address (conj adventure :stylist-match-address))
(def adventure-stylist-match-location (conj adventure-choices :location))
(def adventure-matching-stylists-timer (conj adventure-choices :matching-stylists-timer))

(def adventure-selected-stylist-id (conj adventure :selected-stylist-id))

(def adventure-stylist-gallery (conj adventure :stylist-gallery))
(def adventure-stylist-gallery-index (conj adventure-stylist-gallery :index))
(def adventure-stylist-gallery-image-index (conj adventure-stylist-gallery :image-index))
(def adventure-stylist-gallery-open? (conj adventure-stylist-gallery :open?))

(def adventure-servicing-stylist (conj adventure :servicing-stylist))
