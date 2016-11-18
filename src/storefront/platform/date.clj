(ns storefront.platform.date
  (:import [java.util Calendar]))

(defn current-date []
  (Calendar/getInstance))

(defn full-year [d]
  (.get d Calendar/YEAR))

(defn between [start-iso8601 end-iso8601]
  (<= (.toEpochMilli (.toInstant (java.time.OffsetDateTime/parse start-iso8601)))
      (.getTimeInMillis (current-date))
      (.toEpochMilli (.toInstant (java.time.OffsetDateTime/parse end-iso8601)))))

(defn after [start-iso8601]
  (<= (.toEpochMilli (.toInstant (java.time.OffsetDateTime/parse start-iso8601)))
      (.getTimeInMillis (current-date))))

(def black-friday "2016-11-24T21:00:00.000-08:00")
