(ns storefront.components.stylist.commission-details
  (:require [spice.date :as date]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.order-summary :as summary]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn all-skus-in-commission [skus commission]
  (->> (:order commission)
       orders/product-items
       (mapv :sku)
       (select-keys skus)))

(defn query [data]
  (let [commission          (get-in data keypaths/stylist-commissions-detailed-commission)
        skus-for-commission (all-skus-in-commission (get-in data keypaths/v2-skus)
                                                    commission)]
    {:commission commission
     :fetching?  (utils/requesting? data request-keys/get-stylist-commission)
     :skus       skus-for-commission
     :ship-date  (f/less-year-more-day-date (date/to-iso (->> (:order commission)
                                                              :shipments
                                                              first
                                                              :shipped-at)))}))

(def back-caret
  (component/html
   [:div.inline-block.pr1
    (svg/left-caret {:class  "stroke-gray align-middle"
                     :width  "15px"
                     :height "1.5rem"})]))

(defn component [{:keys [commission fetching? ship-date skus]} owner opts]
  (let [{:keys [id number order amount earned-date commissionable-amount]} commission]
    (component/create
     (if fetching?
       [:div.my2.h2 ui/spinner]

       [:div.container.mb4.px3
        [:a.left.col-12.dark-gray.flex.items-center.py3
         (utils/route-to events/navigate-stylist-dashboard-earnings)
         (ui/back-caret "back to earnings")]
        [:h3.my4 "Details - Commission Earned"]
        [:div.flex.justify-between.col-12
         [:div (f/less-year-more-day-date earned-date)]
         [:div (:full-name order)]
         [:div.green "+" (mf/as-money amount)]]

        [:div.col-12.pt2
         [:div.col-4.inline-block
          [:span.h5.dark-gray "Order Number"]
          [:div.h5 (:number order)]]
         [:div.col-8.inline-block
          [:span.h5.dark-gray "Ship Date"]
          [:div.h5 ship-date]]]

        [:div.mt2.mbnp2.mtnp2.border-top.border-gray
         (summary/display-line-items (orders/product-items order) skus)]

        (summary/display-order-summary-for-commissions order)

        [:div.h5.center.navy
         (str (mf/as-money amount) " has been added to your next payment.")]]))))

(defn built-component [data opts]
  (component/build component (query data) opts))
