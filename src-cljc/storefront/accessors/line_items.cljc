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

(defn base-service?
  [line-item]
  (and (service? line-item)
       (-> line-item :variant-attrs :service/type #{"base"})))

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

(defn customizable-wig?
  [line-item]
  (-> line-item :variant-attrs :hair/family #{"360-wigs" "lace-front-wigs"}))

(defn sew-in-eligible? [sku-catalog line-item]
  (->> line-item :sku (get sku-catalog) :promo.mayvenn-install/eligible first))

(defn discounted-unit-price
  [{:keys [applied-promotions unit-price quantity]}]
  (let [total-amount-off  (->> applied-promotions
                               (map :amount)
                               (reduce + 0)
                               -)
        discount-per-item (/ total-amount-off quantity)]
    (- unit-price discount-per-item)))

(defn fully-discounted?
  [{:as   line-item
    :keys [applied-promotions unit-price]}]
  (when line-item
    (= 0 (+ unit-price (->> applied-promotions (keep :amount) (reduce + 0))))))

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
