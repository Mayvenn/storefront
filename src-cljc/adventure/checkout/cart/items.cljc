(ns adventure.checkout.cart.items
  (:require
   [checkout.accessors.vouchers :as vouchers]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.orders :as orders]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [storefront.components.svg :as svg]))

(def line-item-detail
  [:div.mb1.mt0 (str "w/ " "a Certified Mayvenn Stylist")
   [:ul.h6.purple-checkmark.pl4
    (mapv (fn [%] [:li %])
          ["Licensed Salon Stylist" "Near you" "Experienced"])]])

(defn freeinstall-line-item-query [data]
  (let [order                 (get-in data keypaths/order)
        highest-value-service (or (some-> order
                                      orders/product-items
                                      vouchers/product-items->highest-value-service)
                                  :leave-out)

        {:as   campaign
         :keys [:voucherify/campaign-name
                :service/diva-advertised-type]} (->> (get-in data keypaths/environment)
                                                     vouchers/campaign-configuration
                                                     (filter #(= (:service/type %) highest-value-service))
                                                     first)
        service-price                           (some-> data
                                                        (get-in keypaths/store-service-menu)
                                                        (get diva-advertised-type ))
        number-of-items-needed (- 3 (orders/product-quantity order))]
    {:id                     "freeinstall"
     :title                  "Install"
     :detail                 line-item-detail
     :price                  service-price
     :total-savings          (orders/total-savings order service-price)
     :number-of-items-needed number-of-items-needed
     :add-more-hair?         (< 0 number-of-items-needed)
     :thumbnail-image-fn     (fn [height-width-int]
                               (ui/ucare-img {:width height-width-int}
                                             "688ebf23-5e54-45ef-a8bb-7d7480317022"))}))
