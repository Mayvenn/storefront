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

(defn checked-item [text]
  [:div.flex.items-center
   [:div.mr2 (ui/ucare-img {:width "12"} "2560cee9-9ac7-4706-ade4-2f92d127b565")]
   text])

(def line-item-detail
  [:div (str "w/ " "a Certified Mayvenn Stylist")
   (checked-item "Licensed Salon Stylist")
   (checked-item "Near you")
   (checked-item "Experienced")])

(defn freeinstall-line-item-query [data]
  (let [order (get-in data keypaths/order)]
    (when (experiments/v2-experience? data)
      (let [highest-value-service (some-> order
                                          orders/product-items
                                          vouchers/product-items->highest-value-service)

            {:as   campaign
             :keys [:voucherify/campaign-name
                    :service/diva-advertised-type]} (->> (get-in data keypaths/environment)
                                                         vouchers/campaign-configuration
                                                         (filter #(= (:service/type %) highest-value-service))
                                                         first)
            service-price                           (some-> data
                                                            (get-in keypaths/store-service-menu)
                                                            (get diva-advertised-type))]
        (when service-price
          {:id                 "freeinstall"
           :title              "Install "#_campaign-name
           :detail             line-item-detail
           :price              service-price
           :total-savings      (orders/total-savings order service-price)
           :thumbnail-image-fn (fn [height-width-int]
                                 (ui/ucare-img {:width height-width-int}
                                               "688ebf23-5e54-45ef-a8bb-7d7480317022"))})))))
