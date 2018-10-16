(ns checkout.cart.items
  (:require
   [checkout.accessors.vouchers :as vouchers]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.orders :as orders]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]))

(defn determine-service-price [menu install-type advertised-type]
  (or (advertised-type menu)
      (install-type menu)))

(defn freeinstall-line-item-query [data]
  (let [order (get-in data keypaths/order)]
    (when (and (experiments/aladdin-experience? data)
               (orders/freeinstall-applied? order))
      (let [store-nickname        (get-in data keypaths/store-nickname)
            highest-value-service (some-> order
                                          orders/product-items
                                          vouchers/product-items->highest-value-service)

            {:as   campaign
             :keys [:voucherify/campaign-name
                    :service/diva-install-type
                    :service/diva-advertised-type]} (->> (get-in data keypaths/environment)
                                                         vouchers/campaign-configuration
                                                         (filter #(= (:service/type %) highest-value-service))
                                                         first)

            service-price (some-> data
                                  (get-in keypaths/store-service-menu)
                                  (determine-service-price diva-install-type diva-advertised-type))]
        {:removing?          (utils/requesting? data request-keys/remove-promotion-code)
         :id                 "freeinstall"
         :title              campaign-name
         :detail             (str "w/ " store-nickname)
         :price              service-price
         :total-savings      (orders/total-savings order service-price)
         :remove-event       [events/control-checkout-remove-promotion {:code "freeinstall"}]
         :thumbnail-image-fn (fn [height-width-int]
                               (ui/ucare-img {:width height-width-int}
                                             "688ebf23-5e54-45ef-a8bb-7d7480317022"))}))))
