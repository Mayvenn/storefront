(ns storefront.components.stylist.stats
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as util]
            [storefront.platform.carousel :as carousel]))

(def payday 3) ;; 3 -> Wednesday in JS

(defn days-till-payout []
  (let [dow (.getDay (js/Date.))]
    (mod (+ 7 payday (- dow))
         7)))

(defn in-x-days []
  (let [days (days-till-payout)]
    (condp = days
      0 "today"
      1 "tomorrow"
      (str "in " days " days"))))

(def stat-card "left col-12 relative")
(def re-center-money {:style {:margin-left "-5px"}})

(defn previous-payout-slide [{:keys [amount date]}]
  [:div.my4.clearfix
   [:.my4
    {:key "previous-payout" :class stat-card}
    [:.p1 "LAST PAYMENT"]
    (if (> amount 0)
      [:div
       [:.py2.h0 re-center-money (ui/big-money amount)]
       [:div "On " (f/long-date date)]]
      [:div
       [:.py2.h0 svg/large-payout]
       [:div "Your last payment will show here."]])]])

(defn cash-out-now-slide [{:keys [amount]}]
  [:div.my4.clearfix
   [:.my4
    {:key "available-earnings" :class stat-card}
    [:.p1 "AVAILABLE EARNINGS"]
    (if (> amount 0)
      [:div
       [:.py2.h0 re-center-money (ui/big-money amount)]
       [:div.col-5.mt1.mb2.mx-auto
        (ui/light-ghost-button (merge
                                {:class "rounded-1 p1 light"}
                                (util/route-to events/navigate-stylist-dashboard-cash-out-now))
                               [:span.ml1 "Cash Out Now"]
                               [:span.ml2
                                (svg/dropdown-arrow {:class  "stroke-white"
                                                     :width  "12px"
                                                     :height "10px"
                                                     :style  {:transform "rotate(-90deg)"}})])]]
      [:div
       [:.py2.h0 svg/large-dollar]
       [:div "See your available earnings here."]])]])

(defn next-payout-slide [{:keys [amount]}]
  [:div.my4.clearfix
   [:.my4
    {:key "next-payment" :class stat-card}
    [:.p1 "NEXT PAYMENT"]
    (if (> amount 0)
      [:div
       [:.py2.h0 re-center-money (ui/big-money amount)]
       [:div "Payment " (in-x-days)]]
      [:div
       [:.py2.h0 svg/large-dollar]
       [:div "See your next payment amount here."]])]])

(defn old-lifetime-payouts-slide [{:keys [amount]}]
  [:div.my4.clearfix
   [:.my4
    {:class stat-card :key "render-stat"}
    [:.p1 "LIFETIME COMMISSIONS"]
    (if (> amount 0)
      [:div
       [:.py2.h0 re-center-money [:div.line-height-1 (mf/as-money-without-cents amount)]]
       [:div "Sales since you joined Mayvenn"]]
      [:div
       [:.py2.h0 svg/large-percent]
       [:div "All sales since you joined Mayvenn."]])]])

(defn lifetime-stats-slide [{:keys [total-paid-out]}]
  [:div.my4.clearfix
   [:.my4
    {:class stat-card :key "render-stat"}
    [:.p1 "LIFETIME COMMISSIONS"]
    (if (> total-paid-out 0)
      [:div
       [:.py2.h0 re-center-money [:div.line-height-1 (mf/as-money-without-cents total-paid-out)]]
       [:div "Sales since you joined Mayvenn"]]
      [:div
       [:.py2.h0 svg/large-percent]
       [:div "All sales since you joined Mayvenn."]])]])

(defn stylist-dashboard-stats-component [{:keys [stats show-cash-out-now-ui?]} owner]
  (om/component
   (html
    (let [items [(previous-payout-slide (:previous-payouts stats))
                 (if show-cash-out-now-ui?
                   (cash-out-now-slide (:next-payout stats))
                   (next-payout-slide (:next-payout stats)))
                 (old-lifetime-payouts-slide (:lifetime-payouts stats))]
          initial-slide-index (if show-cash-out-now-ui? 1 0)]
      [:div.bg-teal.white.center
       [:div.bg-darken-bottom-1
        (om/build carousel/component
                  {:slides   items
                   :settings {:arrows true
                              :dots   true
                              :swipe  true
                              :initialSlide initial-slide-index}}
                  {:react-key "stat-swiper"})]]))))

(defn new-stylist-dashboard-stats-component [{:keys [payout-stats show-cash-out-now-ui?]} owner]
  (om/component
   (html
    (let [items [(previous-payout-slide (:previous-payout payout-stats))
                 (if show-cash-out-now-ui?
                   (cash-out-now-slide (:next-payout payout-stats))
                   (next-payout-slide (:next-payout payout-stats)))
                 (lifetime-stats-slide (:lifetime-stats payout-stats))]
          initial-slide-index (if show-cash-out-now-ui? 1 0)]
      [:div.bg-teal.white.center
       [:div.bg-darken-bottom-1
        (om/build carousel/component
                  {:slides   items
                   :settings {:arrows true
                              :dots   true
                              :swipe  true
                              :initialSlide initial-slide-index}}
                  {:react-key "stat-swiper"})]]))))
