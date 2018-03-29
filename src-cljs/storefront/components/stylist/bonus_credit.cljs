(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [goog.string]))

(def check-svg
  (svg/circled-check {:class "stroke-teal"
                      :style {:width "1.5rem" :height "1.5rem"}}))

(defn display-stylist-bonus [{:keys [revenue-surpassed amount created-at]}]
  [:.dark-gray.flex.items-center.justify-between.py1
   {:key revenue-surpassed}
   [:.mr1 check-svg]
   [:.flex-auto.h6
    "Credit Earned: " (mf/as-money-without-cents amount) " on " (f/epoch-date created-at)]
   [:.h4.ml1.mr1.strike (mf/as-money-without-cents revenue-surpassed)]])

(defn bonus-history-component [{:keys [history page pages fetching?]}]
  (om/component
   (html
    (when history
      [:.mx2.py2
       [:.h6.mb1 "Sales Goals"]

       (map display-stylist-bonus history)

       (pagination/fetch-more events/control-stylist-bonuses-fetch fetching? page pages)]))))


(defn show-lifetime-total [lifetime-total]
  (let [message (goog.string/format "You have earned %s in bonus credits since you joined Mayvenn."
                                    (mf/as-money-without-cents lifetime-total))]
    [:div.h6.dark-gray
     [:div.p3.hide-on-mb
      [:div.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:div.my3.hide-on-tb-dt
      [:div.center message]]]))

(def show-program-terms
  [:.col-right-on-tb-dt.col-4-on-tb-dt
   [:.border-top.border-gray.mx-auto.my2 {:style {:width "100px"}}]
   [:.center.my2.h6
    [:a.dark-gray (utils/route-to events/navigate-content-program-terms) "Mayvenn Program Terms"]]])

(defn component [{:keys [available-credit
                         award-amount
                         milestone-amount
                         progress-amount
                         lifetime-total
                         page
                         pages
                         history
                         fetching?]}
                 owner opts]
  (om/component
   (html
    (if (and (empty? history) fetching?)
      [:.my2.h2 ui/spinner]
      [:.clearfix.mb3
       {:data-test "bonuses-panel"}
       [:.col-on-tb-dt.col-8-on-tb-dt
        (when award-amount
          [:div
           [:.center.px1.py2
            (cond
              history                [:.h4 "Sell " (mf/as-money (- milestone-amount progress-amount)) " more to earn your next bonus!"]
              (pos? progress-amount) [:.h4 "Sell " (mf/as-money (- milestone-amount progress-amount)) " more to earn your first bonus!"]
              :else                  [:.h4 "Sell " (mf/as-money-without-cents milestone-amount) " to earn your first bonus!"])

            [:.mx1
             (ui/progress-indicator {:value  progress-amount
                                     :maximum milestone-amount})]

            [:.h6.dark-gray
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
             [:.center.bg-light-gray.p2
              [:p
               "Bonus credits available " [:span.navy (mf/as-money available-credit)]
               [:br]
               "Why not treat yourself?"]

              [:p.btn.mt1
               [:a.navy
                (utils/route-to events/navigate-shop-by-look {:album-slug "look"})
                "Shop our looks" ui/rarr]]])])]

       [:.col-right-on-tb-dt.col-4-on-tb-dt
        (when (pos? lifetime-total)
          (show-lifetime-total lifetime-total))]
       show-program-terms]))))

(defn query [data]
  {:available-credit (get-in data keypaths/user-total-available-store-credit)
   :award-amount     (get-in data keypaths/stylist-bonuses-award-amount)
   :milestone-amount (get-in data keypaths/stylist-bonuses-milestone-amount)
   :progress-amount  (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)
   :lifetime-total   (get-in data keypaths/stylist-bonuses-lifetime-total)
   :page             (get-in data keypaths/stylist-bonuses-page)
   :pages            (get-in data keypaths/stylist-bonuses-pages)
   :history          (seq (get-in data keypaths/stylist-bonuses-history))
   :fetching?        (utils/requesting? data request-keys/get-stylist-bonus-credits)})
