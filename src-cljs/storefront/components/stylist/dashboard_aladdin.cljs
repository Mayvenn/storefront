(ns storefront.components.stylist.dashboard-aladdin
  (:require [storefront.component :as component]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.earnings :as earnings]
            [storefront.platform.numbers :as numbers]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.stylist.stats :as stats]
            [storefront.components.tabs :as tabs]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
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

(defn ^:private cash-balance-card []
  [:div.h6.bg-too-light-teal.p2

   [:div.letter-spacing-1.shout.dark-gray.mbnp5.flex.items-center
    "Cash Balance"
    (svg/dropdown-arrow {:class  "ml1 stroke-dark-gray rotate-180"
                         :style  {:stroke-width "2"}
                         :height ".75em"
                         :width  ".75em"})]

   [:div.flex.items-center
    [:div.col-7
     [:div.h1.black.bold.flex "$130"]]
    [:div.col-5
     (ui/teal-button
      {:height-class "py2"}
      [:div.flex.items-center.justify-center.regular.h5
       (ui/ucare-img {:width "28" :class "mr2 flex items-center"} "3d651ddf-b37d-441b-a162-b83728f2a2eb")
       "Cash Out"])]]
   [:div.flex.mt2
    [:div.col-7
     (earnings-count "Monthly Earnings" "$490")]
    [:div.col-5
     (earnings-count "Lifetime Earnings" "$6,255")]]
   [:div.flex.pt2
    [:div.col-7
     (earnings-count "Monthly Services" "4")]
    [:div.col-5
     (earnings-count "Lifetime Services" "57")]]])

(defn ^:private store-credit-balance-card []
  [:div.h6.bg-too-light-teal.p2
   [:div.letter-spacing-1.shout.dark-gray.mbnp5.flex.items-center
    "Store Credit Balance"
    (svg/dropdown-arrow {:class  "ml1 stroke-dark-gray rotate-180"
                         :style  {:stroke-width "2"}
                         :height ".75em"
                         :width  ".75em"})]

   [:div.flex.items-center
    [:div.col-7
     [:div.h1.black.bold.flex "$100"]]
    [:div.col-5
     (ui/teal-button
      {:height-class "py2"}
      [:div.flex.items-center.justify-center.regular.h5
       (ui/ucare-img {:width "28" :class "mr2 flex items-center"} "81775e67-9a83-46b7-b2ae-1cdb5a737876")
       "Shop"])]]
   [:div.flex.pt2
    [:div.col-7
     (earnings-count "Lifetime Bonuses" "$955")]]])

(defn ^:private sales-bonus-progress []
  [:div.p2
   [:div.h6.letter-spacing-1.shout.dark-gray "Sales Bonus Progress"]
   [:div.h7 "You've hit $600 in non-FREEINSTALL sales and earned " [:span.bold "$100"] " in credit."]
   [:div.mtp2
    (progress-indicator {:value   100
                         :maximum 300})]])

(defn component
  [{:keys []} owner opts]
  (component/create
   [:div.p3-on-mb
    (cash-balance-card)
    [:div.mt2 (store-credit-balance-card)]
    (sales-bonus-progress)]))

(defn query
  [data]
  {:nav-event (get-in data keypaths/navigation-event)
   :earnings  (earnings/query data)
   :bonuses   (bonuses/query data)
   :referrals (referrals/query data)
   :stats     (stats/query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
