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
            [swipe :as swipe]))

(defn choose-stat [data stat]
  (utils/send-event-callback data events/control-stylist-view-stat stat))

(defn choose-stat-now [data stat]
  (messages/send data events/control-stylist-view-stat stat))

(def ordered-stats [:previous-payout :next-payout :lifetime-payouts])
(def default-stat :next-payout) ; NOTE: this should match the `selected-stylist-stat` in `storefront.state`

(defn stat->idx [stat]
  (utils/position #(= % stat) ordered-stats))

(defn idx->stat [idx]
  (get ordered-stats idx default-stat))

(def default-idx (stat->idx default-stat))

(defn ^:private circle [selected]
  [:div.bg-white.circle
   {:class (when-not selected "bg-lighten-3")
    :style {:width "8px" :height "8px"}}])

(defn ^:private circle-for-stat [data selected stat]
  [:div.p1.pointer
   {:on-click (choose-stat data stat)}
   (circle (= selected stat))])

;; TODO: h00 is 64px when base font-size is 16px, but we need it to be 64 on
;; small screens, where base font-size is 12px
(def really-big {:style {:font-size "64px"}})

(defn ^:private money [amount]
  (f/as-money-without-cents amount))

(defn ^:private money-with-cents [amount]
  [:div.flex.justify-center
   (money amount)
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

(defmulti render-stat (fn [data keypath] keypath))

(defmethod render-stat keypaths/stylist-stats-previous-payout [data keypath]
  (let [{:keys [amount date]} (get-in data keypath)]
    [:div.my3
     {:class stat-card}
     [:div.p1 "LAST PAYOUT"]
     (if (> amount 0)
       (list
        [:div.py2 really-big (money-with-cents amount)]
        [:div (f/long-date date)])
       (list
        [:div.py2 svg/large-payout]
        [:div "Tip: Your last payout will show here."]))]))

(defmethod render-stat keypaths/stylist-stats-next-payout [data keypath]
  (let [{:keys [amount]} (get-in data keypath)]
    [:div.my3
     {:class stat-card}
     [:div.p1 "NEXT PAYOUT"]
     (if (> amount 0)
       (list
        [:div.py2 really-big (money-with-cents amount)]
        [:div "Payment " (in-x-days)])
       (list
        [:div.py2 svg/large-dollar]
        [:div "Tip: Your next payout will show here."]))]))

(defmethod render-stat keypaths/stylist-stats-lifetime-payouts [data keypath]
  (let [{:keys [amount]} (get-in data keypath)]
    [:div.my3
     {:class stat-card}
     [:div.p1 "LIFETIME COMMISSIONS"]
     (if (> amount 0)
       (list
        [:div.py2 really-big (money amount)]
        [:div utils/nbsp])
       (list
        [:div.py2 svg/large-percent]
        [:div "Tip: Lifetime commissions will show here."]))]))

(defn stylist-dashboard-stats-component [data owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (om/set-state!
       owner
       {:swiper (js/Swipe. (om/get-node owner "stats")
                           #js {:continuous false
                                :startSlide (or (stat->idx (get-in data keypaths/selected-stylist-stat))
                                                default-idx)
                                :callback (fn [idx _]
                                            (choose-stat-now data (idx->stat idx)))})}))
    om/IWillUnmount
    (will-unmount [this]
      (when-let [swiper (:swiper (om/get-state owner))]
        (.kill swiper)))
    om/IRenderState
    (render-state [_ {:keys [swiper]}]
      (html
       (let [selected (get-in data keypaths/selected-stylist-stat)
             selected-idx (stat->idx selected)]
         (when (and swiper selected-idx)
           (let [delta (- (.getPos swiper) selected-idx)]
             (if (pos? delta)
               (dotimes [_ delta] (.prev swiper))
               (dotimes [_ (- delta)] (.next swiper)))))
         [:div.py1.bg-teal.bg-lighten-top-2.white.center.sans-serif
          [:div.overflow-hidden.relative
           {:ref "stats"}
           [:div.overflow-hidden.relative
            (for [stat ordered-stats]
              (render-stat data (conj keypaths/stylist-stats stat)))]]

          [:div.flex.justify-center
           (for [stat ordered-stats]
             (circle-for-stat data selected stat))]])))))
