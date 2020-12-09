(ns storefront.utils
  (:require [spice.selector :refer [match-all]]))

;; TODO: move to spice
(defn ?update
  "Don't apply the update if the key doesn't exist. Prevents keys being added
  when they shouldn't be"
  [m k & args]
  (if (k m)
    (apply update m k args)
    m))

(defn ?assoc
  "Don't apply the assoc if the value is nil. Prevents keys being added when they shouldn't be"
  [m & args]
  {:pre [(even? (count args))]}
  (reduce
   (fn [m' [k v]]
     (cond-> m'
       (some? v)
       (assoc k v)))
   m
   (partition 2 args)))

(def select
  (partial match-all {:selector/strict? true}))
