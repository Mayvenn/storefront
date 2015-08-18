(ns storefront.accessors.states)

(defn abbr->id [states abbr]
  (some #(when (= (:abbr %) abbr) (:id %)) states))

(defn id->abbr [states id]
  (some #(when (= (:id %) id) (:abbr %)) states))
