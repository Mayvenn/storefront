(ns catalog.products
  (:require [catalog.keypaths :as k]
            [clojure.set :as set]
            [spice.maps :as maps]
            [storefront.accessors.products :as accessors.products]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.transitions :refer [transition-state]]))

(defn product-by-id [app-state product-id]
  (get-in app-state (conj keypaths/v2-products product-id)))

(defn is-hair?
  [skuer]
  (some-> skuer :catalog/department (contains? "hair")))

(defn stylist-only?
  [skuer]
  (some-> skuer :catalog/department (contains? "stylist-exclusives")))

(defn eligible-for-reviews?
  [skuer]
  (and
   (not (accessors.products/product-is-mayvenn-install-service? skuer))
   (not (accessors.products/standalone-service? skuer))
   (not (stylist-only? skuer))))

(defn eligible-for-triple-bundle-discount?
  [skuer]
  (:promo.triple-bundle/eligible skuer
                                 (is-hair? skuer)))

(defn eligible-for-mayvenn-install-discount?
  [skuer]
  (:promo.mayvenn-install/eligible skuer
                                   (is-hair? skuer)))

(defn current-product [app-state]
  (product-by-id app-state
                 (get-in app-state k/detailed-product-id)))

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

(defn index-by [f coll]
  (persistent!
   (reduce (fn [acc item]
             (let [k (f item)]
               (assoc! acc k item)))
           (transient {})
           coll)))

(defn index-skus [skus]
  (->> skus
       (map ->skuer)
       (index-by :catalog/sku-id)))

(defn index-products [products]
  (->> products
       (map ->skuer)
       (index-by :catalog/product-id)))

(defmethod transition-state events/api-success-v3-products
  [_ event {:keys [products skus images] :as response} app-state]
  (-> app-state
      (update-in keypaths/v2-products merge (index-products products))
      (update-in keypaths/v2-skus merge skus) ;; TODO this should always be keyed by strings
      (update-in keypaths/v2-images merge images))) ;;  this should always be keyed by strings

(defn path-for-sku [product-id slug sku]
  (routes/path-for events/navigate-product-details
                   (merge {:catalog/product-id product-id
                           :page/slug          slug}
                          (when sku
                            {:query-params {:SKU sku}}))))

(defn extract-product-skus [app-state product]
  (->> (select-keys (get-in app-state keypaths/v2-skus)
                    (:selector/skus product))
       vals
       (sort-by :sku/price)))
