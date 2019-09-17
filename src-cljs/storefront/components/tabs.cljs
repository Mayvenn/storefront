(ns storefront.components.tabs
  (:require [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.platform.component-utils :as utils]))


(defn tab-link [event ref label]
  [:a.dark-gray.center.pt2
   (merge (utils/route-to event) {:key ref})
   [:.py1 {:ref ref
           :data-test (str "nav-" ref)}
    label]])

(defcomponent sliding-indicator [{:keys [selected-tab tab-bounds]} owner {:keys [tabs]}]
  (let [tab-position         (utils/position #{selected-tab} tabs)
        {:keys [left width]} (get tab-bounds tab-position)]
    [:div.border-navy.border.relative.transition-ease-in.transition-1
     {:style {:margin-top "-2px"
              :left       (str left "px")
              :width      (str width "px")}}]))

(defn get-x-dimension [node]
  (if node
    (let [rect (.getBoundingClientRect node)]
      {:left  (.-left rect)
       :width (.-width rect)})
    {:left 0 :width 0}))

(defn component [{:keys [selected-tab]} owner {:keys [tab-refs tabs labels]}]
  (letfn [(tab-bounds []
            (let [{parent-left :left} (get-x-dimension (component/get-ref owner "tabs"))]
              (vec (for [tab-ref tab-refs]
                     (let [{:keys [left width]} (get-x-dimension (component/get-ref owner tab-ref))]
                       {:left  (- left parent-left)
                        :width width})))))
          (cache-tab-bounds [] (component/set-state! owner {:tab-bounds (tab-bounds)}))
          (handle-resize-event [e] (cache-tab-bounds))]
    (component/create-dynamic
     "tabs-component"
     (constructor [this props]
                  (set! (.-tab-bounds this) (fn []
                                              (let [{parent-left :left} (get-x-dimension (component/get-ref this "tabs"))]
                                                (vec (for [tab-ref tab-refs]
                                                       (let [{:keys [left width]} (get-x-dimension (component/get-ref this tab-ref))]
                                                         {:left  (- left parent-left)
                                                          :width width}))))))
                  (set! (.-cache-tab-bounds this) (fn cache-tab-bounds
                                                    ([] (component/set-state! this {:tab-bounds (tab-bounds)}))
                                                    ([e] (cache-tab-bounds))))
                  nil)
     (did-mount [this]
                (js/window.addEventListener "resize" handle-resize-event)
                (.cache-tab-bounds this))
     (will-unmount [this]
                   (js/window.removeEventListener "resize" handle-resize-event))
     (render [this]
             (let [{:keys [tab-bounds]} (component/get-state this)]
               (component/html
                [:nav.sticky.z1.top-0
                 [:div.flex.justify-around {:ref "tabs"}
                  (for [[event ref label] (map vector tabs tab-refs labels)]
                    (tab-link event ref label))]
                 (component/build sliding-indicator
                                  {:selected-tab selected-tab
                                   :tab-bounds   tab-bounds}
                                  {:opts {:tabs tabs}})]))))))
