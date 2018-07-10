(ns voucher.components.qr-reader
  (:require [sablono.core :refer [html]]
            jsQR
            [om.core :as om]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]))

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
                0 0
                vw vh
                0 0
                cw ch)))

(defn get-image-data [canvas]
  (let [image-data (.getImageData (.getContext canvas "2d") 0 0 (.-width canvas) (.-height canvas))]
    {:data   (.-data image-data)
     :height (.-height image-data)
     :width  (.-width image-data)}))

(defn image-recognized [code]
  (messages/handle-message events/control-voucher-qr-redeem {:code code}))

(defn read-qr-response [img-data]
  (let [clj-img-data (js->clj img-data :keywordize-keys true)]
    (when (->> (:chunks clj-img-data)
               (map :type)
               (some (partial #{"byte" "alphanumeric"})))
      (:data clj-img-data))))

(defn draw-line [canvas begin & rest]
  (let [ctx  (.getContext canvas "2d")]
    (.beginPath ctx)
    (.moveTo ctx (:x begin) (:y begin))
    (doseq [{:keys [x y]} rest]
      (.lineTo ctx x y))
    (set! (.-lineWidth ctx) (* 10 js/devicePixelRatio))
    (set! (.-strokeStyle ctx) "white")
    (.stroke ctx)))

(defn draw-brackets [canvas]
  (let [cw      (.-width canvas)
        ch      (.-height canvas)
        padding (/ cw 8)
        length  (/ cw 10)]
    (draw-line canvas
               {:x (- cw padding)
                :y (- ch (+ padding length))}
               {:x (- cw padding)
                :y (- ch padding)}
               {:x (- cw (+ padding length))
                :y (- ch padding)})
    (draw-line canvas
               {:x padding
                :y (- ch (+ padding length))}
               {:x padding
                :y (- ch padding)}
               {:x (+ padding length)
                :y (- ch padding)})
    (draw-line canvas
               {:x (- cw (+ padding length))
                :y padding}
               {:x (- cw padding)
                :y padding}
               {:x (- cw padding)
                :y (+ padding length)})
    (draw-line canvas
               {:x (+ padding length)
                :y padding}
               {:x padding
                :y padding}
               {:x padding
                :y (+ padding length)})))

(defn resize-canvas [video canvas]
  (let [canvas-parent-rectangle (.getBoundingClientRect (.-parentElement canvas))
        canvas-css-width        (.-width canvas-parent-rectangle)
        canvas-css-height       (* canvas-css-width
                                   (/ (.-videoHeight video)
                                      (.-videoWidth video)))
        scale                   (.-devicePixelRatio js/window)
        ctx                     (.getContext canvas "2d")
        canvas-style            (.-style canvas)]

    (set! (.-width canvas-style)
          (str canvas-css-width "px"))
    (set! (.-height canvas-style)
          (str canvas-css-height "px"))

    (set! (.-width canvas)
          (* canvas-css-width
             scale))
    (set! (.-height canvas)
          (* canvas-css-height scale))))

(defn draw-text [canvas]
  (let [ctx       (.getContext canvas "2d")
        scale     (.-devicePixelRatio js/window)
        font-size (* 16 scale)]
    (set! (.-font ctx) (str "bold " font-size "px Roboto"))
    (set! (.-fillStyle ctx) "white")
    (set! (.-textAlign ctx) "center")
    (.fillText ctx "Point camera at QR code"
               (/ (.-width canvas) 2) (+ font-size
                                         (/ (.-height canvas)
                                            40)))))

(defn tick [video canvas control timestamp]
  (when-not (get @control :stop)
    (when (>= (.-readyState video) (.-HAVE_FUTURE_DATA video))
      (do
        (resize-canvas video canvas)
        (draw video canvas)
        (draw-brackets canvas)
        (draw-text canvas)
        (try
          (let [{:keys [data width height]} (get-image-data canvas)]
            (when-let [voucher-code (read-qr-response (js/jsQR data width height))]
              (image-recognized voucher-code)))
          (catch :default e
            (js/console.log "Error while reading video stream: " e)
            nil))))
    (js/requestAnimationFrame (partial tick video canvas control))))

(defn start-render-loop [^js/HTMLMediaElement video canvas control stream]
  (set! (.-srcObject video) stream)
  (doto video
    (.setAttribute "playsinline" true)
    (.play)
    (.addEventListener "canplay"
                       (fn [e]
                         (js/requestAnimationFrame (partial tick video canvas control)))
                       false)))

(defn camera-permission-denied []
  (messages/handle-message events/voucher-camera-permission-denied))

(defn component [{:keys [] :as data} owner _]
  (reify
    om/IInitState
    (init-state [_]
      {:control (atom {})
       :stream  (atom nil)})
    om.core/IDidMount
    (did-mount [this]
      (let [video        (js/document.createElement "video")
            canvas       (om/get-ref owner "qr-canvas")
            control      (:control (om/get-state owner))
            state-stream (:stream (om/get-state owner))]
        (.then (js/navigator.mediaDevices.getUserMedia (clj->js {:video {:facingMode "environment"}}))
               (fn [stream]
                 (reset! state-stream stream)
                 (start-render-loop video canvas control stream))
               camera-permission-denied)))
    om/IWillUnmount
    (will-unmount [_]
      (when-let [stream @(:stream (om/get-state owner))]
        (doseq [track (.getTracks stream)]
          (.stop track)))
      (swap! (:control (om/get-state owner)) assoc :stop true))
    om/IRender
    (render [_]
      (html
       [:canvas {:ref "qr-canvas"}]))))
