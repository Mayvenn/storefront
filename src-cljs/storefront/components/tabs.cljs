(ns storefront.components.tabs
  (:require [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as component :refer [defcomponent]]
            ))


(defn tab-link [event id ref label]
  [:a.dark-gray.center.pt2
   (merge (utils/route-to event) {:key id})
   [:.py1 {:ref ref
           :data-test (str "nav-" id)}
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

(letfn [(tab-bounds [c]
          (let [{parent-left :left} (get-x-dimension (component/get-ref c "tabs"))]
            (vec (for [tab-ref (:tab-refs (component/get-opts c))]
                   (let [{:keys [left width]} (get-x-dimension (component/get-ref c tab-ref))]
                     {:left  (- left parent-left)
                      :width width})))))
        (cache-tab-bounds [c] (component/set-state! c :tab-bounds (tab-bounds c)))
        (handle-resize-event [c e] (cache-tab-bounds c))]
  (defdynamic-component component
    (constructor [this props]
                 (component/create-ref! this "tabs")
                 (let [{:keys [tab-refs]} (component/get-opts this)]
                   (doseq [tab-ref tab-refs]
                     (component/create-ref! this tab-ref)))
                 (set! (.-tab-bounds this) (partial tab-bounds this))
                 (set! (.-cache-tab-bounds this) (partial cache-tab-bounds this))
                 nil)
    (did-mount [this]
               (js/window.addEventListener "resize" (partial handle-resize-event this))
               (.cache-tab-bounds this))
    (will-unmount [this]
                  (js/window.removeEventListener "resize" (partial handle-resize-event this)))
    (render [this]
            (let [{:keys [selected-tab]}         (component/get-props this)
                  {:keys [tab-bounds]}           (component/get-state this)
                  {:keys [tabs tab-refs labels]} (component/get-opts this)]
              (component/html
               [:nav.sticky.z1.top-0
                [:div.flex.justify-around {:ref (component/use-ref this "tabs")}
                 (for [[event ref label] (map vector tabs tab-refs labels)]
                   (tab-link event ref (component/use-ref this ref) label))]
                (component/build sliding-indicator
                                 {:selected-tab selected-tab
                                  :tab-bounds   tab-bounds}
                                 {:opts {:tabs tabs}})])))))
