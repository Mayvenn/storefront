(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.stylists :as stylists]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn order-state [order]
  (if (:shipment order)
    :paid
    :pending))

(defn state-look [state]
  (case state
    :pending "teal"
    :paid "green"))

(defn four-up [a b c d]
  [:div.clearfix.mxn1
   [:.col.col-3.px1 a]
   [:.col.col-3.px1 b]
   [:.col.col-3.px1 c]
   [:.col.col-3.px1 d]])

(defn show-commission
  [data {:keys [number commissionable_amount full-name commission_date] :as commission}]
  (let [state (order-state commission)]
    [:.p2.border-bottom.border-white
     [:div
      (utils/route-to data events/navigate-order {:number number})
      [:div.mb2
       [:div.px1.h6.right.border.capped
        {:style {:padding-top "3px" :padding-bottom "2px"}
         :class (state-look state)}
        (when (= state :paid) "+") (f/as-money commissionable_amount)]
       [:.h2 full-name]]
      [:div.gray.h5.mb1
       (four-up "Status" "Ship Date" "Order" [:.black.right.h2.mtn1 "..."])]]

     [:div.medium.h5
      (four-up
       [:.titleize {:class (state-look state)} (name state)]
       (f/locale-date commission_date)
       number
       nil)]]))

(def empty-commissions
  (html
   [:div.center
    [:div.p2
     [:.img-receipt-icon.bg-no-repeat.bg-center {:style {:height "8em" }}]
     [:p.h2.gray.muted "Looks like you don't have any commissions yet."]]
    [:hr.border.border-white ]
    [:.py3.h3
     [:p.mx4.pb2 "Get started by sharing your store with your clients:"]
     [:p.medium stylists/store-url]]]))

(def micro-dollar-sign
  [:svg {:viewbox "0 0 14 13", :height "13", :width "14"}
   [:g {:stroke-width "1" :stroke "#9B9B9B" :fill "none"}
    [:path {:d "M13 6.5c0 3.3-2.7 6-6 6s-6-2.7-6-6 2.7-6 6-6 6 2.7 6 6z"}]
    [:path {:d "M5.7 7.8c0 .72.58 1.3 1.3 1.3.72 0 1.3-.58 1.3-1.3 0-.72-.58-1.3-1.3-1.3-.72 0-1.3-.58-1.3-1.3 0-.72.58-1.3 1.3-1.3.72 0 1.3.58 1.3 1.3M7 3.1v6.8"}]]])

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    (let [commissions (get-in data keypaths/stylist-commissions-history)
          commission-rate (get-in data keypaths/stylist-commissions-rate)]
      [:div.mx-auto.container
       [:div.clearfix.mxn2 {:data-test "commissions-panel"}
        [:div.lg-col.lg-col-9.px2
         (if (seq commissions)
           (for [commission commissions]
             (show-commission data commission))
           empty-commissions)]

        [:div.lg-col.lg-col-3.px2
         (when commission-rate
           [:.mt3.h6.muted.flex.justify-center.items-center
            [:div.mr1 micro-dollar-sign]
            [:div.center
             "Earn " commission-rate "% commission on all sales. (tax and store credit excluded)"]])]]]))))
