(ns catalog.skuers)

(defn essentials ;; TODO Name this better
  ([skuer]
   (essentials skuer skuer))
  ([skuer target]
   (select-keys target (:selector/essentials skuer))))

(defn electives ;; TODO Name this better
  ([skuer]
   (electives skuer skuer))
  ([skuer target]
   (select-keys target (:selector/electives skuer))))
