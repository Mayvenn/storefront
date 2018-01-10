(ns storefront.components.stylist.earnings
  (:require [clojure.string :as str]
            goog.string
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.images :as images]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.platform.messages :as messages]))

(defn earnings-table [history]
  [:table.col-12.mb3 {:style {:border-spacing 0}}
   [:tbody
    (map-indexed
     (fn [i {:keys [id number order amount commission-date commissionable-amount] :as commission}]
       [:tr (merge {:key id}
                   (utils/route-to events/navigate-stylist-dashboard-commission-details {:commission-id id})
                   (when (odd? i)
                     {:class "bg-too-light-teal"}))
        [:td.px3.py2 (f/less-year-more-day-date commission-date)]
        [:td.py2 (:full-name order) [:div.h7 "Commission Earned"]]
        [:td.pr3.py2.green.right-align "+" (mf/as-money amount)]])
     history)]])

(def empty-commissions
  (html
   [:div.center
    [:div.p2.border-bottom.border-light-gray
     [:div.img-receipt-icon.bg-no-repeat.bg-center {:style {:height "8em"}}]
     [:p.h3.gray "Looks like you don't have any commissions yet."]]
    [:.py3.h4
     [:p.mx4.pb2 "Get started by sharing your store with your clients:"]
     [:p.medium stylist-urls/store-url]]]))

(defn show-commission-rate [rate]
  (let [message (list "Earn " rate "% commission on all sales. (tax and store credit excluded)")]
    [:div.h6.dark-gray
     [:div.p2.hide-on-mb
      [:div.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:div.my3.hide-on-tb-dt
      [:div.center message]]]))

(def show-program-terms
  [:div.col-on-tb-dt.col-12-on-tb-dt
   [:div.border-top.border-gray.mx-auto.my2 {:style {:width "100px"}}]
   [:div.center.my2.h6
    [:a.dark-gray (utils/route-to events/navigate-content-program-terms) "Mayvenn Program Terms"]]])

(defn component [{:keys [commissions products skus fetching?]}]
  (om/component
   (let [{:keys [history page pages rate]} commissions]
     (html
      (if (and (empty? (seq history)) fetching?)
        [:div.my2.h2 ui/spinner]
        [:div.clearfix
         {:data-test "earnings-panel"}
         [:div.col-on-tb-dt.col-9-on-tb-dt
          (when (seq history)
            (earnings-table history))
          (pagination/fetch-more events/control-stylist-commissions-fetch fetching? page pages)
          (when (zero? pages)
            empty-commissions)]

         [:div.col-on-tb-dt.col-3-on-tb-dt
          (when rate (show-commission-rate rate))
          show-program-terms]])))))

(defn all-skus-in-commissions [commissions]
  (->> (:history commissions)
       (map :order)
       (mapcat orders/product-items)
       (map :sku)))

(defn query [data]
  (let [commissions     (get-in data keypaths/stylist-commissions)
        commission-skus (all-skus-in-commissions commissions)]
    {:commissions commissions
     :fetching?   (utils/requesting? data request-keys/get-stylist-commissions)}))
