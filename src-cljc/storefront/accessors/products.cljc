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

(defn wig-construction-service?
  [product]
  (= #{"construction"} (:service/category product)))

(defn service? [product]
  (seq (:service/type product)))

(defn hair? [product]
  (contains? (set (:catalog/department product))
             "hair"))

(defn base-service?
  [product]
  (contains? (set (:service/type product))
             "base"))

(defn product->faq-id [product]
  (case (:hair/family product)
    #{"ready-wigs"}        :pdp-ready-wear-wigs
    #{"lace-front-wigs"}   :pdp-virgin-lace-front-wigs
    #{"360-wigs"}          :pdp-virgin-360-lace-wigs
    #{"360-frontals"}      :pdp-360-frontals
    #{"bundles"}           :pdp-bundles
    #{"closures"}          :pdp-closures
    #{"frontals"}          :pdp-lace-frontals
    #{"seamless-clip-ins"} :pdp-seamless-clip-ins
    #{"tape-ins"}          :pdp-straight-tape-ins
    nil))
