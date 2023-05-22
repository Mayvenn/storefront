(ns storefront.utils
  (:require [clojure.string :as string]
            #?@(:cljs
                (goog.crypt
                 goog.crypt.Sha256))))

;; TODO: move to spice
(defn ?update
  "Don't apply the update if the key doesn't exist. Prevents keys being added
  when they shouldn't be"
  [m k & args]
  (if (and (associative? m) (contains? m k))
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

(defn insert-at-pos
  [position i coll]
  (let [[h & r] (partition-all position coll)]
    (flatten (into [h] (concat [i] r)))))

(defn sha256< [message]
  #?(:clj nil
     :cljs (when (seq message)
             (let [sha256 (js/goog.crypt.Sha256.)]
               (->> message string/lower-case string/trim goog.crypt/stringToByteArray (.update sha256))
               (goog.crypt/byteArrayToHex (.digest sha256))))))