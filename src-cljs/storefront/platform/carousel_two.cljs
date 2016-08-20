(ns storefront.platform.carousel-two
  (:require [sablono.core :refer-macros [html]]
            react-slick
            [om.core :as om]))

(defn inner-component [{:keys [items config]} owner _]
  (om/component
   (js/React.createElement js/Slider
                           (clj->js (merge {:pauseOnHover true
                                            ;; :waitForAnimate true
                                            ;; TODO: figure out why onMouseUp always
                                            ;; triggers navigation to link in slide,
                                            ;; while onTouchEnd doesn't. This prevents
                                            ;; us from allowing drag on desktop.
                                            :draggable    false}
                                           config))
                           (html items))))

(defn cancel-autoplay [owner]
  (om/set-state! owner {:autoplay false}))

(defn override-autoplay [original autoplay-override]
  (update original :autoplay #(and % autoplay-override)))

(defn component [data owner _]
  (reify
    om/IInitState
    (init-state [_]
      {:autoplay true})

    om/IRenderState
    (render-state [_ {:keys [autoplay]}]
      (html
       ;; Cancel autoplay on interaction
       [:div {:on-mouse-down  #(cancel-autoplay owner)
              :on-touch-start #(cancel-autoplay owner)}
        (om/build inner-component (-> data
                                      (update-in [:config] override-autoplay autoplay)
                                      (update-in [:config :responsive]
                                                 (fn [responsive]
                                                   (map
                                                    (fn [breakpoint]
                                                      (update breakpoint :settings override-autoplay autoplay))
                                                    responsive)))))]))))
