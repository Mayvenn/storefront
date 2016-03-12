(ns storefront.components.formatters
  (:require [goog.string :as gstring]
            [goog.string.format]))

(defn locale-date [iso8601-formatted-string]
  (-> iso8601-formatted-string
      (js/Date.parse)
      (js/Date.)
      (.toLocaleDateString)))

(defn epoch-date [epoch]
  (-> (js/Date. epoch)
      (.toLocaleDateString)))

(defn as-money [amount]
  (let [amount (js/parseFloat amount)
        format (if (< amount 0) "-$%1.2f" "$%1.2f")]
    (gstring/format format (.toLocaleString (js/Math.abs amount)))))

(defn as-money-or-free [amount]
  (if (zero? amount)
    "FREE"
    (as-money amount)))

(defn as-money-without-cents [amount]
  (let [amount (int amount)
        format (if (< amount 0) "-$%s" "$%s")]
    (gstring/format format (.toLocaleString (js/Math.abs amount)))))

(defn as-money-cents-only [amount]
  (let [amount (-> (js/parseFloat amount)
                   js/Math.abs
                   (* 100)
                   (rem 100)
                   int)]
    (gstring/format "%02i" amount)))
