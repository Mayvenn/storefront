(ns adventure.checkout.cart.items
  (:require
   [checkout.accessors.vouchers :as vouchers]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.orders :as orders]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [adventure.keypaths :as adv-keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [storefront.components.svg :as svg]))

(defn line-item-detail [servicing-stylist-name]
  [:div.mb1.mt0
   (str "w/ " (if (empty? servicing-stylist-name)
                "a Certified Mayvenn Stylist"
                servicing-stylist-name))
   [:ul.h6.list-img-purple-checkmark.pl4
    (mapv (fn [%] [:li %])
          ["Licensed Salon Stylist" "Mayvenn Certified" "In your area"])]])

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
        service-price                           (some-> (or (get-in data adv-keypaths/adventure-servicing-stylist)
                                                            (get-in data keypaths/store))
                                                        :service-menu
                                                        (get diva-advertised-type ))
        number-of-items-needed                  (- 3 (orders/product-quantity order))
        {:keys [address]}                       (get-in data adv-keypaths/adventure-servicing-stylist)]
    {:id                     "freeinstall"
     :title                  "Install"
     :detail                 (line-item-detail (:firstname address))
     :price                  service-price
     :total-savings          (orders/total-savings order service-price)
     :number-of-items-needed number-of-items-needed
     :add-more-hair?         (< 0 number-of-items-needed)
     :thumbnail-image-fn     (fn [height-width-int]
                               (ui/ucare-img {:width height-width-int}
                                             "f6e246a6-c07a-44ae-81e9-af8167403352"))}))
