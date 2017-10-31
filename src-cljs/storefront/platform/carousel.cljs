(ns storefront.platform.carousel
  (:require [sablono.core :refer [html]]
            react-slick
            [om.core :as om]))

(def custom-dots (constantly (html [:a [:div.circle]])))

(defn inner-component [{:keys [slides settings]} owner _]
  (om/component
   (if-not (seq slides)
     (html [:div])
     (js/React.createElement js/Slider
                             (clj->js (merge {:pauseOnHover true
                                              :customPaging custom-dots
                                              :dotsClass "carousel-dots"
                                              ;; :waitForAnimate true
                                              ;; TODO: figure out why onMouseUp always
                                              ;; triggers navigation to link in slide,
                                              ;; while onTouchEnd doesn't. This prevents
                                              ;; us from allowing drag on desktop.
                                              :draggable    false}
                                             settings))
                             (html (for [[idx slide] (map-indexed vector slides)]
                                     ;; Wrapping div allows slider.js to attach
                                     ;; click handlers without overwriting ours
                                     [:div {:key idx} slide]))))))

(defn cancel-autoplay [owner]
  (om/set-state! owner {:autoplay false}))

(defn override-autoplay [original autoplay-override]
  (update original :autoplay #(and % autoplay-override)))

(defn component [{:keys [slides] :as data} owner _]
  (reify
    om/IInitState
    (init-state [_]
      {:autoplay true})

    om/IRenderState
    (render-state [_ {:keys [autoplay]}]
      (html
       [:div.stacking-context
        ;; Cancel autoplay on interaction
        {:on-mouse-down  #(cancel-autoplay owner)
         :on-touch-start #(cancel-autoplay owner)}
        (om/build inner-component
                  (cond-> data
                    (= (count slides) 1)
                    (update-in [:settings] merge {:arrows false
                                                  :dots   false
                                                  :swipe  false})
                    :always
                    (update-in [:settings] override-autoplay autoplay)
                    :always
                    (update-in [:settings :responsive]
                               (fn [responsive]
                                 (map
                                  (fn [breakpoint]
                                    (update breakpoint :settings override-autoplay autoplay))
                                  responsive)))))]))))
