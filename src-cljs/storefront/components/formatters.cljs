(ns storefront.components.formatters
  (:require [goog.string :as gstring]
            [goog.string.format]))

(defn locale-date [iso8601-formatted-string]
  (-> iso8601-formatted-string
      (js/Date.parse)
      (js/Date.)
      (.toLocaleDateString)))

(defn float-as-money [amount]
  (apply gstring/format "$%1.2f" [(js/parseFloat amount)]))
