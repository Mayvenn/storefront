(ns catalog.images)

(defn ucare-uri->ucare-id
  [uri]
  (second (re-find #"/([^/]+)/$" uri)))

(defn ucare-id [use-case skuer]
  (->> skuer
       :selector/images
       (filter #(= (:use-case %) use-case))
       first
       :url
       ucare-uri->ucare-id))

(defn image [use-case skuer]
  {:ucare/id (ucare-id use-case skuer)})
