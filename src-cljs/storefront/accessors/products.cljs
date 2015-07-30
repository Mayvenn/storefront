(ns storefront.accessors.products)

(defn graded? [product]
  (-> product
      :collection_name
      #{"premier" "deluxe" "ultra"}
      boolean))
