(ns storefront.platform.carousel
  (:require [sablono.core :refer [html]]
            [storefront.component :as component :refer [defdynamic-component]]
            ;;react-slick
            ))

(defn safely-destroy [carousel]
  (when (some-> carousel .-destroy fn?)
    (.destroy carousel)))

(defn build-carousel [settings]
  (this-as this
           (set! (.-carousel this)
                 (js/tns
                  (clj->js (merge {:container        (component/get-ref this "container")
                                   :prevButton       (component/get-ref this "prev-button")
                                   :nextButton       (component/get-ref this "next-button")
                                   :edgePadding      30
                                   :items            3
                                   :loop             true
                                   :controlsPosition "bottom"
                                   :navPosition      "bottom"
                                   :touch            true
                                   :mouseDrag        true
                                   :autoplay         false}
                                  settings))))))

;; TODO: Make this work nicely when the slides change
(defdynamic-component inner-component
  [_ _ _]
  (constructor [this _]
    (component/create-ref! this "container")
    (component/create-ref! this "prev-button")
    (component/create-ref! this "next-button")
    (set! (.-build-carousel this) (.bind build-carousel this)))
  (did-mount [this]
             (prn "inner mount")
             (let [{:keys [settings]} (component/get-props this)]
               (.build-carousel this settings)))
  (will-unmount [this]
                (prn "inner unmount")
                (safely-destroy (.-carousel this)))

  (did-update [this prev-props]
              (prn "inner did update")
              #_(let [{:keys [slides settings]} (component/get-props this)
                    {prev-settings :settings
                     prev-slides   :slides}   (.-props prev-props)]
                (when (or (not= prev-slides slides)
                          (not= prev-settings settings))
                  (safely-destroy (.-carousel this))
                  (set! (.-carousel this) nil)
                  (.build-carousel this settings))))

  (render [this]
          (prn "inner render")
    (component/html
     (let [{:keys [slides]} (component/get-props this)]
       (if-not (seq slides)
         [:div]
         [:div
          [:div.z2.slick-prev {:style {:height "50px" :width "50px"} :ref (component/use-ref this "prev-button")}]
          [:div.z2.slick-next {:style {:height "50px" :width "50px"} :ref (component/use-ref this "next-button")}]
          [:div {:ref (component/use-ref this "container")}
           (for [[idx slide] (map-indexed vector slides)]
             ;; Wrapping div allows slider.js to attach
             ;; click handlers without overwriting ours
             [:div {:key idx} slide])]])))))

(defdynamic-component component [_ owner _]
  (did-mount [_]
             (prn "outer mount"))
  (will-unmount [_]
             (prn "outer unmount"))
  (did-update [this prev-props]
              (prn "outer did update")
              #_(let [{:keys [slides settings]} (component/get-props this)
                    {prev-settings :settings
                     prev-slides   :slides}   (.-props prev-props)]
                (when (or (not= prev-slides slides)
                          (not= prev-settings settings))
                  (prn "force update")
                  (.forceUpdate this))))
  (render [this]
          (prn "outer render")
          (let [{:keys [slides] :as data} (component/get-props this)]
            (component/build inner-component
                             (cond-> (assoc-in data [:settings :autoplay] false)

                               (= (count slides) 1)
                               (update-in [:settings] merge {:arrows    false
                                                             :nav       false
                                                             :touch     false
                                                             :controls  false
                                                             :mouseDrag false}))))))
