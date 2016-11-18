(ns storefront.platform.date)

(defn current-date []
  (js/Date.))

(defn full-year [d]
  (.getFullYear d))

(defn between [start-iso8601 end-iso8601]
  (<= (.getTime (js/Date. start-iso8601))
      (.getTime (current-date))
      (.getTime (js/Date. end-iso8601))))

(defn after [start-iso8601]
  (<= (.getTime (js/Date. start-iso8601))
      (.getTime (current-date))))

(def black-friday "2016-11-24T21:00:00.000-08:00")
