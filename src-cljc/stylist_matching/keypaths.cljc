(ns stylist-matching.keypaths
  (:refer-clojure :exclude [name]))

;; Stylist Matching model
(def stylist-matching [:models :stylist-matching])

;; What searches and presearches are ready

(def status (conj stylist-matching :status))

;; Stylist Search

(def address  (conj stylist-matching :param/address))
(def location (conj stylist-matching :param/location))
(def services (conj stylist-matching :param/services))
(def ids      (conj stylist-matching :param/ids))
(def name     (conj stylist-matching :param/name))

(def stylist-results (conj stylist-matching :results/stylists))

;; Stylist Profile Selection

;; Presearching

;; Our matching domain includes params that are 'presearched'
;; before using them in the main search.
;; This enables features like validating inputs and autocomplete
(def presearch-name (conj stylist-matching :presearch/name))

(def name-presearch-results (conj stylist-matching :results.presearch/name))

;; Google location interaction (yes, thru keypaths)
(def google-input         (conj stylist-matching :google/input))
(def address-field-errors (conj stylist-matching :address-field/errors))
(def google-location      (conj stylist-matching :google/location))

;; UI model
(def ui-stylist-matching-name-input [:ui :stylist-matching :name-input])

;; TODO(corey) The name param and the location param behave in a similar
;; fashion.
;; Symmetries:
;; - There is a presearch before the param is 'really' used.
;; - UI inputs need to do some setup on mount
;; - keypaths might be stored in matching model (google is already)
;; - results could be handled similarly

(def top-stylist-rejected (conj stylist-matching :ui/top-stylist-rejected))
