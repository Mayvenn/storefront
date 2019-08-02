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
  ;; hot-path: index-skus is ~60ms if work is done naively here
  (let [attrs  (:criteria/attributes image)
        ks     (keys attrs)
        image! (-> (transient image)
                   (assoc! :id (str (:use-case image) "-" (:url image)))
                   (assoc! :order (or (:order image)
                                      (case (:image/of attrs)
                                        "model"   1
                                        "product" 2
                                        "seo"     3
                                        "catalog" 4
                                        5)))
                   (dissoc! :criteria/attributes :filename))]

    ;; (merge image' (:criteria/attributes images))
    (loop [image! image!
           i 0]
      (if (< i (count ks))
        (let [k (.indexOf ks i)]
          (if (not= -1 k)
            (recur (assoc! image! k (k attrs)) (inc i))
            (persistent! image!)))
        (persistent! image!)))))

(defn ->skuer [value]
  ;; hot-path: index-skus is ~60ms if work is done naively here
  (let [value (-> (transient value)
                  (assoc! :selector/electives (mapv keyword (:selector/electives value))
                          :selector/essentials (mapv keyword (:selector/essentials value))
                          :selector/images (mapv normalize-image (:selector/images value)))
                  persistent!)
        ks    (into (:selector/essentials value)
                    (:selector/electives value))]
    (->> (select-keys value ks)
         (maps/map-values set)
         (merge value))))

(defn index-skus [skus]
  (->> skus
       (mapv ->skuer)
       (maps/index-by :catalog/sku-id)))

(defn index-products [products]
  (->> products
       (mapv ->skuer)
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

(defn extract-product-skus [app-state product]
  (->> (select-keys (get-in app-state keypaths/v2-skus)
                    (:selector/skus product))
       vals
       (sort-by :sku/price)))
