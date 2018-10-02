(ns catalog.images)

(defn ucare-id [use-case skuer]
  (->> skuer
       :selector/images
       (filter #(= (:use-case %) use-case))
       first
       :url
       (re-find #"/([^/]+)/$")
       second))

(defn image [use-case skuer]
  {:ucare/id (ucare-id use-case skuer)})
