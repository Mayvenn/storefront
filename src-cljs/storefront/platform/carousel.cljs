(ns storefront.platform.carousel
  (:require [storefront.component :as component :refer [defdynamic-component defcomponent]]))

(defn safely-destroy [carousel]
  (when (some-> carousel .-destroy fn?)
    (.destroy carousel)))

(defn build-carousel [this settings]
  (when (component/get-ref this "container")
    (let [carousel (js/tns
                    (clj->js (merge {:container        (component/get-ref this "container")
                                     :edgePadding      30
                                     :items            3
                                     :loop             true
                                     :controlsPosition "bottom"
                                     :navPosition      "bottom"
                                     :touch            true
                                     :mouseDrag        true}
                                    (when (:controls settings true)
                                      {:prevButton (component/get-ref this "prev-button")
                                       :nextButton (component/get-ref this "next-button")})
                                    settings)))]
      (set! (.-carousel this) carousel))))

(defdynamic-component inner-component
  (constructor [c _]
               (component/create-ref! c "container")
               (component/create-ref! c "prev-button")
               (component/create-ref! c "next-button")
               nil)
  (did-mount [this]
             (let [{:keys [settings]} (component/get-opts this)]
               (build-carousel this settings)))
  (will-unmount [this]
                (safely-destroy (.-carousel this))
                (set! (.-carousel this) nil))
  (render [this]
          (component/html
           (let [{:keys [slides]}                       (component/get-props this)
                 {:keys [controls] :or {controls true}} (:settings (component/get-opts this))]
             [:div.relative
              (when controls
                [:div.z2.carousel-prev {:style {:height "50px" :width "50px"}
                                        :ref   (component/use-ref this "prev-button")}])
              (when controls
                [:div.z2.carousel-next {:style {:height "50px" :width "50px"}
                                        :ref   (component/use-ref this "next-button")}])
              [:div.slides {:ref (component/use-ref this "container")}
               (for [[idx slide] (map-indexed vector slides)]
                 ;; Wrapping div allows slider.js to attach
                 ;; click handlers without overwriting ours
                 [:div {:key idx} slide])]]))))

;; Important note: carousels should be provided a react key, otherwise we'll get strange behavior
;; from dirty state when going between pages containing different carousels.
(defcomponent component [{:keys [slides] :as data} _ _]
  (component/build inner-component
                   {:slides slides}
                   {:opts (cond-> (assoc-in data [:settings :autoplay] false)

                            (= (count slides) 1)
                            (update-in [:settings] merge {:arrows    false
                                                          :nav       false
                                                          :touch     false
                                                          :controls  false
                                                          :mouseDrag false}))}))
