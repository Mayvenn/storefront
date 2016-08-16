(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]))

(def check-svg
  (html (svg/adjustable-check {:width "1em" :height "1em" :class "stroke-green"})))

(defn display-stylist-bonus [{:keys [revenue-surpassed amount created-at]}]
  [:.gray.flex.items-center.justify-between.py1
   {:key revenue-surpassed}
   [:.mr1 check-svg]
   [:.flex-auto.h5
    "Credit Earned: " (mf/as-money-without-cents amount) " on " (f/epoch-date created-at)]
   [:.h3.ml1.mr1.strike (mf/as-money-without-cents revenue-surpassed)]])

(defn bonus-history-component [{:keys [history page pages fetching?]}]
  (om/component
   (html
    (when history
      [:.mx2.py2
       [:.h5.mb1 "Sales Goals"]

       (map display-stylist-bonus history)

       (pagination/fetch-more events/control-stylist-bonuses-fetch fetching? page pages)]))))

(defn pending-bonus-progress [{:keys [progress milestone]}]
  (let [bar-value (-> progress (/ milestone) (* 100.0) (min 100))
        bar-width (str (max 15 bar-value) "%")
        bar-padding-y {:padding-top "0.3em" :padding-bottom "0.15em"}]
    [:.my2.border.border-dark-white.capped.h3.light
     (if (zero? progress)
       [:.light-gray.left-align.px2.self-center.flex.items-center {:style bar-padding-y}
        [:.flex-auto "0%"]]
       [:.bg-green.white.px2.capped.flex.items-center
        {:style (merge bar-padding-y {:width bar-width})}
        [:.right-align.flex-auto
         (str (.toFixed bar-value 0) "%")]])]))

(defn show-lifetime-total [lifetime-total]
  (let [message (goog.string/format "You have earned %s in bonus credits since you joined Mayvenn."
                                    (mf/as-money-without-cents lifetime-total))]
    [:.h6.dark-silver
     [:.p3.to-sm-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.my3.flex.justify-center.items-center.sm-up-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn component [{:keys [available-credit
                                         award-amount
                                         milestone-amount
                                         progress-amount
                                         lifetime-total
                                         page
                                         pages
                                         history
                                         fetching?]}]
  (om/component
   (html
    (if (and (empty? history) fetching?)
      [:.my2.h1 ui/spinner]
      [:.mx-auto.container {:data-test "bonuses-panel"}
       [:.clearfix.mb3
        [:.sm-up-col.sm-up-col-8
         (when award-amount
           [:div
            [:.center.px1.py2
             (cond
               history                [:.h3 "Sell " (mf/as-money (- milestone-amount progress-amount)) " more to earn your next bonus!"]
               (pos? progress-amount) [:.h3 "Sell " (mf/as-money (- milestone-amount progress-amount)) " more to earn your first bonus!"]
               :else                  [:.h3 "Sell " (mf/as-money-without-cents milestone-amount) " to earn your first bonus!"])

             (pending-bonus-progress {:progress  progress-amount
                                      :milestone milestone-amount})

             [:.h6.gray
              "You earn "
              (mf/as-money-without-cents award-amount)
              " in credit for every "
              (mf/as-money-without-cents milestone-amount)
              " in sales you make."]]

            (om/build bonus-history-component
                      {:history history
                       :page    page
                       :pages   pages
                       :fetching? fetching?})

            (when (pos? available-credit)
              [:.center.bg-white.p2.line-height-2
               [:p
                "Bonus credits available " [:span.navy (mf/as-money available-credit)]
                [:br]
                "Why not treat yourself?"]

               [:p.btn.mt1
                [:a.navy
                 (utils/route-to events/navigate-categories)
                 "Shop now " ui/rarr]]])])]

        [:.sm-up-col-right.sm-up-col-4
         (when (pos? lifetime-total)
           (show-lifetime-total lifetime-total))]]]))))

(defn query [data]
  {:available-credit      (get-in data keypaths/user-total-available-store-credit)
   :award-amount          (get-in data keypaths/stylist-bonuses-award-amount)
   :milestone-amount      (get-in data keypaths/stylist-bonuses-milestone-amount)
   :progress-amount       (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)
   :lifetime-total        (get-in data keypaths/stylist-bonuses-lifetime-total)
   :page                  (get-in data keypaths/stylist-bonuses-page)
   :pages                 (get-in data keypaths/stylist-bonuses-pages)
   :history               (seq (get-in data keypaths/stylist-bonuses-history))
   :fetching?             (utils/requesting? data request-keys/get-stylist-bonus-credits)})
