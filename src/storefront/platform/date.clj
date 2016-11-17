(ns storefront.platform.date
  (:import [java.util Calendar]))

(defn current-date []
  (Calendar/getInstance))

(defn full-year [d]
  (.get d Calendar/YEAR))

(defn between [start end]
  (<= (.toEpochMilli (.toInstant (java.time.OffsetDateTime/parse start)))
      (.getTimeInMillis (current-date))
      (.toEpochMilli (.toInstant (java.time.OffsetDateTime/parse end)))))
