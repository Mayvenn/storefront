(ns storefront.components.money-formatters
  (:require [storefront.platform.numbers :as num]
            [storefront.platform.strings :as str]))

(defn number-with-commas [n]
  (->> (str n)
       reverse
       (interleave (concat [""] (cycle ["" "" ","])))
       reverse
       (apply str)))

(defn as-money-without-cents [amount]
  (let [amount (num/parse-int amount)
        format (if (< amount 0) "-$%s" "$%s")]
    (str/format format (number-with-commas (num/abs amount)))))

(defn as-cents [amount]
  (-> (num/parse-float amount)
      num/abs
      (* 100)
      num/round))

(defn as-money-cents-only [amount]
  (let [amount (-> amount as-cents (rem 100))]
    (str/format "%02i" amount)))

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
