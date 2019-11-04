(ns storefront.accessors.credit-cards
  (:require [clojure.string :as string]
            [storefront.platform.numbers :as numbers]
            [goog.string]))

(defn filter-cc-number-format [s]
  (some->> s numbers/digits-only (take 16)))

(defn format-cc-number [s]
  (->> s
       filter-cc-number-format
       (partition 4 4 nil)
       (map (partial apply str))
       (string/join " ")))

(defn parse-expiration [s]
  (->> s
       numbers/digits-only
       (split-at 2)
       (map (partial apply str))))

(defn format-expiration [s]
  (let [[month year] (parse-expiration s)
        filtered-str (str month year)]
    (cond
      (and (empty? month) (empty? year)) filtered-str
      (goog.string/endsWith s " /") (str month) ;; occurs when the user backspaces through the slash
      (and (> month 12) (empty? year)) (str "0" (get filtered-str 0) " / " (.substring filtered-str 1))
      (and (<= 10 month 12) (empty? year)) (str month " / ")
      (and (< 1 month 10) (not (goog.string/startsWith filtered-str "0"))) (str "0" month " / ")
      (and (= month 1) (goog.string/endsWith s " ")) (str "0" month " / ")
      (empty? year) (str month)
      :else (str month " / " year))))

(defn pad-year [y]
  (if (= 2 (count y))
    (str "20" y)
    y))

(def credit-card-brand->abbrev
  {"Discover"         "DISC "
   "Visa"             "VISA "
   "MasterCard"       "MC "
   "American Express" "AMEX "
   "Diners Club"      "DC "
   "JCB"              "JCB "
   "Unknown"          ""})

(defn display-credit-card [{:keys [brand last4 exp-month exp-year]}]
  (apply str (credit-card-brand->abbrev brand) "xxxx-xxxx-xxxx-" last4 " - Ex. " exp-month "/" (drop 2 (str exp-year))))
