(ns storefront.components.stylist.dashboard-aladdin
  (:require [storefront.component :as component]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.earnings :as earnings]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.stylist.stats :as stats]
            [storefront.components.tabs :as tabs]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]))

(defn earnings-count [title value]
  [:div.dark-gray.letter-spacing-0
   [:div.shout.h6 title]
   [:div.black.medium.h5 value]])

(defn component
  [{:keys []} owner opts]
  (component/create
   [:div.p3-on-mb
    [:div.h6.bg-too-light-teal.p2

     [:div.letter-spacing-1
      [:div.shout.dark-gray "Cash Balance"]]

     [:div.flex.items-center
      [:div.col-7
       [:div.h1.black.medium.flex "$130"]]
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
       (earnings-count "Lifetime Services" "57")]]]

    [:div.mt2.h6.bg-too-light-teal.p2

     [:div.letter-spacing-1
      [:div.shout.dark-gray "Store Credit Balance"]]

     [:div.flex.items-center
      [:div.col-7
       [:div.h1.black.medium.flex "$100"]]
      [:div.col-5
       (ui/teal-button
        {:height-class "py2"}
        [:div.flex.items-center.justify-center.regular.h5
         (ui/ucare-img {:width "28" :class "mr2 flex items-center"} "81775e67-9a83-46b7-b2ae-1cdb5a737876")
         "Shop"])]]
     [:div.flex.pt2
      [:div.col-7
       (earnings-count "Lifetime Bonuses" "$955")]]]]))
(defn query
  [data]
  {:nav-event (get-in data keypaths/navigation-event)
   :earnings  (earnings/query data)
   :bonuses   (bonuses/query data)
   :referrals (referrals/query data)
   :stats     (stats/query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
