(ns storefront.components.stylist.dashboard-aladdin
  (:require [storefront.component :as component]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.earnings :as earnings]
            [storefront.api :as api]
            [storefront.platform.messages :as messages]
            [storefront.platform.numbers :as numbers]
            [storefront.accessors.payouts :as payouts]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.stylist.stats :as stats]
            [storefront.components.money-formatters :as mf]
            [storefront.platform.component-utils :as utils]
            [storefront.components.tabs :as tabs]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]))

(defn earnings-count [title value]
  [:div.dark-gray.letter-spacing-0
   [:div.shout.h6 title]
   [:div.black.medium.h5 value]])

(defn progress-indicator [{:keys [value maximum]}]
  (let [bar-value (-> value (/ maximum) (* 100.0) (min 100))
        bar-width (str (numbers/round bar-value) "%")
        bar-style {:height "5px"}]
    [:div.bg-gray.flex-auto
     (cond
       (zero? value) [:div.px2 {:style bar-style}]
       (= value maximum) [:div.bg-teal.px2 {:style bar-style}]
       :else [:div.bg-teal.px2 {:style (merge bar-style {:width bar-width})}])]))

(defn ^:private cash-balance-card
  [{:as earnings :keys [cash-balance lifetime-earnings monthly-earnings]}
   {:as services :keys [lifetime-services monthly-services]}]
  [:div.h6.bg-too-light-teal.p2

   [:div.letter-spacing-1.shout.dark-gray.mbnp5.flex.items-center
    "Cash Balance"
    (svg/dropdown-arrow {:class  "ml1 stroke-dark-gray rotate-180"
                         :style  {:stroke-width "2"}
                         :height ".75em"
                         :width  ".75em"})]

   [:div.flex.items-center
    [:div.col-5
     [:div.h1.black.bold.flex (mf/as-money-without-cents cash-balance)]]
    [:div.col-7
     (if false #_(payouts/cash-out-eligible? payout-method)
       (ui/teal-button
        {:height-class "py2"
         :on-click  (utils/send-event-callback events/control-stylist-dashboard-cash-out-submit)
                                        ;:disabled? (not (payouts/cash-out-eligible? payout-method))
                                        ;:spinning? cashing-out?
         }
        [:div.flex.items-center.justify-center.regular.h5
         (ui/ucare-img {:width "28" :class "mr2 flex items-center"} "3d651ddf-b37d-441b-a162-b83728f2a2eb")
         "Cash Out"])
       [:div.h7.right "Cash out now with " [:a.teal (utils/fake-href events/navigate-stylist-account-commission) "Mayvenn InstaPay"]])]]
   [:div.flex.mt2
    [:div.col-7
     (earnings-count "Monthly Earnings" (mf/as-money-without-cents monthly-earnings))]
    [:div.col-5
     (earnings-count "Lifetime Earnings" (mf/as-money-without-cents lifetime-earnings))]]
   [:div.flex.pt2
    [:div.col-7
     (earnings-count "Monthly Services" monthly-services)]
    [:div.col-5
     (earnings-count "Lifetime Services" lifetime-services)]]])

(defn ^:private store-credit-balance-card [total-available-store-credit lifetime-earned]
  [:div.h6.bg-too-light-teal.p2
   [:div.letter-spacing-1.shout.dark-gray.mbnp5.flex.items-center
    "Store Credit Balance"
    (svg/dropdown-arrow {:class  "ml1 stroke-dark-gray rotate-180"
                         :style  {:stroke-width "2"}
                         :height ".75em"
                         :width  ".75em"})]

   [:div.flex.items-center
    [:div.col-7
     [:div.h1.black.bold.flex (mf/as-money-without-cents total-available-store-credit)]]
    [:div.col-5
     (ui/teal-button
      {:height-class "py2"
       :disabled? (zero? total-available-store-credit)}
      [:div.flex.items-center.justify-center.regular.h5
       (ui/ucare-img {:width "28" :class "mr2 flex items-center"} "81775e67-9a83-46b7-b2ae-1cdb5a737876")
       "Shop"])]]
   [:div.flex.pt2
    [:div.col-7
     (earnings-count "Lifetime Bonuses" (mf/as-money-without-cents lifetime-earned))]]])

