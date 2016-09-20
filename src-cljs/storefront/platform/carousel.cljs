(ns storefront.platform.carousel
  (:require [sablono.core :refer-macros [html]]
            [swipe :as swipe]
            [om.core :as om]))

(defn set-selected-item [owner i]
  (om/set-state! owner :selected-item i)
  false)

(defn index-of [coll item]
  (->> coll
       (map :id)
       (map-indexed vector)
       (filter (comp #{(:id item)} second))
       ffirst))

(defn arrow [{:keys [class on-click]}]
  [:a.block.absolute.bg-no-repeat.bg-center.cursor.to-md-hide.top-0.bottom-0.pointer
   {:class class
    :style    {:width           "5rem"
               :background-size "30px"}
    :on-click on-click}])

(defn dot [{:keys [key on-click selected?]}]
  [:div.pointer.pxp2
   {:key      key
    :on-click on-click}
   [:div.bg-light-silver.border.border-gray.circle
    {:class (if selected? "bg-gray" "bg-lighten-3")
     :style {:width "7px" :height "7px"}}]])

(defn swipe-component [{:keys [items continuous]} owner {:keys [starting-item dot-location]}]
  (reify
    om/IDidMount
    (did-mount [this]
      (let [index (or (index-of items starting-item) 0)]
        (om/set-state!
         owner
         {:items          items
          :swiper         (js/Swipe. (om/get-ref owner "items")
                                     #js {:continuous (or continuous false)
                                          :startSlide index
                                          :callback   (fn [i]
                                                        (set-selected-item owner (get-in (om/get-state owner) [:items i])))})
          :selected-item starting-item})))
    om/IWillUnmount
    (will-unmount [this]
      (when-let [swiper (:swiper (om/get-state owner))]
        (.kill swiper)))
    om/IRenderState
    (render-state [_ {:keys [swiper selected-item]}]
      (let [selected-index (or (index-of items selected-item) 0)]
        (when (and swiper selected-item)
          (let [delta (- (.getPos swiper) selected-index)]
            (if (= (Math/abs delta) (dec (count items)))
              (.slide swiper selected-index)
              (if (pos? delta)
                (dotimes [_ delta] (.prev swiper))
                (dotimes [_ (- delta)] (.next swiper))))))
        (html
         [:div.center.relative
          [:div.overflow-hidden.relative.invisible
           {:ref "items"}
           [:div.overflow-hidden.relative
            (for [item items]
              [:div.left.col-12.relative {:key (:id item)}
               (:body item)])]]
          (when (> (count items) 1)
            [:div
             (arrow {:class "img-left-arrow left-0"
                     :on-click (fn [_]
                                 (set-selected-item owner (get items (if (= 0 selected-index)
                                                                       (dec (count items))
                                                                       (dec selected-index)))))})
             (arrow {:class "img-right-arrow right-0"
                     :on-click (fn [_]
                                 (set-selected-item owner (get items (if (= selected-index (dec (count items)))
                                                                       0
                                                                       (inc selected-index)))))})
             [:div.flex.block.absolute
              {:style {:bottom "1rem"
                       :left   "1.5rem"
                       :right  "1.5rem"}
               :class (when-not (= :left dot-location) "justify-center")}
              (for [[i item] (map-indexed vector items)]
                (dot {:key i
                      :on-click (fn [_] (set-selected-item owner item))
                      :selected? (= selected-index i)}))]])])))))
