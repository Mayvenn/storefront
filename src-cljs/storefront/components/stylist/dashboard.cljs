(ns storefront.components.stylist.dashboard
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.stylist.stats :refer [stylist-dashboard-stats-component]]
            [storefront.components.stylist.commissions :as commissions]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(def tab-refs ["bonuses" "commissions" "referrals"])
(def nav-events [events/navigate-stylist-dashboard-bonus-credit events/navigate-stylist-dashboard-commissions events/navigate-stylist-dashboard-referrals])
(def labels ["Bonuses" "Commissions" "Referrals"])

(defn tab-link [event ref label]
  [:a.black.center.px3.pt2
   (merge (utils/route-to event) {:key ref})
   [:.py1 {:ref ref
           :data-test (str "nav-" ref)}
    label]])

(defn sliding-indicator [{:keys [navigation-state tab-bounds]}]
  (om/component
   (html
    (let [tab-position         (utils/position #(= % navigation-state) nav-events)
          {:keys [left width]} (get tab-bounds tab-position)]
      [:div.border-navy.border.absolute.transition-ease-in.transition-1
       {:style {:margin-top "-2px"
                :left       (str left "px")
                :width      (str width "px")}}]))))

(defn get-x-dimension [node]
  (if node
    (let [rect (.getBoundingClientRect node)]
      {:left  (.-left rect)
       :width (.-width rect)})
    {:left 0 :width 0}))

(defn nav-component [nav-state owner]
  (letfn [(tab-bounds []
            (let [{parent-left :left} (get-x-dimension (om/get-ref owner "tabs"))]
              (vec (for [tab-ref tab-refs]
                     (let [{:keys [left width]} (get-x-dimension (om/get-ref owner tab-ref))]
                       {:left  (- left parent-left)
                        :width width})))))
          (cache-tab-bounds [] (om/set-state! owner {:tab-bounds (tab-bounds)}))
          (handle-resize-event [e] (cache-tab-bounds))]
    (reify
      om/IDidMount
      (did-mount [this]
        (js/window.addEventListener "resize" handle-resize-event)
        (cache-tab-bounds))
      om/IWillUnmount
      (will-unmount [this]
        (js/window.removeEventListener "resize" handle-resize-event))
      om/IRenderState
      (render-state [this {:keys [tab-bounds]}]
        (html
         [:nav.bg-white.sticky.z1.top-0 {:ref "tabs"}
          [:div.flex.justify-center
           (for [[event ref label] (map vector nav-events tab-refs labels)]
             (tab-link event ref label))]
          (om/build sliding-indicator {:navigation-state nav-state
                                       :tab-bounds       tab-bounds})])))))

(defn stylist-dashboard-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:.legacy-container.sans-serif.black
      (om/build stylist-dashboard-stats-component
                {:stats    (get-in data keypaths/stylist-stats)
                 :selected (get-in data keypaths/selected-stylist-stat)})

      (om/build nav-component (get-in data keypaths/navigation-event))
      (condp = (get-in data keypaths/navigation-event)

        events/navigate-stylist-dashboard-commissions
        (om/build commissions/stylist-commissions-component (commissions/stylist-commissions-query data))

        events/navigate-stylist-dashboard-bonus-credit
        (om/build bonuses/stylist-bonuses-component (bonuses/stylist-bonuses-query data))

        events/navigate-stylist-dashboard-referrals
        (om/build referrals/stylist-referrals-component (referrals/stylist-referrals-query data)))]])))
