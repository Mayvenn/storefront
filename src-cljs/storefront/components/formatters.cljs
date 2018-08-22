(ns storefront.components.formatters
  (:require [spice.date :as date]
            [goog.string.format]
            [goog.string]))

(defn short-date [date-like]
  (when-let [date (date/to-datetime date-like)]
    (goog.string/format "%d/%d/%d" (inc (.getMonth date)) (.getDate date) (.getFullYear date))))

(defn less-year-more-day-date [date-like]
  (when-let [date (date/to-datetime date-like)]
    (goog.string/format "%02d/%02d/%d" (inc (.getMonth date)) (.getDate date) (mod (.getFullYear date) 100))))

(def month-names ["January"
                  "February"
                  "March"
                  "April"
                  "May"
                  "June"
                  "July"
                  "August"
                  "September"
                  "October"
                  "November"
                  "December"])

(def month-abbr ["Jan"
                 "Feb"
                 "Mar"
                 "Apr"
                 "May"
                 "June"
                 "July"
                 "Aug"
                 "Sept"
                 "Oct"
                 "Nov"
                 "Dec"])

(defn date->month-name [date]
  ;; This is actually the recommended way to do this in JavaScript.
  ;; The other option is to use a time library, but goog.i18n adds 500K to the
  ;; page size.
  (get month-names (.getMonth date)))

(defn date->month-abbr [date]
  ;; This is actually the recommended way to do this in JavaScript.
  ;; The other option is to use a time library, but goog.i18n adds 500K to the
  ;; page size.
  (get month-abbr (.getMonth date)))

(defn long-date [date-like]
  (let [date (date/to-datetime date-like)]
    (goog.string/format "%s %d, %d" (date->month-name date) (.getDate date) (.getFullYear date))))

(defn abbr-date [date-like]
  (let [date (date/to-datetime date-like)]
    (goog.string/format "%s %d, %d" (date->month-abbr date) (.getDate date) (.getFullYear date))))

(defn epoch-date [date-like]
  (-> (date/to-datetime date-like)
      (.toLocaleDateString)))

(defn date-tuple [date-like]
  (let [date (date/to-datetime date-like)]
    [(.getFullYear date) (.getMonth date) (.getDate date)]))
