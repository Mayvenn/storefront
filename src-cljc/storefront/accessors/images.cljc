(ns storefront.accessors.images)

;; return images for a given product or sku
(defn for-skuer [images-catalog skuer]
  (or (not-empty ;; v3/products style - normalized
       (into []
             (keep (fn [[use-case order catalog-id]]
                     (some-> catalog-id
                             images-catalog
                             (assoc :use-case use-case
                                    :order order))))
             (:selector/image-cases skuer)))
      ;; GROT: v2/skus, ~~v2/products~~, shared-cart style - denormalized already
      (:selector/images skuer)))

(defn- matches-use-case? [use-case image]
  (= (:use-case image) use-case))

(defn skuer->image [images-catalog use-case skuer]
  (->> skuer
       (for-skuer images-catalog)
       (filter (partial matches-use-case? use-case))
       first))

(defn ^:private image-by-use-case [images-catalog use-case skuer]
  ;; TODO fix this!!! PLEASE!!! (should be using selector and doing something more clever than this.)
  (when-let [image (skuer->image images-catalog use-case skuer)]
    {:src (str (:url image) "-/format/auto/")
     :alt (str (:copy/title skuer))}))

(defn cart-image [images-catalog skuer]
  (image-by-use-case images-catalog "cart" skuer))
