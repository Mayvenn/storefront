(ns storefront.accessors.shared-cart
  (:require [spice.maps :as maps]))

(defn base-service?
  [line-item]
  (-> line-item :service/type first #{"base"} boolean))

(defn discountable?
  [line-item]
  (-> line-item :promo.mayvenn-install/discountable first true?))

(defn sort-by-depart-and-price
  [items]
  (sort-by (fn [{:keys [catalog/department promo.mayvenn-install/discountable sku/price]}]
             [(first department) (not (first discountable)) price])
           items))

(defn enrich-line-items-with-sku-data
  [catalog-skus shared-cart-line-items]
  (let [indexed-catalog-skus (maps/index-by :legacy/variant-id (vals catalog-skus))]
    (map
     (fn [line-item]
       (merge line-item
              (get indexed-catalog-skus (:legacy/variant-id line-item))))
     shared-cart-line-items)))
