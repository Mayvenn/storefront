(ns stylist-matching.keypaths)

(def stylist-matching [:models :stylist-matching])

(def address  (conj stylist-matching :stylist-matching/address))
(def location (conj stylist-matching :stylist-matching/location))
(def services (conj stylist-matching :stylist-matching/services))
(def ids      (conj stylist-matching :stylist-matching/ids))

(def google-input    (conj stylist-matching :google/input))
(def google-location (conj stylist-matching :google/location))

(def stylist-results (conj stylist-matching :results/stylists))
