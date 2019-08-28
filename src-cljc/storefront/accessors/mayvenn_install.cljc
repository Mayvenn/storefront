(ns storefront.accessors.mayvenn-install
  (:require
   [adventure.keypaths :as adventure-keypaths]
   [checkout.accessors.vouchers :as vouchers]
   [storefront.accessors.orders :as orders]
   [storefront.keypaths :as keypaths]
   [storefront.accessors.service-menu :as service-menu]
   [spice.core :as spice]))

(defn mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views"
  [app-state]
  (let [order                       (get-in app-state keypaths/order)
        freeinstall-entered?        (boolean (orders/freeinstall-entered? order))
        install-items-required      3
        sku-catalog                 (get-in app-state keypaths/v2-skus)
        items-added-for-install     (->> order
                                         :shipments
                                         first
                                         :line-items
                                         (filter #(->> %
                                                       :sku
                                                       (get sku-catalog)
                                                       :promo.mayvenn-install/eligible
                                                       first))
                                         (map :quantity)
                                         (apply +)
                                         (min install-items-required))
        items-remaining-for-install (- install-items-required items-added-for-install)
        servicing-stylist           (get-in app-state adventure-keypaths/adventure-servicing-stylist)
        service-type                (->> (get-in app-state keypaths/environment)
                                         vouchers/campaign-configuration
                                         (filter #(= (:service/type %)
                                                     (some-> (orders/product-items order)
                                                             vouchers/product-items->highest-value-service)))
                                         first
                                         :service/diva-advertised-type)
        service-menu                (or (get-in app-state adventure-keypaths/adventure-servicing-stylist-service-menu)
                                        service-menu/default-service-menu)]
    {:mayvenn-install/entered?           freeinstall-entered?
     :mayvenn-install/locked?            (and freeinstall-entered?
                                              (pos? items-remaining-for-install))
     :mayvenn-install/applied?           (and freeinstall-entered?
                                              ;; TODO should we consider the following that checks line-items for promos
                                              ;; (boolean (orders/applied-install-promotion order))
                                              ;; (orders/freeinstall-applied? order)
                                              (zero? items-remaining-for-install))
     :mayvenn-install/quantity-required  install-items-required
     :mayvenn-install/quantity-remaining (- install-items-required items-added-for-install)
     :mayvenn-install/quantity-added     items-added-for-install
     :mayvenn-install/stylist            servicing-stylist
     :mayvenn-install/service-type       service-type
     :mayvenn-install/service-discount   (some-> service-menu
                                                 ;; If the menu does not provide the service matching the
                                                 ;; cart contents, use the leave out price
                                                 (get service-type (:advertised-sew-in-leave-out service-menu))
                                                 spice/parse-double
                                                 -)}))
