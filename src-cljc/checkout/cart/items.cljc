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

(defn maybe-advertised-price [type menu]
  (let [advertised-service-key (some->> type name (drop-while #(not= \- %)) (apply str "advertised") keyword)]
    (or (advertised-service-key menu)
        (type menu))))

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
                    :service/diva-type]} (->> (get-in data keypaths/environment)
                                              vouchers/campaign-configuration
                                              (filter #(= (:service/type %) highest-value-service))
                                              first)

            service-price (some-> data
                                  (get-in keypaths/store-service-menu)
                                  (maybe-advertised-price diva-type))]
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
