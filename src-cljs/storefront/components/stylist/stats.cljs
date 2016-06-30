(ns storefront.components.stylist.stats
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.platform.carousel :as carousel]))

(def ordered-stats [:previous-payout :next-payout :lifetime-payouts])

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
  [:.my3
   {:key "previous-payout" :class stat-card}
   [:.p1 "LAST PAYMENT"]
   (if (> amount 0)
     [:div
      [:.py2.h00 re-center-money (ui/big-money amount)]
      [:div "On " (f/long-date date)]]
     [:div
      [:div {:style {:padding "18px"}} svg/large-payout]
      [:div "Your last payment will show here."]])])

(defmethod render-stat :next-payout [_ {:keys [amount]}]
  [:.my3
   {:key "next-payment" :class stat-card}
   [:.p1 "NEXT PAYMENT"]
   (if (> amount 0)
     [:div
      [:.py2.h00 re-center-money (ui/big-money amount)]
      [:div "Payment " (in-x-days)]]
     [:div
      [:.py2 svg/large-dollar]
      [:div "See your next payment amount here."]])])

(defmethod render-stat :lifetime-payouts [_ {:keys [amount]}]
  [:.my3
   {:class stat-card :key "render-stat"}
   [:.p1 "LIFETIME COMMISSIONS"]
   (if (> amount 0)
     [:div
      [:.py2.h00 re-center-money (mf/as-money-without-cents amount)]
      [:div "Sales since you joined Mayvenn"]]
     [:div
      [:.py2 svg/large-percent]
      [:div "All sales since you joined Mayvenn."]])])

(defn stats-details-component [stats]
  (om/component
   (html
    [:.overflow-hidden.relative.engrave-2
     (for [stat ordered-stats]
       (render-stat stat (get stats stat)))])))

(defn stylist-dashboard-stats-component [{:keys [stats]} owner]
  (om/component
   (html
    (let [items (vec (for [[idx stat] (map-indexed vector ordered-stats)]
                       {:id idx
                        :body (render-stat stat (get stats stat))}))]
      [:.bg-green.white.center.sans-serif
       [:.bg-darken-bottom-2
        (om/build carousel/swipe-component {:items items :continuous true}
                  {:react-key "stat-swiper"
                   :opts {:starting-item (nth items 1)}})]]))))
