(ns storefront.accessors.mayvenn-install
  (:require [adventure.keypaths :as adventure-keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            storefront.keypaths))

(defn ^:private family->ordering
  [wig-customization? family]
  (let [families (merge
                  {"bundles"      5
                   "closures"     4
                   "frontals"     3
                   "360-frontals" 2}
                  (when wig-customization?
                    {"360-wigs"        1
                     "lace-front-wigs" 1}))]
    (get families family 10)))

(defn ^:private product-family->service-type
  [wig-customization? family]
  (get
   (merge
    {"bundles"      :leave-out
     "closures"     :closure
     "frontals"     :frontal
     "360-frontals" :three-sixty}
    (when wig-customization?
      {"360-wigs"        :wig-customization
       "lace-front-wigs" :wig-customization}))
   family))

(defn product-line-items->service-type
  [wig-customization? product-items]
  (->> product-items
       (map (comp :hair/family :variant-attrs))
       (sort-by (partial family->ordering wig-customization?))
       first
       (product-family->service-type wig-customization?)))

;; TODO: consider unifying this with api.orders/mayvenn-install
(defn mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views.
  Service type is inferred from the cart content in order to display appropriate
  service related copy"
  [app-state]
  (let [order                       (get-in app-state storefront.keypaths/order)
        shipment                    (-> order :shipments first)
        wig-customization?          (experiments/wig-customization? app-state)
        any-wig?                    (and
                                     wig-customization?
                                     (->> shipment
                                          :line-items
                                          (filter line-items/any-wig?)
                                          count
                                          pos?))
        service-type                (product-line-items->service-type
                                     wig-customization?
                                     (orders/product-items-for-shipment shipment))
        service-line-item           (first (orders/service-line-items order))
        sku-catalog                 (get-in app-state storefront.keypaths/v2-skus)
        wig-customization-service?  (= :wig-customization service-type)
        install-items-required      (if wig-customization-service? 1 3)
        item-eligibility-fn         (if wig-customization-service?
                                      line-items/customizable-wig?
                                      (partial line-items/sew-in-eligible? sku-catalog))
        items-added-for-install     (->> shipment
                                         :line-items
                                         (filter item-eligibility-fn)
                                         (map :quantity)
                                         (apply +)
                                         (min install-items-required))
        items-remaining-for-install (- install-items-required items-added-for-install)
        freeinstall-entered?        (boolean (orders/freeinstall-entered? order))
        servicing-stylist           (if (= "aladdin" (get-in app-state storefront.keypaths/store-experience))
                                      (get-in app-state storefront.keypaths/store)
                                      (get-in app-state adventure-keypaths/adventure-servicing-stylist))]
    {:mayvenn-install/entered?           freeinstall-entered?
     :mayvenn-install/locked?            (and service-line-item
                                              (not (line-items/fully-discounted? service-line-item)))
     :mayvenn-install/applied?           (orders/service-line-item-promotion-applied? order)
     :mayvenn-install/quantity-required  install-items-required
     :mayvenn-install/quantity-remaining items-remaining-for-install
     :mayvenn-install/quantity-added     items-added-for-install
     :mayvenn-install/stylist            servicing-stylist
     :mayvenn-install/service-discount   (- (line-items/service-line-item-price service-line-item))
     :mayvenn-install/any-wig?           any-wig?
     :mayvenn-install/service-type       service-type}))
