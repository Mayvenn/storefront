(ns storefront.accessors.products)

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


(defn product-is-mayvenn-install-service?
  [product]
  (contains? (set (:promo.mayvenn-install/discountable product))
             true))

(defn service? [product]
  (seq (:service/type product)))

(defn base-service?
  [product]
  (contains? (set (:service/type product))
             "base"))

(defn standalone-service?
  [product]
  (and (base-service? product)
       (not (product-is-mayvenn-install-service? product))))
