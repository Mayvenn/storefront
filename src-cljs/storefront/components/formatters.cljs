(ns storefront.components.formatters
  (:require [goog.string.format]
            [goog.string]))

(defn parse-iso8601 [date-string]
  (-> date-string
      (js/Date.parse)
      (js/Date.)))

(defn locale-date [iso8601-formatted-string]
  (-> iso8601-formatted-string
      parse-iso8601
      (.toLocaleDateString)))

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

(defn as-money [amount]
  (let [amount (js/parseFloat amount)
        format (if (< amount 0) "-$%1.2f" "$%1.2f")]
    (goog.string/format format (.toLocaleString (js/Math.abs amount)))))

(defn as-money-or-free [amount]
  (if (zero? amount)
    "FREE"
    (as-money amount)))

(defn as-money-without-cents [amount]
  (let [amount (int amount)
        format (if (< amount 0) "-$%s" "$%s")]
    (goog.string/format format (.toLocaleString (js/Math.abs amount)))))

(defn as-money-cents-only [amount]
  (let [amount (-> (js/parseFloat amount)
                   js/Math.abs
                   (* 100)
                   js/Math.round
                   (rem 100))]
    (goog.string/format "%02i" amount)))
