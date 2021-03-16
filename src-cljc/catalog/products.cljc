(ns catalog.products
  (:require api.orders
            [catalog.keypaths :as k]
            [storefront.accessors.experiments :as ff]
            [storefront.accessors.products :as accessors.products]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [catalog.skuers :as skuers]
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

(defn index-by [f coll]
  (persistent!
   (reduce (fn [acc item]
             (let [k (f item)]
               (assoc! acc k item)))
           (transient {})
           coll)))

(defn index-skus [skus]
  (->> skus
       (map skuers/->skuer)
       (index-by :catalog/sku-id)))

(defn index-products [products]
  (->> products
       (map skuers/->skuer)
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

(defn extract-product-skus
  [state product]
  (let [skus-db (get-in state keypaths/v2-skus)]
    (->> (:selector/skus product)
         (select-keys skus-db)
         vals
         (sort-by :sku/price))))
