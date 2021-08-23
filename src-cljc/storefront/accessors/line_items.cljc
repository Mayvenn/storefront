(ns storefront.accessors.line-items
  (:require [storefront.accessors.products :as products]))

(defn service? [{:keys [source]}]
  (= "service" source))

(defn shipping-method?
  [{:keys [source]}]
  (= "waiter" source))

(defn product?
  [{:keys [source]}]
  (= "spree" source))

(defn product-or-service?
  [line-item]
  (or
   (product? line-item)
   (service? line-item)))

(defn addon-service?
  [line-item]
  (and (service? line-item)
       (-> line-item :variant-attrs :service/type #{"addon"})))

(defn mayvenn-install-service?
  [line-item]
  (and (service? line-item)
       (-> line-item :variant-attrs :service/type #{"base"})
       (-> line-item :variant-attrs :promo.mayvenn-install/discountable true?)))

(defn standalone-service?
  [line-item]
  (and (service? line-item)
       (-> line-item :variant-attrs :service/type #{"base"})
       (-> line-item :variant-attrs :promo.mayvenn-install/discountable false?)))

(defn any-wig?
  [line-item]
  (-> line-item :variant-attrs :hair/family #{"ready-wigs" "360-wigs" "lace-front-wigs"}))


(defn add-product-title-to-line-item [products line-item]
  (cond-> line-item
    (seq line-item) (assoc :product-title (->> line-item
                                               :sku
                                               (products/find-product-by-sku-id products)
                                               :copy/title))))

(defn join-facets-to-line-item [skus facets line-item]
  (let [sku (get skus (:sku line-item))]
    (cond-> line-item
      (seq line-item) (assoc
                       :join/facets
                       (into {} (comp (map
                                       (fn [facet]
                                         (let [facet-slug  (:facet/slug facet)
                                               facet-value (first (get sku facet-slug))]
                                           [facet-slug (->> facet
                                                            :facet/options
                                                            (filter (fn [option]
                                                                      (= facet-value (:option/slug option))))
                                                            first)])))
                                      (remove (comp nil? second)))
                             facets)))))

(defn prep-for-display [products skus facets line-item]
  (->> line-item
       (add-product-title-to-line-item products)
       (join-facets-to-line-item skus facets)))
