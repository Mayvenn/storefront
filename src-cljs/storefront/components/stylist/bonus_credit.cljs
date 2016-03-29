(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.navigation :as navigation]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.utils.query :as query]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.svg :as svg]
            [storefront.components.utils :as utils]))

(defn display-stylist-bonus [{:keys [revenue-surpassed amount created-at]}]
  [:.gray.border-bottom.border-white.flex.items-center.justify-between.py1
   [:.mr1
    (svg/adjustable-check {:width "12px" :height "12px" :class "stroke-green"})]
   [:.flex-auto.h5
    "Credit Earned: " (f/as-money-without-cents amount)
    " on " (f/epoch-date created-at)]
   [:.h3.ml1.mr1.strike (f/as-money-without-cents revenue-surpassed)]])

(defn bonus-history-component [{:keys [history page pages fetching?]}]
  (om/component
   (html
    (when history
      [:div.border-top.border-white.mx2.py2
       [:.h5.gray.mb1 "Sales Goals"]

       (map display-stylist-bonus history)

       (pagination/fetch-more events/control-stylist-bonuses-fetch
                              fetching?
                              page
                              pages)]))))

(defn pending-bonus-progress [{:keys [progress milestone]}]
  (let [bar-value (min 100 (/ progress (/ milestone 100.0)))
        bar-width (str (max 15 bar-value) "%")
        bar-padding-y {:padding-top ".25em"
                       :padding-bottom ".2em"}]
    [:div.my2.border.border-silver.capped
     (if (zero? progress)
       [:div.gray.left-align.px1 {:style bar-padding-y} "0%"]
       [:div.bg-teal-gradient.border.border-dark-teal.white.bg-embossed.right-align.px2.capped.engrave-2.line-height-1
        {:style (merge bar-padding-y {:width bar-width})}
        (str (.toFixed bar-value 0) "%")])]))

(defn show-lifetime-total [lifetime-total]
  (let [message (goog.string/format "You have earned %s in bonus credits since you joined Mayvenn."
                                    (f/as-money-without-cents lifetime-total))]
    [:.h6.muted
     [:.p3.to-sm-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.my3.flex.justify-center.items-center.sm-up-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn stylist-bonus-credit-component [data]
  (om/component
   (html
    (let [available-credit (get-in data keypaths/user-total-available-store-credit)
          award-amount     (get-in data keypaths/stylist-bonuses-award-amount)
          milestone-amount (get-in data keypaths/stylist-bonuses-milestone-amount)
          progress-amount  (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)
          lifetime-total   (get-in data keypaths/stylist-bonuses-lifetime-total)
          page             (get-in data keypaths/stylist-bonuses-page)
          pages            (get-in data keypaths/stylist-bonuses-pages)
          history          (seq (get-in data keypaths/stylist-bonuses-history))
          fetching?        (query/get {:request-key request-keys/get-stylist-bonus-credits}
                                      (get-in data keypaths/api-requests))]
      (if (and (empty? history) fetching?)
        (utils/spinner {:height "100px"})
        [:.mx-auto.container {:data-test "bonuses-panel"}
         [:.clearfix.mb3
          [:.sm-col.sm-col-8
           (when award-amount
             (list
              [:.center.px1.py2
               (cond
                 history                [:.h3 "Sell " (f/as-money (- milestone-amount progress-amount)) " more to earn your next bonus!"]
                 (pos? progress-amount) [:.h3 "Sell " (f/as-money (- milestone-amount progress-amount)) " more to earn your first bonus!"]
                 :else                  [:.h3 "Sell " (f/as-money-without-cents milestone-amount) " to earn your first bonus!"])

               (pending-bonus-progress {:progress  progress-amount
                                        :milestone milestone-amount})

               [:.h6.gray
                "You earn "
                (f/as-money-without-cents award-amount)
                " in credit for every "
                (f/as-money-without-cents milestone-amount)
                " in sales you make."]]

              (om/build bonus-history-component
                        {:history history
                         :page    page
                         :pages   pages
                         :fetching? fetching?})

              (when (pos? available-credit)
                [:.center.bg-white.p2.line-height-2
                 [:p
                  "Bonus credits available " [:span.green (f/as-money available-credit)]
                  [:br]
                  "Why not treat yourself?"]

                 [:p.btn.mt1
                  [:a.teal
                   (apply utils/route-to (navigation/shop-now-navigation-message data))
                   "Shop now " utils/rarr]]])))]

          [:.sm-col-right.sm-col-4
           (when (pos? lifetime-total)
             (show-lifetime-total lifetime-total))]]])))))
