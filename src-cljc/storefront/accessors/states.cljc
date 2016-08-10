(ns storefront.accessors.states)

(defn abbr->id [states abbr]
  (some #(when (= (:abbr %) abbr) (:id %)) states))
