(ns catalog.selector
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [spice.maps :as maps]
            [spice.selector :as selector]))

;; TODO(jjw, jjh): Decommission this namespace after cellar deploy

(defn- contains-or-equal [key search-value item]
  (let [item-value (get item key :query/missing)]
    (cond
      (and (coll? item-value) (coll? search-value))
      (seq (set/intersection (set item-value) (set search-value)))

      (and (coll? search-value) (> (count search-value) 1))
      ((set search-value) item-value)

      (coll? search-value) ;; Singleton set
      (= item-value (first search-value))

      :else
      (= item-value search-value))))

(defn- missing-contains-or-equal [key search-value item]
  (let [item-value (get item key :query/missing)]
    ;; matches items that don't have the property (key) at all
    (cond
      (= item-value :query/missing)
      true

      (and (coll? item-value) (coll? search-value))
      (seq (set/intersection (set item-value) (set search-value)))

      (and (coll? search-value) (> (count search-value) 1))
      ((set search-value) item-value)

      (coll? search-value) ;; Singleton set
      (= item-value (first search-value))

      :else
      (= item-value search-value))))

(defn- criteria->strict-query [criteria]
  (let [xforms (map (fn [[key value]]
                      (filter (partial contains-or-equal key value)))
                    criteria)]
    (apply comp xforms)))

(defn- criteria->query [criteria]
  (let [xforms (map (fn [[key value]]
                      (filter (partial missing-contains-or-equal key value)))
                    criteria)]
    (apply comp xforms)))

(defn query [coll & criteria]
  (if-let [merged-criteria (reduce merge {} criteria)]
    (sequence (criteria->query merged-criteria) coll)
    coll))

(defn strict-query [coll & criteria]
  (if-let [merged-criteria (reduce (partial merge-with set/union) {} criteria)]
    (sequence (criteria->strict-query merged-criteria) coll)
    coll))

(defn select [coll skuer & criteria]
  (apply query coll (select-keys skuer (:selector/essentials skuer)) criteria))

(defn- use-case-then-order-key [img]
  [(condp = (:use-case img)
     "seo"      0
     "carousel" 1
     2)
   (:order img)])

(defn seo-image [skuer]
  (->> (selector/match-essentials skuer (:selector/images skuer))
       (sort-by use-case-then-order-key)))

;; Using a set finds equality or a subset
(deftest selector
  (testing "querying with set"
    (is (= '({:a #{"3"}})
           (query [{:a #{"1"}} {:a #{"2"}} {:a #{"3"}} {:a #{"2" "3"}}]
                  {:a #{"3"}})))

    (is (= '({:a "3"})
           (query [{:a "1"} {:a "2"} {:a "3"}]
                  {:a #{"3"}})))

    (testing "finds only a match or subset"
      (is (= '({:a #{"2"}} {:a #{"3" "2"}})
             (query [{:a #{"1"}} {:a #{"2"}} {:a #{"3" "2"}}]
                    {:a #{"3" "2"}})))))

  (testing "matching with a value"
    (testing "values don't match against a set"
      (is (empty?
           (query [{:a #{"1"}} {:a #{"2"}} {:a #{"3" "2"}}]
                  {:a "3"})))

      (is (empty?
           (query [{:a #{"1"}} {:a #{"2"}} {:a #{"3" "2"}}]
                  {:a "3"})))
      (is (empty?
           (query [{:a #{"1"}} {:a #{"2"}} {:a #{"3"}}]
                  {:a "3"}))))

    (testing "matches on equality"
      (is (= '({:a "3"})
             (query [{:a "1"} {:a "2"} {:a "3"}]
                    {:a "3"}))))

    (testing "includes matches that have data that wasn't searched for"
      (is (= '({:a "1" :b "hey"})
             (query [{:a "1" :b "hey"} {:a "2"} {:a "3"}]
                    {:a "1"}))))))
