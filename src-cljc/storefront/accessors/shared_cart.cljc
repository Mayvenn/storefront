(ns storefront.accessors.shared-cart
  (:require [api.catalog :refer [select]]
            [spice.maps :as maps]
            [api.orders :as orders]))

(defn base-service?
  [line-item]
  (-> line-item :service/type first #{"base"} boolean))

(defn discountable?
  [line-item]
  (-> line-item :promo.mayvenn-install/discountable first true?))

(defn sort-by-depart-and-price
  [items]
  (sort-by (fn [{:keys [catalog/department promo.mayvenn-install/discountable sku/price]}]
             [(first department) (not (first discountable)) price])
           items))

(defn enrich-line-items-with-sku-data
  [catalog-skus shared-cart-line-items]
  (let [indexed-catalog-skus (maps/index-by :legacy/variant-id (vals catalog-skus))]
    (map
     (fn [line-item]
       (merge line-item
              (get indexed-catalog-skus (:legacy/variant-id line-item))))
     shared-cart-line-items)))

(defn ^:private prepared-discountable-service [proto-line-items]
  (let [discountable-service-proto-li      (first (filter
                                                   (fn [li]
                                                     (and
                                                      (= "base" (first (:service/type li)))
                                                      (first (:promo.mayvenn-install/discountable li))))
                                                   proto-line-items))
        rules-for-service                  (get orders/rules (:catalog/sku-id discountable-service-proto-li))
        physical-proto-li                  (filter (fn [li]
                                                   (contains? (:catalog/department li) "hair"))
                                                 proto-line-items)
        meets-freeinstall-discountability? (empty? (keep (fn [[word essentials rule-quantity]]
                                                           (let [cart-quantity    (->> physical-proto-li
                                                                                       (select essentials)
                                                                                       (map :item/quantity)
                                                                                       (apply +))
                                                                 missing-quantity (- rule-quantity cart-quantity)]
                                                             (when (pos? missing-quantity)
                                                               {:word             word
                                                                :cart-quantity    cart-quantity
                                                                :missing-quantity missing-quantity
                                                                :essentials       essentials})))
                                                         rules-for-service))]

    (if meets-freeinstall-discountability?
      [(assoc discountable-service-proto-li :discounted-amount (:sku/price discountable-service-proto-li))]
      [discountable-service-proto-li])))

(defn apply-promos
  [proto-line-items]
  (let [other-proto-li    (filter
                           (fn [li]
                             (not
                              (and
                               (= "base" (first (:service/type li)))
                               (first (:promo.mayvenn-install/discountable li)))))
                           proto-line-items)]
    ;; TODO: RENAME
    (concat (prepared-discountable-service proto-line-items)
            other-proto-li)))
