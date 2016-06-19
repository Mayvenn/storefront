(ns storefront.platform.date)

(defn current-date []
  (js/Date.))

(defn full-year [d]
  (.getFullYear d))
