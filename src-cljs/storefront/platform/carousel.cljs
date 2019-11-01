(ns storefront.platform.carousel
  (:require [sablono.core :refer [html]]
            [storefront.component :as component :refer [defdynamic-component defcomponent]]
            ;;react-slick
            ))

(defn safely-destroy [carousel]
  (when (some-> carousel .-destroy fn?)
    (.destroy carousel)))

(defn build-carousel [this settings]
  (when (component/get-ref this "container")
    (set! (.-carousel this)
          (js/tns
           (clj->js (merge {:container (component/get-ref this "container")
                            :edgePadding     30
                            :items            3
                            :loop             true
                            :controlsPosition "bottom"
                            :navPosition      "bottom"
                            :touch            true
                            :mouseDrag        true
                            :autoplay         false}
                           (when (:controls settings true)
                             {:prevButton (component/get-ref this "prev-button")
                              :nextButton (component/get-ref this "next-button")})
                           settings))))))

;; TODO: Make this work nicely when the slides change
(defdynamic-component inner-component
  (constructor [this _]
    (component/create-ref! this "container")
    (component/create-ref! this "prev-button")
    (component/create-ref! this "next-button")
    nil)
  (did-mount [this]
             (let [{:keys [settings]} (component/get-props this)]
               (build-carousel this settings)))
  (will-unmount [this]
                (safely-destroy (.-carousel this)))
  (render [this]
    (component/html
     (let [{:keys [slides settings]} (component/get-props this)
           {:keys [controls] :or {controls true}} settings]
       [:div.relative
        (when controls
          [:div.z2.slick-prev {:style {:height "50px" :width "50px"}
                               :ref   (component/use-ref this "prev-button")}])
        (when controls
          [:div.z2.slick-next {:style {:height "50px" :width "50px"}
                               :ref   (component/use-ref this "next-button")}])
        [:div.slides {:ref (component/use-ref this "container")}
         (for [[idx slide] (map-indexed vector slides)]
           ;; Wrapping div allows slider.js to attach
           ;; click handlers without overwriting ours
           [:div {:key (str idx)} slide])]]))))

(defcomponent component [{:keys [slides] :as data} owner _]
  (component/build inner-component
                   (cond-> (assoc-in data [:settings :autoplay] false)

                     (= (count slides) 1)
                     (update-in [:settings] merge {:arrows    false
                                                   :nav       false
                                                   :touch     false
                                                   :controls  false
                                                   :mouseDrag false}))))
