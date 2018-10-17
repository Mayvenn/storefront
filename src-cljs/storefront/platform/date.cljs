(ns storefront.platform.date
  (:require [spice.date :as date]))

(defn current-date []
  (js/Date.))

(defn full-year [d]
  (.getFullYear d))

(defn weekday? [date]
  (let [weekdays #{1 2 3 4 5}]
    (contains? weekdays (.getDay date))))

(defn add-business-days [date ndays]
  (if (not (pos? ndays))
    date
    (let [next-day (date/add-delta date {:days 1})]
      (recur next-day
             (if (weekday? next-day)
               (dec ndays)
               ndays)))))
