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

(def tab-refs ["bonuses" "commissions" "referrals"])
(def nav-events [events/navigate-stylist-dashboard-bonus-credit events/navigate-stylist-dashboard-commissions events/navigate-stylist-dashboard-referrals])
(def labels ["Bonuses" "Commissions" "Referrals"])

(defn tab-link [data event ref label]
  [:a.black.py1.mx3.mt2
   (merge {:ref ref
           :data-test (str "nav-" ref)}
          (utils/route-to data event)) label])

(defn sliding-indicator [data tab-bounds]
  (let [navigation-state     (get-in data keypaths/navigation-event)
        tab-position         (utils/position #(= % navigation-state) nav-events)
        {:keys [left width]} (get tab-bounds tab-position)]
    [:div.border-teal.border.absolute.transition-ease-in.transition-1
     {:style {:margin-top "-2px"
              :left       (str left "px")
              :width      (str width "px")}}]))

(defn get-node
  "A backport of om/get-node, which returns nil instead of raising error when
  owner doesn't have refs yet"
  ([owner]
   (.getDOMNode owner))
  ([owner name]
   {:pre [(string? name)]}
   (some-> (.-refs owner) (aget name) (.getDOMNode))))

(defn get-x-dimension [node]
  (if node
    (let [rect (.getBoundingClientRect node)]
      {:left  (.-left rect)
       :width (.-width rect)})
    {:left 0 :width 0}))

(defn nav-component [data owner]
  (letfn [(tab-bounds []
            (let [{parent-left :left} (get-x-dimension (get-node owner "tabs"))]
              (vec (for [tab-ref tab-refs]
                     (let [{:keys [left width]} (get-x-dimension (get-node owner tab-ref))]
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
             (tab-link data event ref label))]
          (sliding-indicator data tab-bounds)])))))

(defn stylist-dashboard-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:.legacy-container.sans-serif.black
      (om/build stylist-dashboard-stats-component data)

      (om/build nav-component data)
      (om/build
       (condp = (get-in data keypaths/navigation-event)
         events/navigate-stylist-dashboard-commissions  stylist-commissions-component
         events/navigate-stylist-dashboard-bonus-credit stylist-bonus-credit-component
         events/navigate-stylist-dashboard-referrals    stylist-referrals-component)
       data)]])))
