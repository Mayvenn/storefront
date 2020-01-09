(ns storefront.accessors.mayvenn-install
  (:require [adventure.keypaths :as adventure-keypaths]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            storefront.keypaths))

;; TODO: consider unifying this with api.orders/mayvenn-install
(defn mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views"
  [app-state]
  (let [order                       (get-in app-state storefront.keypaths/order)
        service-line-item           (first (orders/service-line-items order))
        sku-catalog                 (get-in app-state storefront.keypaths/v2-skus)
        wig-customization?          (= "SRV-WGC-000" (:sku service-line-item))
        install-items-required      (if wig-customization? 1 3)
        item-eligibility-fn         (if wig-customization?
                                      line-items/customizable-wig?
                                      (partial line-items/sew-in-eligible? sku-catalog))
        items-added-for-install     (->> order
                                         :shipments
                                         first
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
     :mayvenn-install/locked?            (and freeinstall-entered? (pos? items-remaining-for-install))
     :mayvenn-install/applied?           (orders/freeinstall-applied? order)
     :mayvenn-install/quantity-required  install-items-required
     :mayvenn-install/quantity-remaining (- install-items-required items-added-for-install)
     :mayvenn-install/quantity-added     items-added-for-install
     :mayvenn-install/stylist            servicing-stylist
     :mayvenn-install/service-discount   (- (line-items/service-line-item-price service-line-item))}))
