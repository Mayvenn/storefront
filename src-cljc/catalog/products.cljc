(ns catalog.products
  (:require [catalog.keypaths :as k]
            [clojure.set :as set]
            [spice.maps :as maps]
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

(def eligible-for-reviews? (complement stylist-only?))

(defn eligible-for-triple-bundle-discount?
  [skuer]
  (:promo.triple-bundle/eligible skuer
                                (is-hair? skuer)))

(defn current-product [app-state]
  (product-by-id app-state
                 (get-in app-state k/detailed-product-id)))

(defn normalize-image [image]
  (-> image
      (assoc :id (str (:use-case image) "-" (:url image)))
      (assoc :order (or (:order image)
                        (case (:image/of (:criteria/attributes image))
                          "model"   1
                          "product" 2
                          "seo"     3
                          "catalog" 4
                          5)))
      (merge (:criteria/attributes image))
      (dissoc :criteria/attributes :filename)))

(defn ->skuer [value]
  (let [value (-> value
                  (update :selector/electives
                          (partial map keyword))
                  (update :selector/essentials
                          (partial map keyword))
                  (update :selector/images (partial mapv normalize-image)))
        ks (concat (:selector/essentials value)
                   (:selector/electives value))]
    (->> (select-keys value ks)
         (maps/map-values set)
         (merge value))))

(defn index-skus [skus]
  (->> skus
       (map ->skuer)
       (maps/index-by :catalog/sku-id)))

(defn index-products [products]
  (->> products
       (map ->skuer)
       (maps/index-by :catalog/product-id)))

(defmethod transition-state events/api-success-v2-products
  [_ event {:keys [products skus] :as response} app-state]
  (-> app-state
      (update-in keypaths/v2-products merge (index-products products))
      (update-in keypaths/v2-skus merge (index-skus skus))))

(defn path-for-sku [product-id slug sku]
  (routes/path-for events/navigate-product-details
                   (merge {:catalog/product-id product-id
                           :page/slug          slug}
                          (when sku
                            {:query-params {:SKU sku}}))))
