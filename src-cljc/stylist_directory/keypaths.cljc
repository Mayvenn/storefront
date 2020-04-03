(ns stylist-directory.keypaths
  (:require [storefront.keypaths :as keypaths]))

(def stylist-directory [:stylist-directory])

(def stylists (conj stylist-directory :stylists))
(def paginated-reviews (conj stylist-directory :paginated-reviews))

(def ui (conj keypaths/ui :stylist-directory))
(def stylist-search (conj ui :stylist-search))
(def stylist-search-selected-filters (conj stylist-search :selected-filters))
(def stylist-search-address-input (conj stylist-search :address-input))
(def stylist-search-selected-location (conj stylist-search :selected-location))
(def stylist-search-show-filters? (conj stylist-search :show-filters?))

(def user-toggled-preference (conj ui :user-toggled-preference))
