(ns storefront.components.stylist.stats
  (:require [storefront.accessors.payouts :as payouts]
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]))

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

(defmulti payout-slide (fn [slide-name next-payout]
                         slide-name))

(defmethod payout-slide :stats/cash-out-now [_ {:keys [next-payout]}]
  (let [amount (:amount next-payout)]
    [:div.my4.clearfix
     [:.my4
      {:key "available-earnings" :class stat-card}
      [:.p1 "AVAILABLE EARNINGS"]
      (if (> amount 0)
        [:div
         [:.py2.h0 re-center-money (ui/big-money amount)]
         (when (payouts/cash-out-eligible? (:payout-method next-payout))
           [:div.col-5.mt1.mb2.mx-auto
            (ui/light-ghost-button {:on-click (utils/send-event-callback events/control-stylist-dashboard-cash-out-now-submit)
                                    :class    "rounded-1 p1 light"
                                    :data-test "cash-out-now-button"}

                                   [:span.ml1 "Cash Out Now"]
                                   [:span.ml2
                                    (svg/dropdown-arrow {:class  "stroke-white"
                                                         :width  "12px"
                                                         :height "10px"
                                                         :style  {:transform "rotate(-90deg)"}})])])]
        [:div
         [:.py2.h0 svg/large-dollar]
         [:div "See your available earnings here."]])]]))

(defmethod payout-slide :stats/next-payout [_ {:keys [next-payout]}]
  (let [amount (:amount next-payout)]
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
         [:div "See your next payment amount here."]])]]))

(defmethod payout-slide :stats/transfer-in-progress [_ {:keys [initiated-payout]}]
  (let [amount (:amount initiated-payout)]
    [:div.my4.clearfix
     [:.my4
      {:key "available-earnings" :class stat-card}
      [:.p1 "AVAILABLE EARNINGS"]
      [:div
       [:.py2.h0 re-center-money (ui/big-money amount)]
       [:div.col-5.mt1.mb2.mx-auto "Transfer in progress"]]]]))

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

(defn query
  [data]
  (let [payout-stats       (get-in data keypaths/stylist-payout-stats)
        payout-method      (-> payout-stats :next-payout :payout-method)
        cash-out-eligible? (payouts/cash-out-eligible? payout-method)
        next-payout-slide  (cond
                             (-> payout-stats
                                 :initiated-payout
                                 :status-id)    :stats/transfer-in-progress
                             cash-out-eligible? :stats/cash-out-now
                             :else              :stats/next-payout)]
    {:payout-stats        payout-stats
     :next-payout-slide   next-payout-slide
     :initial-slide-index (if (= :stats/cash-out-now next-payout-slide) 1 0) }))

(defn component
  [{:keys [payout-stats next-payout-slide initial-slide-index]} owner]
  (component/create
   [:div.bg-teal.white.center
    [:div.bg-darken-bottom-1
     (component/build carousel/component
                      {:slides   [(previous-payout-slide (:previous-payout payout-stats))
                                  (payout-slide next-payout-slide payout-stats)
                                  (lifetime-stats-slide (:lifetime-stats payout-stats))]
                       :settings {:arrows       true
                                  :dots         true
                                  :swipe        true
                                  :initialSlide initial-slide-index
                                  :slickGoTo    initial-slide-index}}
                      {:react-key "stat-swiper"})]]))

(defmethod effects/perform-effects events/control-stylist-dashboard-cash-out-now-submit
  [_ _ _ _ app-state]
  (history/enqueue-navigate events/navigate-stylist-dashboard-cash-out-now))
