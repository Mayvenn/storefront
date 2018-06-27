(ns voucher.components.qr-reader
  (:require [sablono.core :refer [html]]
            jsQR
            [om.core :as om]))

(defn ^:private video-offset [v1 v2]
  (if (> v1 v2)
    (/ (- v1 v2) 2)
    0))

(defn draw [video canvas]
  (let [vw (.-videoWidth video)
        vh (.-videoHeight video)
        cw (.-width canvas)
        ch (.-height canvas)]
    (.drawImage (.getContext canvas "2d")
                video
                (video-offset vw vh)
                (video-offset vh vw)
                (min vw vh) (min vw vh)
                0 0
                cw ch)))

(defn get-image-data [canvas]
  (let [image-data (.getImageData (.getContext canvas "2d") 0 0 (.-width canvas) (.-height canvas))]
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

(defn draw-brackets [canvas]
  (draw-line canvas
             {:x (- (.-width canvas) 80)
              :y (- (.-height canvas) 40)}
             {:x (- (.-width canvas) 40)
              :y (- (.-height canvas) 40)}
             {:x (- (.-width canvas) 40)
              :y (- (.-height canvas) 80)})
  (draw-line canvas
             {:x 40
              :y (- (.-height canvas) 80)}
             {:x 40
              :y (- (.-height canvas) 40)}
             {:x 80
              :y (- (.-height canvas) 40)})
  (draw-line canvas
             {:x (- (.-width canvas) 80)
              :y 40}
             {:x (- (.-width canvas) 40)
              :y 40}
             {:x (- (.-width canvas) 40)
              :y 80})
  (draw-line canvas
             {:x 80 :y 40}
             {:x 40 :y 40}
             {:x 40 :y 80}))

(defn resize-canvas [video canvas]
  (let [canvas-parent-rectangle (.getBoundingClientRect (.-parentElement canvas))]
    (set! (.-width canvas)
          (.-width canvas-parent-rectangle))
    (set! (.-height canvas)
          (* (.-width canvas-parent-rectangle)
             (/ (.-videoHeight video)
                (.-videoWidth video))))))

(defn tick [video canvas control timestamp]
  (when-not (get @control :stop)
    (if (= (.-readyState video) (.-HAVE_ENOUGH_DATA video))
      (do
        (resize-canvas video canvas)
        (draw video canvas)
        (draw-brackets canvas)
        (let [{:keys [data width height]} (get-image-data canvas)]
          (try
            (if-let [voucher-code (read-qr-response (js/jsQR data width height))]
              (image-recognized voucher-code)
              (js/requestAnimationFrame (partial tick video canvas control)))
            (catch :default e
              (js/requestAnimationFrame (partial tick video canvas control))))))
      (js/requestAnimationFrame (partial tick video canvas control)))))

(defn start-render-loop [video canvas control stream]
  (set! (.-srcObject video) stream)
  (doto video
    (.setAttribute "playsinline" true)
    (.play))
  (js/requestAnimationFrame (partial tick video canvas control)))

(defn component [{:keys [] :as data} owner _]
  (reify
    om/IInitState
    (init-state [_]
      {:control (atom {})})
    om.core/IDidMount
    (did-mount [this]
      (let [video   (js/document.createElement "video")
            canvas  (om/get-ref owner "qr-canvas")
            control (:control (om/get-state owner))]
        (.then (js/navigator.mediaDevices.getUserMedia (clj->js {:video {:facingMode "environment"}}))
               (partial start-render-loop video canvas control))))
    om/IWillUnmount
    (will-unmount [_]
      (swap! (:control (om/get-state owner)) assoc :stop true))
    om/IRender
    (render [_]
      (html
       [:canvas {:ref "qr-canvas"}]))))
