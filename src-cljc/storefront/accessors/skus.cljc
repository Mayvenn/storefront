(ns storefront.accessors.skus)

(defn determine-epitome [color-order-map skus]
  (->> skus
       (sort-by (juxt (comp color-order-map first :hair/color)
                      :sku/price))
       first))
