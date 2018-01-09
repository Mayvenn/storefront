(ns storefront.components.formatters
  (:require [goog.string.format]
            [goog.string]))

(defn parse-iso8601 [date-string]
  (-> date-string
      (js/Date.parse)
      (js/Date.)))

(defn short-date [iso8601-formatted-string]
  (let [date (parse-iso8601 iso8601-formatted-string)]
    (goog.string/format "%d/%d/%d" (inc (.getMonth date)) (.getDate date) (.getFullYear date))))

(defn less-year-more-day-date [iso8601-formatted-string]
  (let [date (parse-iso8601 iso8601-formatted-string)]
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

(defn date->month-name [date]
  ;; This is actually the recommended way to do this in JavaScript.
  ;; The other option is to use a time library, but goog.i18n adds 500K to the
  ;; page size.
  (get month-names (.getMonth date)))

(defn long-date [date-string]
  (let [date (parse-iso8601 date-string)]
    (goog.string/format "%s %d, %d" (date->month-name date) (.getDate date) (.getFullYear date))))

(defn epoch-date [epoch]
  (-> (js/Date. epoch)
      (.toLocaleDateString)))

