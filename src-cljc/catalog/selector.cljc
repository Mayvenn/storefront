(ns catalog.selector)

(defn missing-contains-or-equal [key value item]
  (let [item-value (key item :query/missing)]
    (cond
      (= item-value :query/missing)
      true

      (and (coll? value) (< 1 (count value)))
      ((set value) item-value)

      (coll? value)
      (= item-value (first value))

      :else
      (= item-value value))))

(defn criteria->query [criteria]
  (let [xforms (map (fn [[key value]]
                      (filter (partial missing-contains-or-equal key value)))
                    criteria)]
    (apply comp xforms)))

(defn query [coll & criteria]
  (if-let [merged-criteria (reduce merge criteria)]
    (sequence (criteria->query merged-criteria) coll)
    coll))

(defn select [coll skuer & criteria]
  (apply query coll (select-keys skuer (:selector/essentials skuer)) criteria))

(defn images-matching-product [image-db product & criteria]
  (->> (apply query image-db
              (-> (:criteria/essential product)
                  (dissoc :hair/origin))
              criteria)
       (sort-by :order)))
