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

(defn skus->skuers
  "Given a list of skus, group the skus into skuers according to a supplied list of dimensions."
  ([dimensions skus]
   (skus->skuers dimensions skus nil))
  ;; This arity is for when we need a matching product for title, product-id, and slug
  ([dimensions skus products]
   (->> skus
        (reduce (fn [acc sku]
                  (let [ix (select-keys sku dimensions)]
                    (if-let [skuer (get acc ix)]
                      (assoc acc ix (-> skuer
                                        (update :selector/skus conj (:catalog/sku-id sku))
                                        (update :selector/image-cases conj (:selector/image-cases sku))))
                      (assoc acc ix (merge (select-keys sku (concat dimensions
                                                                    [:selector/image-cases]))
                                           (when products
                                             (let [product (->> products (filter #(get (set (:selector/skus %)) (:catalog/sku-id sku))) first)]
                                               (select-keys product [:copy/title :catalog/product-id :page/slug])))
                                           {:selector/skus [(:catalog/sku-id sku)]}))))) {})
        vals)))
