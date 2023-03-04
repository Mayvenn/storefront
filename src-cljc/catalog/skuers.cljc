(ns catalog.skuers
  (:require [spice.maps :as maps]))

(defn essentials<
  ([skuer]
   (essentials< skuer skuer))
  ([skuer target]
   (select-keys target (:selector/essentials skuer))))

(defn electives<
  ([skuer]
   (electives< skuer skuer))
  ([skuer target]
   (select-keys target (:selector/electives skuer))))

(defn normalize-image [image]
  ;; PERF(jeff): this is called for every product in category pages, which can
  ;; be many times.
  (if (nil? (:criteria/attributes image))
    image ;; assume cellar has done this work for us (technically, image = [use-case order catalog/image-id])
    (let [img (-> (transient image)
                  (assoc! :id (str (:use-case image) "-" (:url image))
                          :order (or (:order image)
                                     (case (:image/of (:criteria/attributes image))
                                       "model"   1
                                       "product" 2
                                       "seo"     3
                                       "catalog" 4
                                       5))))]
      (persistent!
       (dissoc!
        (reduce (fn [acc [k v]]
                  (assoc! acc k v))
                img
                (:criteria/attributes image))
        :criteria/attributes :filename)))))

(defn ->skuer [value]
  (let [value (-> value
                  (update :selector/electives (partial mapv keyword))
                  (update :selector/essentials (partial mapv keyword))
                  (update :selector/images (partial mapv normalize-image)))
        ks (into (:selector/essentials value)
                 (:selector/electives value))]
    (->> (select-keys value ks)
         (maps/map-values set)
         (merge value))))

#_(defn skus->skuer 
  "Given a list of skus, return the skuer that contains all of them."
  [skus]
  
  
  )