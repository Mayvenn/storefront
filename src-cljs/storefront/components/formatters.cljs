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

(defn number-with-commas [n]
  (->> (str n)
       reverse
       (interleave (concat [""] (cycle ["" "" ","])))
       reverse
       (apply str)))

(defn as-money-without-cents [amount]
  (let [amount (int amount)
        format (if (< amount 0) "-$%s" "$%s")]
    (goog.string/format format (number-with-commas (js/Math.abs amount)))))

(defn as-money-cents-only [amount]
  (let [amount (-> (js/parseFloat amount)
                   js/Math.abs
                   (* 100)
                   js/Math.round
                   (rem 100))]
    (goog.string/format "%02i" amount)))

(defn as-money [amount]
  (str (as-money-without-cents amount) "." (as-money-cents-only amount)))

(defn as-money-or-free [amount]
  (if (zero? amount)
    "FREE"
    (as-money amount)))

(defn as-money-without-cents-or-free [amount]
  (if (zero? amount)
    "FREE"
    (as-money-without-cents amount)))
