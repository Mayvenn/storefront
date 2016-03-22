(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.svg :as svg]
            [storefront.components.utils :as utils]))

(defn display-stylist-bonus [{:keys [revenue-surpassed amount created-at]}]
  [:.gray.border-bottom.border-white.flex.items-center.justify-between.py1
   [:.mr1
    (svg/adjustable-check {:width "12px" :height "12px" :class "stroke-green"})]
   [:.flex-auto.h6
    "Credit Earned: " (f/as-money-without-cents amount)
    " on " (f/epoch-date created-at)]
   [:.h3.ml1.mr1.strike (f/as-money-without-cents revenue-surpassed)]])

(defn bonus-history-component [data]
  (om/component
   (html
    (when-let [bonuses (seq (get-in data keypaths/stylist-bonuses-history))]
      [:div.border-top.border-white.mx2.py2
       [:.h6.gray.mb1 "Sales Goals"]

       (map display-stylist-bonus bonuses)

       (pagination/fetch-more
        data
        events/control-stylist-bonuses-fetch
        (get-in data keypaths/stylist-bonuses-page)
        (get-in data keypaths/stylist-bonuses-pages))]))))

(defn pending-bonus-progress-component [data]
  (om/component
   (html
    (let [progress  (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)
          milestone (get-in data keypaths/stylist-bonuses-milestone-amount)
          bar-value (min 100 (/ progress (/ milestone 100.0)))
          bar-width (str (max 15 bar-value) "%")
          bar-padding-y {:padding-top ".25em"
                         :padding-bottom ".25em"}]
      [:div.my2.border.border-silver.capped
       (if (zero? progress)
         [:div.gray.left-align.px1 {:style bar-padding-y} "0%"]
         [:div.bg-teal.white.right-align.px2.bg-lighten-top-2.capped.inset-top-2
          {:style (merge bar-padding-y {:width bar-width})}
          (str (.toFixed bar-value 0) "%")])]))))

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
          lifetime-total (get-in data keypaths/stylist-bonuses-lifetime-total)]

      [:.mx-auto.container.border.border-white {:data-test "bonuses-panel"}
       (when award-amount
         [:.clearfix.mb3
          [:.sm-col.sm-col-8.p1
           [:.center.px1.py2
            [:.h3
             "Sell "
             (f/as-money (- milestone-amount progress-amount))
             " more to earn your next bonus!"]

            (om/build pending-bonus-progress-component data)

            [:.h6.gray
             "You earn "
             (f/as-money-without-cents award-amount)
             " in credit for every "
             (f/as-money-without-cents milestone-amount)
             " in sales you make."]]

           (om/build bonus-history-component data)

           (when (pos? available-credit)
             [:.center.bg-white.p2.line-height-3
              [:p
               "Bonus credits available "
               [:span.green (f/as-money available-credit)]
               [:br]
               " why not treat yourself?"]

              [:p [:a.btn.teal (utils/route-to data events/navigate-cart) "Shop now " utils/rarr]]])]


          [:.sm-col-right.sm-col-4
           (when lifetime-total
             (show-lifetime-total lifetime-total))]])]))))
