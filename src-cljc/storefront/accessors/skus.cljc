(ns storefront.accessors.skus)

(defn determine-epitome
  "Sort SKUs by color and price, then take the first."
  [color-order-map skus]
  (->> skus
       (sort-by (juxt (comp color-order-map first :hair/color)
                      :sku/price))
       first))

(defn determine-cheapest
  "Sort SKUs by price and color, then take the first."
  [color-order-map skus]
  (->> skus
       (sort-by (juxt :sku/price
                      (comp color-order-map first :hair/color)))
       first))
