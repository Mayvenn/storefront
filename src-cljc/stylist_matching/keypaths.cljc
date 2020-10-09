(ns stylist-matching.keypaths)

(def stylist-matching [:models :stylist-matching])

(def address  (conj stylist-matching :param/address))
(def location (conj stylist-matching :param/location))
(def services (conj stylist-matching :param/services))
(def ids      (conj stylist-matching :param/ids))

(def google-input    (conj stylist-matching :google/input))
(def google-location (conj stylist-matching :google/location))

(def stylist-results (conj stylist-matching :results/stylists))
