(ns adventure.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def adventure [:adventure])

(def adventure-affiliate-stylist-id (conj adventure :affiliate-stylist-id))

(def adventure-home-video (conj adventure :home-video))

(def adventure-choices (conj adventure :choices))
(def adventure-choices-selected-stylist-id (conj adventure-choices :selected-stylist-id))

(def adventure-stylist-match-address (conj adventure :stylist-match-address))
(def adventure-stylist-match-location (conj adventure-choices :location))

(def adventure-stylist-gallery (conj adventure :stylist-gallery))
(def adventure-stylist-gallery-image-urls (conj adventure-stylist-gallery :image-urls))
(def adventure-stylist-gallery-image-index (conj adventure-stylist-gallery :image-index))

(def adventure-servicing-stylist (conj adventure :servicing-stylist))
(def adventure-servicing-stylist-service-menu (conj adventure-servicing-stylist :service-menu))

(def stylist-matching-ui (conj keypaths/ui :stylist-matching))

(def adventure-stylist-results-returned (conj adventure :stylist-results-returned))
