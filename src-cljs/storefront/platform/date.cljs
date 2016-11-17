(ns storefront.platform.date)

(defn current-date []
  (js/Date.))

(defn full-year [d]
  (.getFullYear d))

(defn between [start end]
  (<= (.getTime (js/Date. start))
      (.getTime (current-date))
      (.getTime (js/Date. end))))
