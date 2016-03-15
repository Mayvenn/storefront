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

(def ordered-stats [:previous-payout :next-payout :lifetime-payouts])
(def default-stat :next-payout) ; NOTE: this should match the `selected-stylist-stat` in `storefront.state`

(defn position-by-stat [stat]
  (utils/position #(= % stat) ordered-stats))

(defn stat-by-position [idx]
  (get ordered-stats idx default-stat))

(def default-position (position-by-stat default-stat))

(defn ^:private circle [selected]
  [:div.bg-white.circle
   {:class (when-not selected "bg-lighten-4")
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

(def large-dollar-svg
  [:svg {:version "1.1" :viewbox "0 0 72 72" :height "72px" :width "72px"}
   [:g {:fill "none" :stroke-width "1" :stroke "#FFFFFF"}
    [:circle {:cx "36" :cy "36" :r "35" }]
    [:g {:transform "translate(1, 1)"}
     [:path {:d "M27.3913043,42.6086957 C27.3913043,46.8117391 30.7969565,50.2173913 35,50.2173913 C39.2030435,50.2173913 42.6086957,46.8117391 42.6086957,42.6086957 C42.6086957,38.4071739 39.2030435,35 35,35 C30.7969565,35 27.3913043,31.5943478 27.3913043,27.3913043 C27.3913043,23.1897826 30.7969565,19.7826087 35,19.7826087 C39.2030435,19.7826087 42.6086957,23.1897826 42.6086957,27.3913043"}]
     [:path {:d "M35,15.2173913 V54.7826087"}]]]])

(def large-percent-svg
  [:svg {:version "1.1" :viewbox "0 0 72 72" :height "72px" :width "72px"}
  [:g {:fill "none" :stroke-width "1" :stroke "#FFFFFF"}
   [:g {:transform "translate(1, 1)"}
    [:path {:d "M22.826087,47.173913 L47.173913,22.826087"}]
    [:g
     [:path {:d "M30.4347826,25.8695652 C30.4347826,28.391087 28.3895652,30.4347826 25.8695652,30.4347826 C23.3495652,30.4347826 21.3043478,28.391087 21.3043478,25.8695652 C21.3043478,23.3495652 23.3495652,21.3043478 25.8695652,21.3043478 C28.3895652,21.3043478 30.4347826,23.3495652 30.4347826,25.8695652 L30.4347826,25.8695652 Z"}]
     [:path {:d "M48.6956522,44.1304348 C48.6956522,46.6519565 46.6504348,48.6956522 44.1304348,48.6956522 C41.6104348,48.6956522 39.5652174,46.6519565 39.5652174,44.1304348 C39.5652174,41.6104348 41.6104348,39.5652174 44.1304348,39.5652174 C46.6504348,39.5652174 48.6956522,41.6104348 48.6956522,44.1304348 L48.6956522,44.1304348 Z"}]
     [:path {:d "M70,35 C70,54.3306522 54.3306522,70 35,70 C15.6723913,70 0,54.3306522 0,35 C0,15.6708696 15.6723913,0 35,0 C54.3306522,0 70,15.6708696 70,35 L70,35 Z"}]]]]])

(def large-payout-svg
  [:svg {:version "1.1" :viewbox "0 0 62 60" :height "60px" :width "62px"}
   [:g {:fill "none" :stroke-width "1" :stroke "#FFFFFF"}
    [:g
     {:transform "translate(1, 1)"}
     [:path {:d "M0,54.0451909 L10.6086957,54.0451909 L10.6086957,34.2724636 L0,34.2724636 L0,54.0451909 Z"}]
     [:path {:d "M10.6086957,51.4090909 C38.4565217,60.6363636 29.173913,60.6363636 61,44.8181818 C58.1807391,42.0183636 55.9542391,41.3553182 53.0434783,42.1818182 L41.2837391,46.0599091"}]
     [:path {:d "M10.6086957,36.9090909 L18.5652174,36.9090909 C24.8044565,36.9090909 29.173913,40.8636364 30.5,42.1818182 L38.4565217,42.1818182 C42.6827609,42.1818182 42.6827609,47.4545455 38.4565217,47.4545455 L23.8695652,47.4545455"}]
     [:path {:d "M35.8043478,7.90909091 C35.8043478,12.2775455 39.3662174,15.8181818 43.7608696,15.8181818 C48.1555217,15.8181818 51.7173913,12.2775455 51.7173913,7.90909091 C51.7173913,3.54063636 48.1555217,0 43.7608696,0 C39.3662174,0 35.8043478,3.54063636 35.8043478,7.90909091 L35.8043478,7.90909091 Z"}]
     [:path {:d "M23.8695652,26.3636364 C23.8695652,30.7320909 27.4314348,34.2727273 31.826087,34.2727273 C36.2207391,34.2727273 39.7826087,30.7320909 39.7826087,26.3636364 C39.7826087,21.9951818 36.2207391,18.4545455 31.826087,18.4545455 C27.4314348,18.4545455 23.8695652,21.9951818 23.8695652,26.3636364 L23.8695652,26.3636364 Z"}]
     [:path {:d "M31.3714286,23.7272727 L31.3714286,29"}]
     [:path {:d "M43.5714286,5.27272727 L43.5714286,10.5454545"}]]]])

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
        [:div.py2 large-payout-svg]
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
        [:div.py2 large-dollar-svg]
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
        [:div.py2 large-percent-svg]
        [:div "Tip: Lifetime commissions will show here."]))]))

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
         (when (and swiper selected-idx)
           (let [delta (- (.getPos swiper) selected-idx)]
             (if (pos? delta)
               (dotimes [_ delta] (.prev swiper))
               (dotimes [_ (- delta)] (.next swiper)))))
         [:div.py1.bg-teal.bg-lighten-top-3.white.center.sans-serif
          [:div.overflow-hidden.relative
           {:ref "stats"}
           [:div.overflow-hidden.relative
            (for [stat ordered-stats]
              (render-stat data (conj keypaths/stylist-stats stat)))]]

          [:div.flex.justify-center
           (for [stat ordered-stats]
             (circle-for-stat data selected stat))]])))))
