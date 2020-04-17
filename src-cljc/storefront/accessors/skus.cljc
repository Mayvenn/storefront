(ns storefront.accessors.skus)

(defn filter-first
  [p coll]
  (some #(when (p %) %) coll))

(defn determine-epitome
  "Product definition of epitome is the 'first' SKU on the product details page where
  first is when the first of every facet is selected.

  We're being lazy and sort by color facet + sku price (which implies sort by hair/length)"
  [color-order-map skus]
  (let [sorted-skus (sort-by (juxt (comp color-order-map first :hair/color)
                                   :sku/price) skus)]
    (or (filter-first :inventory/in-stock? sorted-skus)
        (first sorted-skus))))

(defn determine-cheapest
  "Note, it is technically possible for the cheapest sku to not be the epitome:
  If 10\" Black is sold out, 10\" Brown is the cheapest, but 12\" Black is the epitome

  Sort SKUs by price, using color as a tie-breaker. "
  [color-order-map skus]
  (let [sorted-skus (sort-by (juxt :sku/price
                                   (comp color-order-map first :hair/color)) skus)]
    (or (filter-first :inventory/in-stock? sorted-skus)
        (first sorted-skus))))
