(ns storefront.components.stylist.stats
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn ^:private circle [idx selected]
  [:div.bg-white.circle
   {:class (str
            (when-not (zero? idx) "ml2") " "
            (when-not (= idx selected) "bg-lighten-4"))
    :style {:width "8px" :height "8px"}}])

(defn ^:private big-money [amount]
  [:span {:style {:font-size "64px"}} (f/as-money-without-cents amount)])

(defn ^:private big-money-with-cents [amount]
  [:div.flex.justify-center
   (big-money amount)
   [:span.h5 {:style {:margin "5px 3px"}} (f/as-money-cents-only amount)]])

(defn ^:private next-payout [amount]
  [:div.m3
   [:div.p1 "NEXT PAYOUT"]
   [:div.p2 (big-money-with-cents amount)]
   [:div "Payment in FIXME days"]])

(defn ^:private commissions [amount]
  [:div.m3
   [:div.p1 "LIFETIME COMMISSIONS"]
   [:div.p2 (big-money amount)]
   [:div utils/nbsp]])

(defn stylist-dashboard-stats-component [data owner]
  (om/component
   (html
    [:div.p1.bg-teal.bg-lighten-top-3.white.center.sans-serif
     (commissions (get-in data keypaths/stylist-commissions-paid-total))
     (next-payout (get-in data keypaths/stylist-commissions-next-amount))

     [:div.flex.justify-center.p1
      (circle 0 1)
      (circle 1 1)
      (circle 2 1)]])))
