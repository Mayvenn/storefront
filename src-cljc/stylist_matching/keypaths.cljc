(ns stylist-matching.keypaths)

;; Stylist Matching model
(def stylist-matching [:models :stylist-matching])

(def address  (conj stylist-matching :param/address))
(def location (conj stylist-matching :param/location))
(def services (conj stylist-matching :param/services))
(def ids      (conj stylist-matching :param/ids))

(def stylist-results (conj stylist-matching :results/stylists))

(def status (conj stylist-matching :status))

;; Google location interaction (yes, thru keypaths)
(def google-input    (conj stylist-matching :google/input))
(def google-location (conj stylist-matching :google/location))

;; UI model
(def ui-stylist-matching-name-input [:ui :stylist-matching :name-input])
