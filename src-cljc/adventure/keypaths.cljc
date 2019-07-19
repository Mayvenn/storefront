(ns adventure.keypaths)

(def adventure [:adventure])
(def adventure-affiliate-stylist-id (conj adventure :affiliate-stylist-id))
(def adventure-from-shop-to-freeinstall? (conj adventure :from-shop-to-freeinstall?))
(def adventure-random-sequence (conj adventure :random-sequence))
(def adventure-home-video (conj adventure :home-video))
(def adventure-choices (conj adventure :choices))
(def adventure-choices-flow (conj adventure-choices :flow))
(def adventure-choices-color (conj adventure-choices :color))
(def adventure-choices-how-shop (conj adventure-choices :how-shop))
(def adventure-choices-install-type (conj adventure-choices :install-type))
(def adventure-choices-selected-stylist-id (conj adventure-choices :selected-stylist-id))
(def adventure-matched-stylists (conj adventure :matched-stylists))
(def adventure-stylist-match-address (conj adventure :stylist-match-address))
(def adventure-stylist-match-location (conj adventure-choices :location))
(def adventure-matching-stylists-timer (conj adventure-choices :matching-stylists-timer))

(def adventure-matching-skus-color (conj adventure :matching-skus-color))

(def adventure-stylist-gallery (conj adventure :stylist-gallery))
(def adventure-stylist-gallery-image-urls (conj adventure-stylist-gallery :image-urls))
(def adventure-stylist-gallery-image-index (conj adventure-stylist-gallery :image-index))

(def adventure-servicing-stylist (conj adventure :servicing-stylist))

(def adventure-geocode-address (conj adventure :geocode-address))
