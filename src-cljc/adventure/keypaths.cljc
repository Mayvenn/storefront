(ns adventure.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def adventure [:adventure])

(def adventure-affiliate-stylist-id (conj adventure :affiliate-stylist-id))

(def adventure-home-video (conj adventure :home-video))

(def adventure-choices (conj adventure :choices))

(def adventure-stylist-gallery (conj adventure :stylist-gallery))
(def adventure-stylist-gallery-image-urls (conj adventure-stylist-gallery :image-urls))
(def adventure-stylist-gallery-image-index (conj adventure-stylist-gallery :image-index))

(def stylist-matching-ui (conj keypaths/ui :stylist-matching))
(def stylist-profile (conj stylist-matching-ui :stylist-profile))
(def stylist-profile-id (conj stylist-profile :id))

(def adventure-stylist-results-returned (conj adventure :stylist-results-returned))
