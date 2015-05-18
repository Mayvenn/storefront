(ns storefront.components.formatters
  (:require [goog.string :as gstring]
            [goog.string.format]))

(defn locale-date [iso8601-formatted-string]
  (-> iso8601-formatted-string
      (js/Date.parse)
      (js/Date.)
      (.toLocaleDateString)))

(defn float-as-money [amount & {:keys [cents] :or {cents true}}]
  (let [money-format (if cents "$%1.2f" "$%1f")]
    (apply gstring/format money-format [(js/parseFloat amount)])))
