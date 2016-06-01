(ns storefront.components.carousel
  (:require [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.messages :as messages]
            [storefront.request-keys :as request-keys]
            [sablono.core :refer-macros [html]]
            [swipe :as swipe]
            [om.core :as om]
            [storefront.components.ui :as ui]))

(defn css-url [url] (str "url(" url ")"))

(defn carousel-component [data owner {:keys [index-path images]}]
  (om/component
   (html
    (let [idx (get-in data index-path)]
      [:.carousel-component
       [:.hair-category-image {:style {:background-image (css-url (get images idx))}}]
       [:.left {:on-click
                (utils/send-event-callback events/control-carousel-move
                                           {:index-path index-path
                                            :index (mod (dec idx) (count images))})}]
       [:.right {:on-click
                 (utils/send-event-callback events/control-carousel-move
                                            {:index-path index-path
                                             :index (mod (inc idx) (count images))})}]]))))

(defn set-selected-index [owner i]
  (om/set-state! owner :selected-index i)
  false)

(defn swipe-component [{:keys [items continuous]} owner {:keys [start-index dot-location]}]
  (reify
    om/IDidMount
    (did-mount [this]
      (om/set-state!
       owner
       {:swiper         (js/Swipe. (om/get-ref owner "items")
                                   #js {:continuous (or continuous false)
                                        :startSlide (or start-index 0)
                                        :callback   (partial set-selected-index owner)})
        :selected-index (or start-index 0)}))
    om/IWillUnmount
    (will-unmount [this]
      (when-let [swiper (:swiper (om/get-state owner))]
        (.kill swiper)))
    om/IRenderState
    (render-state [_ {:keys [swiper selected-index]}]
      (when (and swiper selected-index)
        (let [delta (- (.getPos swiper) selected-index)]
          (if (= (Math/abs delta) (dec (count items)))
            (.slide swiper selected-index)
            (if (pos? delta)
              (dotimes [_ delta] (.prev swiper))
              (dotimes [_ (- delta)] (.next swiper))))))
      (html
       [:.center.relative
        [:.overflow-hidden.relative.invisible
         {:ref "items"}
         [:.overflow-hidden.relative
          (for [item items]
            [:.left.col-12.relative {:key (:id item)}
             (:body item)])]]
        [:a.block.absolute.img-left-arrow.bg-no-repeat.bg-center.cursor.active-darken-3.to-md-hide.top-0.bottom-0.left-0
         {:style    {:width           "5rem"
                     :background-size "30px"}
          :on-click (fn [_]
                      (set-selected-index owner (if (= 0 selected-index)
                                                  (dec (count items))
                                                  (dec selected-index))))}]
        [:a.block.absolute.img-right-arrow.bg-no-repeat.bg-center.cursor.active-darken-3.to-md-hide.top-0.bottom-0.right-0
         {:style    {:width           "5rem"
                     :background-size "30px"}
          :on-click (fn [_]
                      (set-selected-index owner (if (= selected-index (dec (count items)))
                                                  0
                                                  (inc selected-index))))}]
        [:.flex.block.absolute
         {:style {:bottom "1rem"
                  :left   "1.5rem"
                  :right  "1.5rem"}
          :class (when-not (= :left dot-location) "justify-center")}
         (for [i (range (count items))]
           [:.pointer.pxp2
            {:key i
             :on-click (fn [_] (set-selected-index owner i))}
            [:.bg-white.border.border-dark-gray.circle.bg-lighten-3
             {:class (when (= selected-index i) "bg-dark-gray")
              :style {:width "7px" :height "7px"}}]])]]))))
