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

(defn commission-row [row-number {:keys [id number order amount earned-date commissionable-amount] :as commission}]
  [:tr.pointer (merge {:key id}
                      (utils/route-to events/navigate-stylist-dashboard-commission-details {:commission-id id})
                      (when (odd? row-number)
                        {:class "bg-too-light-teal"}))
   [:td.px3.py2 (f/less-year-more-day-date earned-date)]
   [:td.py2 (:full-name order) [:div.h6 "Commission Earned"]]
   [:td.pr3.py2.green.right-align "+" (mf/as-money amount)]])

(defn earning-row [row-number {:keys [id amount earned-date reason] :as earning}]
  [:tr.pointer (when (odd? row-number)
                 {:class "bg-too-light-teal"})
   [:td.px3.py2 (f/less-year-more-day-date earned-date)]
   [:td.py2 "Account Correction" [:div.h6 "Admin Payout"]]
   [:td.pr3.py2.green.right-align "+" (mf/as-money amount)]])

(defn payout-row [{:keys [id amount earned-date payout-method] :as earning}]
  [:tr.bg-warmer-silver
   [:td.px3.py2 (f/less-year-more-day-date earned-date)]
   [:td.py2 {:col-span 2} "You transferred " [:span.medium amount]
    [:div.h6 (str "Earnings Transfer - " payout-method)]]])

(defn earnings-table [history]
  [:table.col-12.mb3 {:style {:border-spacing 0}}
   [:tbody
    (map-indexed
     (fn [i {:keys [type] :as earning}]
       (condp = type
         "commission" (commission-row i earning)
         "earning"    (earning-row i earning)
         "payout"     (payout-row earning)))
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

(defn component [{:keys [earnings products fetching?]}]
  (om/component
   (let [{:keys [history page pages rate]} earnings]
     (html
      (if (and (empty? (seq history)) fetching?)
        [:div.my2.h2 ui/spinner]
        [:div.clearfix
         {:data-test "earnings-panel"}
         [:div.col-on-tb-dt.col-9-on-tb-dt
          (when (seq history)
            (earnings-table history))
          (pagination/fetch-more events/control-stylist-earnings-fetch fetching? page pages)
          (when (zero? pages)
            empty-commissions)]

         [:div.col-on-tb-dt.col-3-on-tb-dt
          (when rate (show-commission-rate rate))
          show-program-terms]])))))

(defn query [data]
  (let [earnings (get-in data keypaths/stylist-earnings)]
    {:earnings  earnings
     :fetching? (utils/requesting? data request-keys/get-stylist-commissions)}))
