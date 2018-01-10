(ns storefront.components.stylist.commission-details
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.assets :as assets]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.accessors.images :as images] [storefront.components.stylist.pagination :as pagination]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.components.order-summary :as summary]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]
            [goog.string]
            [goog.userAgent.product :as product]
            [spice.core :as spice]
            [spice.date :as date]
            [storefront.component :as component]))

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
  (let [{:keys [id number order amount commission-date commissionable-amount]} commission]
    (component/create
     (if fetching?
       [:div.my2.h2 ui/spinner]

       [:div.container.mb4.px3
        [:a.left.col-12.dark-gray.flex.items-center.py3
         (utils/route-to events/navigate-stylist-dashboard-earnings)
         (ui/back-caret "back to earnings")]
        [:h3.my4 "Details - Commission Earned"]
        [:div.flex.justify-between.col-12
         [:div (f/less-year-more-day-date commission-date)]
         [:div (:full-name order)]
         [:div.green "+" (mf/as-money amount)]]

        [:div.col-12
         [:div.col-4.inline-block
          [:span.h6.dark-gray "Order Number"]
          [:div.h6 (:number order)]]
         [:div.col-8.inline-block
          [:span.h6.dark-gray "Ship Date"]
          [:div.h6 ship-date]]]

        [:div.mt2.mbnp2.mtnp2.border-top.border-gray
         (summary/display-line-items (orders/product-items order) skus)]

        (summary/display-order-summary order {:read-only? true})

        [:div.h5.center
         (str (mf/as-money amount) " has been added to your next payment.")]]))))

(defn built-component [data opts]
  (component/build component (query data) opts))
