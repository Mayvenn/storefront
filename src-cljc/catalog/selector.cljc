(ns catalog.selector
  (:require [datascript.core :as d]))

(defn ^:private ->clauses
  [m] (mapcat (fn [[k v]]
                (cond
                  (and (coll? v) (< 1 (count v)))
                  (let [v (set v)
                        sym (gensym "?v")]
                    [['?s k sym]
                     [`(~'contains? ~v ~sym)]])

                  (coll? v)
                  [['?s k (first v)]]

                  :else
                  [['?s k v]])) m))

(defn query [db & criteria]
  (let [query (->> criteria
                   (reduce merge)
                   ->clauses
                   (concat [:find '(pull ?s [*])]
                           (when (seq criteria) [:where])))]
    (some->> db
             (d/q query)
             (map first))))

(defn all [db]
  (some->> db
           (d/q '[:find (pull ?s [*])
                 :where [?s]])
           (map first)))

(defn new-db [coll]
  (d/db-with (d/empty-db) coll))

(defn images-matching-product [image-db product & criteria]
  (->> (apply query image-db
              (dissoc (:criteria/essential product)
                      :hair/origin)
              criteria)
       (sort-by :order)))
