(ns storefront.components.tabs
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]))


(defn tab-link [event ref label]
  [:a.dark-gray.center.pt2
   (merge (utils/route-to event) {:key ref})
   [:.py1 {:ref ref
           :data-test (str "nav-" ref)}
    label]])

(defn sliding-indicator [{:keys [selected-tab tab-bounds]} owner {:keys [tabs]}]
  (om/component
   (html
    (let [tab-position         (utils/position #{selected-tab} tabs)
          {:keys [left width]} (get tab-bounds tab-position)]
      [:div.border-navy.border.relative.transition-ease-in.transition-1
       {:style {:margin-top "-2px"
                :left       (str left "px")
                :width      (str width "px")}}]))))

(defn get-x-dimension [node]
  (if node
    (let [rect (.getBoundingClientRect node)]
      {:left  (.-left rect)
       :width (.-width rect)})
    {:left 0 :width 0}))

(defn component [{:keys [selected-tab]} owner {:keys [tab-refs tabs labels]}]
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
         [:nav.sticky.z1.top-0
          [:div.flex.justify-around {:ref "tabs"}
           (for [[event ref label] (map vector tabs tab-refs labels)]
             (tab-link event ref label))]
          (om/build sliding-indicator
                    {:selected-tab selected-tab
                     :tab-bounds   tab-bounds}
                    {:opts {:tabs tabs}})])))))
