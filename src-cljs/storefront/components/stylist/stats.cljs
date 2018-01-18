(ns storefront.components.stylist.stats
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]))

(def ordered-stats [:next-payout :previous-payout :lifetime-payouts])

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

(defmulti render-stat (fn [name stat] name))

(defmethod render-stat :previous-payout [_ {:keys [amount date]}]
  [:.my4
   {:key "previous-payout" :class stat-card}
   [:.p1 "LAST PAYMENT"]
   (if (> amount 0)
     [:div
      [:.py2.h0 re-center-money (ui/big-money amount)]
      [:div "On " (f/long-date date)]]
     [:div
      [:.py2.h0 svg/large-payout]
      [:div "Your last payment will show here."]])])

(defmethod render-stat :next-payout [_ {:keys [amount]}]
  [:.my4
   {:key "next-payment" :class stat-card}
   [:.p1 "NEXT PAYMENT"]
   (if (> amount 0)
     [:div
      [:.py2.h0 re-center-money (ui/big-money amount)]
      [:div "Payment " (in-x-days)]]
     [:div
      [:.py2.h0 svg/large-dollar]
      [:div "See your next payment amount here."]])])

(defmethod render-stat :lifetime-payouts [_ {:keys [amount]}]
  [:.my4
   {:class stat-card :key "render-stat"}
   [:.p1 "LIFETIME COMMISSIONS"]
   (if (> amount 0)
     [:div
      [:.py2.h0 re-center-money [:div.line-height-1 (mf/as-money-without-cents amount)]]
      [:div "Sales since you joined Mayvenn"]]
     [:div
      [:.py2.h0 svg/large-percent]
      [:div "All sales since you joined Mayvenn."]])])

(defn stylist-dashboard-stats-component [{:keys [stats]} owner]
  (om/component
   (html
    (let [items (vec (for [[_ stat] (map-indexed vector ordered-stats)] ;;NOTE Unsure of why we did this...
                       [:.my4.clearfix
                        (render-stat stat (get stats stat))]))]
      [:div.bg-teal.white.center
       [:div.bg-darken-bottom-1
        (om/build carousel/component
                  {:slides items
                   :settings {:arrows true
                              :dots true
                              :swipe true}}
                  {:react-key "stat-swiper"})]]))))
