(ns storefront.components.stylist.dashboard
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.stylist.stats :refer [stylist-dashboard-stats-component]]
            [storefront.components.stylist.commissions :refer [stylist-commissions-component]]
            [storefront.components.stylist.bonus-credit :refer [stylist-bonus-credit-component]]
            [storefront.components.stylist.referrals :refer [stylist-referrals-component]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn link-with-selected [data event ref label]
  (let [navigation-state (get-in data keypaths/navigation-event)]
    [:a.black.py1.mx3.lg-mt2
     (merge {:ref ref}
            (utils/route-to data event)) label]))

(def tab-refs ["bonuses" "commissions" "referrals"])
(def nav-events [events/navigate-stylist-dashboard-bonus-credit events/navigate-stylist-dashboard-commissions events/navigate-stylist-dashboard-referrals])
(def labels ["Bonuses" "Commissions" "Referrals"])

(defn save-tab-bounds [owner]
  (om/set-state! owner
                 {:bounds (vec (for [tab-ref tab-refs]
                                 (let [parent-rect (.getBoundingClientRect (om/get-node owner "tabs"))
                                       rect (.getBoundingClientRect (om/get-node owner tab-ref))]
                                   {:left (- (.-left rect) (.-left parent-rect))
                                    :width (.-width rect)})))}))

(defn stylist-dashboard-nav-component [data owner]
  (let [handle-resize-event (fn [e] (save-tab-bounds owner))]
    (reify
      om/IDidMount
      (did-mount [this]
        (js/window.addEventListener "resize" handle-resize-event)
        (save-tab-bounds owner))
      om/IWillUnmount
      (will-unmount [this]
        (js/window.removeEventListener "resize" handle-resize-event))
      om/IRenderState
      (render-state [this {:keys [bounds]}]
        (let [navigation-state (get-in data keypaths/navigation-event)
              tab-position     (utils/position #(= % navigation-state) nav-events)
              tab-underline    (get bounds tab-position)]
          (html
           [:nav.bg-silver.h5 {:ref "tabs"}
            [:div.bg-lighten-4
             [:div.bg-lighten-4
              [:div.flex.justify-center
               (for [[event ref label] (map vector nav-events tab-refs labels)]
                 (link-with-selected data event ref label))]
              [:div.border-teal.border.absolute
               {:style {:margin-top "-2px"
                        :transition "all 0.25s ease-in"
                        :left       (str (:left tab-underline) "px")
                        :width      (str (:width tab-underline) "px")}}]]]]))))))

(defn stylist-dashboard-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:.legacy-container
      (om/build stylist-dashboard-stats-component data)

      (om/build stylist-dashboard-nav-component data)
      (om/build
       (condp = (get-in data keypaths/navigation-event)
         events/navigate-stylist-dashboard-commissions  stylist-commissions-component
         events/navigate-stylist-dashboard-bonus-credit stylist-bonus-credit-component
         events/navigate-stylist-dashboard-referrals    stylist-referrals-component)
       data)]])))
