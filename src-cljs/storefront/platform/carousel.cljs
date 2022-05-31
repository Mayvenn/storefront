(ns storefront.platform.carousel
  (:require [storefront.component :as component :refer [defdynamic-component defcomponent]]))

(defn safely-destroy [carousel]
  (when (some-> carousel .-destroy fn?)
    (.destroy carousel)))

(defn build-carousel [this events settings]
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
      (doseq [[event-name f] events]
        (.on (.-events carousel) event-name f))
      (set! (.-carousel this) carousel))))

(defdynamic-component inner-component
  (constructor [c _]
               (component/create-ref! c "container")
               (component/create-ref! c "prev-button")
               (component/create-ref! c "next-button")
               nil)
  (did-mount [this]
             (let [{:keys [events settings]} (component/get-opts this)]
               (build-carousel this events settings)))
  (will-unmount [this]
                (safely-destroy (.-carousel this)))
  (render [this]
          (component/html
           (let [{:keys [settings slides]}              (component/get-opts this)
                 {:keys [controls controls-classes] :or {controls true}} settings]
             [:div.relative.stacking-context
              (when controls
                [:div.z2.carousel-prev {:style {:height "50px" :width "50px"}
                                        :class controls-classes
                                        :ref   (component/use-ref this "prev-button")}])
              (when controls
                [:div.z2.carousel-next {:style {:height "50px" :width "50px"}
                                        :class controls-classes
                                        :ref   (component/use-ref this "next-button")}])
              [:div.slides {:ref (component/use-ref this "container")}
               (for [[idx slide] (map-indexed vector slides)]
                 ;; Wrapping div allows slider.js to attach
                 ;; click handlers without overwriting ours
                 [:div {:key idx} slide])]]))))

;; Important note: carousels should be provided a react key, otherwise we'll get strange behavior
;; from dirty state when going between pages containing different carousels.
(defcomponent component [{:as data :keys [slides]} _ {:as opts-data :keys [events]}]
  (component/build inner-component
                   {} ;; we're using key to invalidate the component
                   {:opts (-> opts-data
                              (assoc-in [:settings :autoplay] false)
                              (assoc :slides slides :events events)
                              (cond->

                                  (= (count slides) 1)
                                (update-in [:settings] merge {:arrows    false
                                                              :nav       false
                                                              :touch     false
                                                              :controls  false
                                                              :mouseDrag false})))
                    :key  (->> data
                               hash
                               (str "product-carousel-inner-"))}))
