(ns storefront.components.stylist.stats
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.messages :as messages]
            [swipe :as swipe]))

(defn choose-stat [data stat]
  (utils/send-event-callback data events/control-stylist-view-stat stat))

(defn choose-stat-now [data stat]
  (messages/send data events/control-stylist-view-stat stat))

(defn position [pred coll]
  (first (keep-indexed #(when (pred %2) %1)
                       coll)))

(def ordered-stats [:previous-payout :next-payout :lifetime-payouts])
(def default-stat :next-payout)

(defn position-by-stat [stat]
  (position #(= % stat) ordered-stats))

(defn stat-by-position [idx]
  (get ordered-stats idx :previous-payout))

(def default-position (position-by-stat default-stat))

(defn ^:private circle [selected]
  [:div.bg-white.circle
   {:class (when-not selected "bg-lighten-4")
    :style {:width "8px" :height "8px"}}])

(defn ^:private circle-for-stat [data selected stat]
  [:div.p1.pointer
   {:on-click (choose-stat data stat)}
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

(def stat-card "left col-12 relative")

(defn ^:private previous-payout [{:keys [amount date]}]
  [:div.my3
   {:class stat-card}
   [:div.p1 "LAST PAYOUT"]
   [:div.py2 (big-money-with-cents amount)]
   [:div (f/long-date date)]])

(defn ^:private next-payout [{:keys [amount]}]
  [:div.my3
   {:class stat-card}
   [:div.p1 "NEXT PAYOUT"]
   (if (> amount 0)
     (list
      [:div.py2 (big-money-with-cents amount)]
      [:div "Payment " (in-x-days)])
     (list
      [:div.py2 (big-money 0)]
      [:div utils/nbsp]))])

(defn ^:private lifetime-payouts [{:keys [amount]}]
  [:div.my3
   {:class stat-card}
   [:div.p1 "LIFETIME COMMISSIONS"]
   [:div.py2 (big-money amount)]
   [:div utils/nbsp]])

(defn stylist-dashboard-stats-component [data owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (om/set-state!
       owner
       {:swiper (js/Swipe. (om/get-node owner "stats")
                           #js {:continuous false
                                :startSlide (or (position-by-stat (get-in data keypaths/selected-stylist-stat))
                                                default-position)
                                :callback (fn [idx _]
                                            (choose-stat-now data (stat-by-position idx)))})}))
    om/IWillUnmount
    (will-unmount [this]
      (when-let [swiper (:swiper (om/get-state owner))]
        (.kill swiper)))
    om/IRenderState
    (render-state [_ {:keys [swiper]}]
      (html
       (let [selected (get-in data keypaths/selected-stylist-stat)
             selected-idx (position-by-stat selected)]
         (when (and swiper
                    (not= (.getPos swiper) selected-idx))
           (.slide swiper selected-idx))
         [:div.py1.bg-teal.bg-lighten-top-3.white.center.sans-serif
          [:div.overflow-hidden.relative
           {:ref "stats"}
           [:div.overflow-hidden.relative
            (previous-payout (get-in data keypaths/stylist-stats-previous-payout))
            (next-payout (get-in data keypaths/stylist-stats-next-payout))
            (lifetime-payouts (get-in data keypaths/stylist-stats-lifetime-payouts))]]

          [:div.flex.justify-center
           (circle-for-stat data selected :previous-payout)
           (circle-for-stat data selected :next-payout)
           (circle-for-stat data selected :lifetime-payouts)]])))))