(defn ^:private sales-bonus-progress [{:keys [previous-level next-level award-for-next-level total-eligible-sales]}]
  [:div.p2
   [:div.h6.letter-spacing-1.shout.dark-gray "Sales Bonus Progress"]
   [:div.h7
    "Sell "
    (mf/as-money-without-cents (- next-level total-eligible-sales))
    " more in non-FREEINSTALL sales to earn your next "
    [:span.bold (mf/as-money-without-cents award-for-next-level)]
    " in credit."]
   [:div.mtp2
    (progress-indicator {:value   (- total-eligible-sales previous-level)
                         :maximum (- next-level previous-level)})]])

(def tabs
  [{:id :orders
    :title "Orders"
    :navigate events/navigate-stylist-v2-dashboard-orders}
   {:id :payments
    :title "Payments"
    :navigate events/navigate-stylist-v2-dashboard-payments}])

(defn ^:private activity-ledger [{:keys [active-tab-name empty-copy empty-title]} table-data]
  [:div
   [:div.flex.flex-wrap
    (for [{:keys [id title navigate]} tabs]
      [:a.h6.col-6.p2.black
       (merge (utils/fake-href navigate)
              {:key (name id)
               :class (if (= id active-tab-name)
                        "bg-gray bold"
                        "bg-light-gray")})
       title])]
   (if (seq table-data)
     [:div.my6.center
      [:h4.gray.bold "TK"]
      [:h6.dark-gray "TK TK TK TK TK TK TK"]]
     [:div.my6.center
      [:h4.gray.bold.p1 empty-title]
      [:h6.dark-gray.col-5.mx-auto.line-height-2 empty-copy]])])

(defn component
  [{:keys [stats total-available-store-credit activity-ledger-tab]} owner opts]
  (let [{:keys [bonuses earnings services]} stats
        {:keys [lifetime-earned]}           bonuses]
    (component/create
     [:div
      [:div.p2
       (cash-balance-card earnings services)
       [:div.mt2 (store-credit-balance-card total-available-store-credit lifetime-earned)]
       (sales-bonus-progress bonuses)]
      (activity-ledger activity-ledger-tab [])])))

(defn query
  [data]
  {:stats                        (get-in data keypaths/stylist-v2-dashboard-stats)
   :payout-method                nil ;; TODO Finish this.
   :activity-ledger-tab          ({events/navigate-stylist-v2-dashboard-payments {:active-tab-name :payments
                                                                                  :empty-copy "Payments and bonus activity will appear here."
                                                                                  :empty-title "No payments yet"}
                                   events/navigate-stylist-v2-dashboard-orders   {:active-tab-name :orders
                                                                                  :empty-copy "Orders from your store will appear here."
                                                                                  :empty-title "No orders yet"}}
                                  (get-in data keypaths/navigation-event))
   :total-available-store-credit (get-in data keypaths/user-total-available-store-credit)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-stylist-v2-dashboard-payments [_ event args _ app-state]
  (messages/handle-message events/stylist-v2-dashboard-stats-fetch))

(defmethod effects/perform-effects events/stylist-v2-dashboard-stats-fetch [_ event args _ app-state]
  (let [stylist-id (get-in app-state keypaths/store-stylist-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (when (and user-id user-token)
      (api/get-stylist-dashboard-stats events/api-success-stylist-v2-dashboard-stats
                                       stylist-id
                                       user-id
                                       user-token))))

(defmethod transitions/transition-state events/api-success-stylist-v2-dashboard-stats
  [_ event {:as stats :keys [earnings services store-credit-balance bonuses]} app-state]
  (assoc-in app-state keypaths/stylist-v2-dashboard-stats stats))
