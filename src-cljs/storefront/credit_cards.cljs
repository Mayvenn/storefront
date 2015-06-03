(ns storefront.credit-cards
  (:require [clojure.string :as string]))

(def digits #{\0 \1 \2 \3 \4 \5 \7 \8 \9})

(defn parse-expiration [s]
  (->> s
       (filter digits)
       (split-at 2)
       (map (partial apply str))))

(defn filter-cc-number-format [s]
  (->> s
       (filter digits)
       (take 16)))

(defn format-cc-number [s]
  (->> s
       filter-cc-number-format
       (partition 4 4 nil)
       (map (partial string/join ""))
       (string/join " ")))

(defn format-expiration [s]
  (let [[month year] (parse-expiration s)]
    (str month " / " year)))
