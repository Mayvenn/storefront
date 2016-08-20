(ns storefront.platform.carousel-two
  (:require [sablono.core :refer-macros [html]]
            react-slick
            [om.core :as om]))

(defn inner-component [{:keys [items autoplay?]} owner _]
  (om/component
   (js/React.createElement js/Slider
                           (clj->js {:autoplay       autoplay?
                                     :pauseOnHover   true
                                     :slidesToShow   (count items)
                                     :arrows         true
                                     ;; TODO: figure out why onMouseUp always
                                     ;; triggers navigation to link in slide,
                                     ;; while onTouchEnd doesn't
                                     :draggable      false
                                     ;; :waitForAnimate true
                                     :responsive     [{:breakpoint 640
                                                       :settings   {:slidesToShow 2}}
                                                      {:breakpoint 768
                                                       :settings   {:slidesToShow 3}}
                                                      {:breakpoint 1024
                                                       :settings   {:slidesToShow 5}}
                                                      {:breakpoint 100000
                                                       :settings   {:autoplay     false
                                                                    :arrows       false
                                                                    :swipe        false}}]})
                           (html items))))

(defn cancel-autoplay [owner]
  (om/update-state! owner #(assoc % :autoplay? false)))

(defn component [data owner _]
  (reify
    om/IRenderState
    (render-state [_ inner-data]
      (html
       [:div {:on-mouse-down  #(cancel-autoplay owner)
              :on-touch-start #(cancel-autoplay owner)}
        (om/build inner-component {:items     (:items data)
                                   :autoplay? (get inner-data :autoplay? (get data :autoplay?))})]))))
