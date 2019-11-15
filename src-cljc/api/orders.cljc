(ns api.orders
  (:require adventure.keypaths
            [checkout.accessors.vouchers :as vouchers-accessors]
            [spice.core :as spice]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            storefront.keypaths))

(def install-items-required 3)

(def default-service-menu
  "Install prices to use when a stylist has not yet been selected."
  {:advertised-sew-in-360-frontal "225.0"
   :advertised-sew-in-closure     "175.0"
   :advertised-sew-in-frontal     "200.0"
   :advertised-sew-in-leave-out   "150.0"})

(defn ^:private mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views"
  [order servicing-stylist sku-catalog environment]
  (let [freeinstall-entered?        (boolean (orders/freeinstall-entered? order))
        service-line-item           (first (orders/service-line-items order))
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
        service-type                (->> environment
                                         vouchers-accessors/campaign-configuration
                                         (filter #(= (:service/type %)
                                                     (some-> (orders/product-items order)
                                                             vouchers-accessors/product-items->highest-value-service)))
                                         first
                                         :service/diva-advertised-type)
        service-menu                (or (:servicing-menu servicing-stylist)
                                        default-service-menu)]
    (when freeinstall-entered?
      {:mayvenn-install/entered?           freeinstall-entered?
       :mayvenn-install/locked?            (and freeinstall-entered?
                                                (pos? items-remaining-for-install))
       :mayvenn-install/applied?           (orders/freeinstall-applied? order)
       :mayvenn-install/quantity-required  install-items-required
       :mayvenn-install/quantity-remaining (- install-items-required items-added-for-install)
       :mayvenn-install/quantity-added     items-added-for-install
       :mayvenn-install/stylist            servicing-stylist
       :mayvenn-install/service-type       service-type
       :mayvenn-install/service-discount   (- 0
                                              (or
                                               (line-items/service-line-item-price service-line-item)
                                               (some-> service-menu
                                                       ;; If the menu does not provide the service matching the
                                                       ;; cart contents, use the leave out price
                                                       (get service-type (:advertised-sew-in-leave-out service-menu))
                                                       spice/parse-double)))})))

(defn ->order
  [app-state order]
  (let [waiter-order      order
        servicing-stylist (if (= "aladdin" (get-in app-state storefront.keypaths/store-experience))
                            (get-in app-state storefront.keypaths/store)
                            (get-in app-state adventure.keypaths/adventure-servicing-stylist))
        sku-catalog       (get-in app-state storefront.keypaths/v2-skus)
        environment       (get-in app-state storefront.keypaths/environment)
        store-slug        (get-in app-state storefront.keypaths/store-slug)
        mayvenn-install   (mayvenn-install waiter-order servicing-stylist sku-catalog environment)]
    (merge
     mayvenn-install
     {:waiter/order         waiter-order
      :order/dtc?           (contains? #{"shop" "freeinstall"} store-slug)
      :order/submitted?     (= "submitted" (:state order))
      :order.shipping/phone (get-in waiter-order [:shipping-address :phone])
      :order.items/quantity (orders/product-quantity waiter-order)})))

(defn completed
  [app-state]
  (->order app-state (get-in app-state storefront.keypaths/completed-order)))

(defn current
  [app-state]
  (->order app-state (get-in app-state storefront.keypaths/order)))
