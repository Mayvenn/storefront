(ns storefront.components.formatters
  (:require [goog.string :as gstring]
            [goog.string.format]))

(defn locale-date [iso8601-formatted-string]
  (-> iso8601-formatted-string
      (js/Date.parse)
      (js/Date.)
      (.toLocaleDateString)))

(defn as-money [amount]
  (let [amount (js/parseFloat amount)
        format (if (< amount 0) "-$%1.2f" "$%1.2f")]
    (apply gstring/format format [(js/Math.abs amount)])))

(defn as-money-without-cents [amount]
  (let [amount (int amount)
        format (if (< amount 0) "-$%1f" "$%1f")]
    (apply gstring/format format [(js/Math.abs amount)])))
