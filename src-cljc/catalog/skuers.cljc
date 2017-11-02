(ns catalog.skuers)

(defn essentials [skuer]
  (select-keys skuer (:selector/essentials skuer)))
