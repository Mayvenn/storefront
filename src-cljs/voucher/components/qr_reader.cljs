(ns voucher.components.qr-reader
  (:require [sablono.core :refer [html]]
            jsQR
            [om.core :as om]))

(defn inner-component [{:keys []} owner _]
  (om/component
   [:div]))

(defn get-video-element []
  )

(defn draw [video canvas-ref]
  (.drawImage (.getContext canvas-ref "2d") video 0 0))

(defn get-image-data [canvas-ref]
  (let [image-data (.getImageData (.getContext canvas-ref "2d") 0 0 (.-width canvas-ref) (.-height canvas-ref))]
    {:data   (.-data image-data)
     :height (.-height image-data)
     :width  (.-width image-data)}))

(defn image-recognized [voucher-code]
  (prn voucher-code))

(defn read-qr-response [img-data]
  (let [clj-img-data (js->clj img-data :keywordize-keys true)]
    (when (->> (:chunks clj-img-data)
               (map :type)
               (some (partial = "byte")))
      (:data clj-img-data))))

(defn draw-line [canvas begin & rest]
  (let [ctx  (.getContext canvas "2d")]
    (.beginPath ctx)
    (.moveTo ctx (:x begin) (:y begin))
    (doseq [{:keys [x y]} rest]
      (.lineTo ctx x y))
    (set! (.-lineWidth ctx) 10)
    (set! (.-strokeStyle ctx) "white")
    (.stroke ctx)))

(defn tick [video canvas-ref timestamp]
  (if (= (.-readyState video) (.-HAVE_ENOUGH_DATA video))
    (do
      (draw video canvas-ref)
      (draw-line canvas-ref
                 {:x (- (.-width canvas-ref) 120)
                  :y (- (.-height canvas-ref) 40)}
                 {:x (- (.-width canvas-ref) 40)
                  :y (- (.-height canvas-ref) 40)}
                 {:x (- (.-width canvas-ref) 40)
                  :y (- (.-height canvas-ref) 120)})
      (draw-line canvas-ref
                 {:x 120
                  :y 40}
                 {:x 40
                  :y 40}
                 {:x 40
                  :y 120})
      (let [{:keys [data width height]} (get-image-data canvas-ref)]
        (if-let [voucher-code (read-qr-response (js/jsQR data width height))]
          (image-recognized voucher-code)
          (js/requestAnimationFrame (partial tick video canvas-ref)))))
    (js/requestAnimationFrame (partial tick video canvas-ref))))

(defn start-render-loop [video canvas-ref stream]
  (set! (.-srcObject video) stream)
  (doto video
    (.setAttribute "playsinline" true)
    (.play))
  (js/requestAnimationFrame (partial tick video canvas-ref)))

(defn component [{:keys [slides] :as data} owner _]
  (let [video (js/document.createElement "video")]
    (reify
      om/IInitState
      (init-state [_]
        {})
      om.core/IDidMount
      (did-mount [_]
        (let [canvas (om/get-ref owner "qr-canvas")]
          ;; Start render loop (I think)
          (.then (js/navigator.mediaDevices.getUserMedia (clj->js {:video {:facingMode "environment"
                                        ;:aspectRatio (/ 21 4)
                                                                           :resizeMode "crop-and-scale"
                                                                           }}))
                 (fn [stream]
                   (let [video-track (-> stream .getVideoTracks first)
                         {:keys [width height]} (-> video-track .getSettings (js->clj :keywordize-keys true))]
                     (.applyConstraints video-track (clj->js {:height (.-height canvas)
                                                              :aspectRatio (/ width height)})))
                   (start-render-loop video canvas stream)))))

      om/IRenderState
      (render-state [_ {:keys [autoplay]}]
        (html
         ;; TODO Create video element
         ;; TODO Create canvas element
         ;;          (with canvas context)
         [:canvas {:width 400
                   :height 400
                   :ref "qr-canvas"}])))))
