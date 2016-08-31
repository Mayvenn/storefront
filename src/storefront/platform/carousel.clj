(ns storefront.platform.carousel)

(defn swipe-component [{:keys [items continuous]} owner {:keys [starting-item dot-location]}]
  [:div.center.relative
   [:div.overflow-hidden.relative
    {:ref "items"}
    [:div.overflow-hidden.relative
     (for [i (range (count items))
           :let [item (nth items i)]]
       [:div.left.col-12.relative
        (merge {:key (:id item)}
               (when (> i 0)
                 {:class "display-none"}))
        (:body item)])]]])
