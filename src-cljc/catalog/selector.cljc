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
  (->> (apply query image-db criteria)
       (sort-by :order)))

(defprotocol Selection
  "Selects sku(er)s from a skuer"
  (essentials [this])
  (select-all [this])
  (select [this user-selections]))

(defrecord Selector [skuer identifier space]
  Selection
  (essentials [{:keys [skuer]}]
    (select-keys skuer
                 (:selector/essentials skuer)))
  (select-all [this]
    (->> (essentials this)
         (query space)))
  (select [this user-selections]
    (query space
           (merge user-selections
                  (essentials this)))))
