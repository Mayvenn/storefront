(ns catalog.skuers
  (:require [clojure.string :as string]
            [spice.maps :as maps]))

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
  [dimensions skus]
  (->> skus
       (reduce (fn [acc sku]
                  ;; TODO(jjh): generate slug better by ordering dimensions
                 (let [slug (->> (select-keys sku dimensions) vals (map first) (string/join "-"))]
                   (if-let [skuer (get acc slug)]
                     (assoc acc slug (-> skuer
                                         (update :selector/skus conj (:catalog/sku-id sku))
                                         (update :selector/image-cases conj (:selector/image-cases sku))))
                     (assoc acc slug (assoc (select-keys sku (concat dimensions
                                                                     [:selector/from-products
                                                                      :selector/image-cases]))
                                            :selector/skus [(:catalog/sku-id sku)]
                                            :copy/title slug ; TODO(jjh): generate better title
                                            ;; TODO(jjh): get product id by querying catalog
                                            :catalog/product-id (first (:selector/from-products sku))
                                            :page/slug slug))))) {})
       vals
       ;; TODO(jjh): sort properly
       (sort-by :copy/title)))
