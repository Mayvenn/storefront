(ns storefront.components.money-formatters
  (:require [storefront.platform.numbers :as num]))

(defn warn-if-nil [amount]
  (when (nil? amount)
    (let [exception (ex-info "Attempting to use money formatter with nil value! Assuming 0 instead." {})]
      #?(:clj (throw exception)
         :cljs (try
                 ;;HACK(ellie+heather): We want to know *where* this happened,
                 ;;                     but we don't want to break rendering.
                 (throw exception)
                 (catch ExceptionInfo e
                   (js/console.error (.-stack e))
                   nil))))))

(defn number-with-commas [amount]
  (warn-if-nil amount)
  (->> (or amount 0)
       str
       reverse
       (interleave (concat [""] (cycle ["" "" ","])))
       reverse
       (apply str)))

(defn as-money-without-cents [amount]
  (warn-if-nil amount)
  (let [amount (num/parse-int (or amount 0))]
    (str (if (< amount 0) "-$" "$") (number-with-commas (num/abs amount)))))

(defn as-cents [amount]
  (warn-if-nil amount)
  (-> (or amount 0)
      num/parse-float
      num/abs
      (* 100)
      num/round))

(defn as-money-cents-only [amount]
  (warn-if-nil amount)
  (let [amount (-> (or amount 0) as-cents (rem 100))]
    (if (< amount 10)
      (str "0" amount)
      (str amount))))

(defn as-money [amount]
  (warn-if-nil amount)
  (let [amount (or amount 0)]
    (str (as-money-without-cents amount) "." (as-money-cents-only amount))))

(defn as-money-or-free [amount]
  (warn-if-nil amount)
  (let [amount (or amount 0)]
    (if (zero? amount)
      "FREE"
      (as-money amount))))

(defn as-money-without-cents-or-free [amount]
  (warn-if-nil amount)
  (let [amount (or amount 0)]
    (if (zero? amount)
      "FREE"
      (as-money-without-cents amount))))
