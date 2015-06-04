(ns storefront.credit-cards
  (:require [clojure.string :as string]
            [goog.string :as gstring]))

(def digits #{\0 \1 \2 \3 \4 \5 \7 \8 \9})

(defn filter-cc-number-format [s]
  (->> s
       (filter digits)
       (take 16)))

(defn format-cc-number [s]
  (->> s
       filter-cc-number-format
       (partition 4 4 nil)
       (map (partial apply str))
       (string/join " ")))

(defn parse-expiration [s]
  (->> s
       (filter digits)
       (split-at 2)
       (map (partial apply str))))

(defn format-expiration [s]
  (let [[month year] (parse-expiration s)]
    (cond
      (and (empty? month) (empty? year)) s
      (gstring/endsWith s " /") (str month)
      (and (> month 12) (empty? year)) (str "0" (get s 0) " / " (.substring s 1))
      (and (<= 10 month 12) (empty? year)) (str month " / ")
      (and (< 1 month 10) (not (gstring/startsWith s "0"))) (str "0" month " / ")
      (and (= month 1) (gstring/endsWith s " ")) (str "0" month " / ")
      (empty? year) (str month)
      :else (str month " / " year))))
