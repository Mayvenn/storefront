(ns storefront.components.stylist.stats
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn ^:private circle [selected]
  [:div.bg-white.circle
   {:class (when-not selected "bg-lighten-4")
    :style {:width "8px" :height "8px"}}])

(defn ^:private circle-for-stat [data selected stat]
  [:div.p1.pointer
   {:on-click (utils/send-event-callback data events/control-stylist-view-stat stat)}
   (circle (= selected stat))])

(defn ^:private big-money [amount]
  [:span {:style {:font-size "64px"}} (f/as-money-without-cents amount)])

(defn ^:private big-money-with-cents [amount]
  [:div.flex.justify-center
   (big-money amount)
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

(defn ^:private last-payout [amount]
  [:div.my3
   [:div.p1 "LAST PAYOUT"]
   [:div.py2 (big-money-with-cents amount)]
   [:div "FIXME amount and date"]])

(defn ^:private next-payout [amount]
  [:div.my3
   [:div.p1 "NEXT PAYOUT"]
   (if (> amount 0)
     (list
      [:div.py2 (big-money-with-cents amount)]
      [:div "Payment " (in-x-days)])
     (list
      [:div.py2 (big-money 0)]
      [:div utils/nbsp]))])

(defn ^:private commissions [amount]
  [:div.my3
   [:div.p1 "LIFETIME COMMISSIONS"]
   [:div.py2 (big-money amount)]
   [:div utils/nbsp]])

(defn stylist-dashboard-stats-component [data owner]
  (om/component
   (html
    (let [selected (get-in data keypaths/selected-stylist-stat)]
      [:div.p1.bg-teal.bg-lighten-top-3.white.center.sans-serif
       (case selected
         :last-payout
         (last-payout 10000)
         :next-payout
         (next-payout (get-in data keypaths/stylist-commissions-next-amount))
         :lifetime-commissions
         (commissions (get-in data keypaths/stylist-commissions-paid-total)))

       [:div.flex.justify-center
        (circle-for-stat data selected :last-payout)
        (circle-for-stat data selected :next-payout)
        (circle-for-stat data selected :lifetime-commissions)]]))))
