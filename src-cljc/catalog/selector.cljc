(ns catalog.selector
  (:require [datascript.core :as d]))

(defn ^:private ->clauses
  [m] (mapv (fn [[k v]] ['?s k v]) m))

;; TODO selector/query should understand sets as values
(defn query [db & criteria]
  (let [query (->> criteria
                   (reduce merge)
                   ->clauses
                   (concat [:find '(pull ?s [*])
                            :where]))]
    (->> db
         (d/q query)
         (map first))))

(defn skus-db [skus]
  (d/db-with (d/empty-db) skus))
