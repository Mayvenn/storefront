(ns storefront.accessors.products)

(defn graded? [product]
  (-> product
      :collection_name
      #{"premier" "deluxe" "ultra"}
      boolean))

(defn all-variants [product]
  (conj (:variants product) (:master product)))
