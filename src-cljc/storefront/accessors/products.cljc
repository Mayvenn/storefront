(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

(defn product-title
  "Prefer variant-name, if available. Otherwise use product name (product-name
  from waiter line item; name from cellar variant)"
  [{:keys [variant-name product-name name]}]
  (or variant-name product-name name))

(defn find-product-by-sku-id [products line-item-sku]
  (->> (vals products)
       (filter (fn [product]
                 (contains? (set (:selector/skus product))
                            line-item-sku)))
       first))

(def wig-families #{"ready-wigs" "360-wigs" "lace-front-wigs"})

(defn wig-product? [product]
  (-> product
      :hair/family
      first
      wig-families))
