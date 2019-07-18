(ns adventure.checkout.cart.items
  (:require
   [adventure.keypaths :as adv-keypaths]
   [checkout.accessors.vouchers :as vouchers]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.stylists :as stylists]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]))

(defn line-item-detail [servicing-stylist-name]
  [:div.mtn1
   (str "w/ " (if (empty? servicing-stylist-name)
                "a Certified Mayvenn Stylist"
                servicing-stylist-name))

   (if (empty? servicing-stylist-name)
     (ui/teal-button (merge {:height-class :small
                             :width-class "col-6"
                             :class "mt1"}
                            (utils/route-to events/navigate-adventure-install-type))
                     "Pick a Stylist")
     [:ul.h6.list-img-purple-checkmark.pl4
      (mapv (fn [%] [:li %])
            ["Licensed Salon Stylist" "Mayvenn Certified" "In your area"])])])

(defn freeinstall-line-item-query [data]
  (let [order                 (get-in data keypaths/order)
        highest-value-service (or
                               (when-let [install-type (:install-type order)]
                                 (keyword install-type))
                               ;; TODO: GROT when all older cart orders have been migrated to install-type
                               (some-> order
                                       orders/product-items
                                       vouchers/product-items->highest-value-service)
                               :leave-out)
        diva-advertised-type   (->> (get-in data keypaths/environment)
                                    vouchers/campaign-configuration
                                    (filter #(= (:service/type %) highest-value-service))
                                    first
                                    :service/diva-advertised-type)
        service-price          (some-> (or (get-in data adv-keypaths/adventure-servicing-stylist)
                                           (get-in data keypaths/store))
                                       :service-menu
                                       (get diva-advertised-type ))
        number-of-items-needed (- 3 (orders/product-quantity order))
        servicing-stylist      (get-in data adv-keypaths/adventure-servicing-stylist)]
    {:id                     "freeinstall"
     :title                  "Install"
     :detail                 (line-item-detail (stylists/->display-name servicing-stylist))
     :price                  service-price
     :total-savings          (orders/total-savings order service-price)
     :number-of-items-needed number-of-items-needed
     :add-more-hair?         (< 0 number-of-items-needed)
     :thumbnail-image        "f6e246a6-c07a-44ae-81e9-af8167403352"
     :thumbnail-image-fn     (fn [height-width-int]
                               (ui/ucare-img {:width height-width-int}
                                             "f6e246a6-c07a-44ae-81e9-af8167403352"))}))
