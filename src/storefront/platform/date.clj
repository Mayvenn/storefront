(ns storefront.platform.date
  (:import [java.util Calendar]))

(defn current-date []
  (Calendar/getInstance))

(defn full-year [d]
  (.get d Calendar/YEAR))
