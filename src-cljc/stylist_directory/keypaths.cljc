(ns stylist-directory.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def stylist-directory [:stylist-directory])

(def stylists (conj stylist-directory :stylists))
(def paginated-reviews (conj stylist-directory :paginated-reviews))

(def ui (conj keypaths/ui :stylist-directory))
(def stylist-search (conj ui :stylist-search))
(def stylist-search-address-input (conj stylist-search :address-input))
(def stylist-search-selected-location (conj stylist-search :selected-location))
