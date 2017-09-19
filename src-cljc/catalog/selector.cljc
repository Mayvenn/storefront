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

(defn criteria->xf-query [criteria]
  (let [xforms (reduce (fn [xfs [key value]]
                         (conj xfs (filter (partial missing-contains-or-equal key value))))
                       []
                       criteria)]
    (apply comp xforms)))

(defn all [db]
  db)

(defn query [db & criteria]
  (time (when db
          (let [merged-criteria (reduce merge criteria)
                xf-items db
                xf-query (criteria->xf-query merged-criteria)
                xf-result (sequence xf-query xf-items)]
            xf-result))))

(defn new-db [coll]
  coll)

(defn images-matching-product [image-db product & criteria]
  (->> (apply query image-db
              (-> (:criteria/essential product)
                  (dissoc :hair/origin))
              criteria)
       (sort-by :order)))
