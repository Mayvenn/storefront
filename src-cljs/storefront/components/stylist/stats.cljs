(ns storefront.components.stylist.stats
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.messages :as messages]
            [swipe :as swipe]
            [storefront.components.carousel :as carousel]))

(def ordered-stats [:previous-payout :next-payout :lifetime-payouts])

(defn ^:private money-with-cents [amount]
  [:.flex.justify-center.line-height-1
   (f/as-money-without-cents amount)
   [:span.h5 {:style {:margin "5px 3px"}} (f/as-money-cents-only amount)]])

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
      [:.py2.h00 re-center-money (money-with-cents amount)]
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
      [:.py2.h00 re-center-money (money-with-cents amount)]
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
      [:.py2.h00 re-center-money (f/as-money-cents-only amount)]
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

(defn stylist-dashboard-stats-component [{:keys [selected stats]} owner]
  (om/component
   (html
    (let [handler (partial messages/handle-message events/control-stylist-stat-move)
          items (vec (for [[idx stat] (map-indexed vector ordered-stats)]
                       {:id idx
                        :body (render-stat stat (get stats stat))}))]
      [:.bg-green.white.center.sans-serif
       [:.py1.bg-darken-bottom-2
        (om/build carousel/swipe-component
                  {:selected-index selected
                   :items items}
                  {:opts {:handler (comp handler :id)}})
        [:.flex.justify-center
         (for [{:keys [id]} items]
           [:.p1.pointer {:key id :on-click (fn [_] (handler id))}
            [:.bg-white.circle
             {:class (when-not (= selected id) "bg-lighten-2")
              :style {:width "8px" :height "8px"}}]])]]]))))
