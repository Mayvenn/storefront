(ns voucher.components.qr-reader
  (:require [sablono.core :refer [html]]
            [om.core :as om]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]))

(defn ^:private video-offset [v1 v2]
  (if (> v1 v2)
    (/ (- v1 v2) 2)
    0))

(defn draw [video ctx [cw ch]]
  (let [vw (.-videoWidth video)
        vh (.-videoHeight video)]
    (.drawImage ctx
                video
                0 0
                vw vh
                0 0
                cw ch)))

(defn get-image-data [ctx [cw ch]]
  (let [image-data (.getImageData ctx 0 0 cw ch)]
    {:data   (.-data image-data)
     :height (.-height image-data)
     :width  (.-width image-data)}))

(defn image-recognized [code]
  (messages/handle-message events/control-voucher-qr-redeem {:code code}))

(defn read-qr-response [img-data]
  (let [clj-img-data (js->clj img-data :keywordize-keys true)]
    (when (->> (:chunks clj-img-data)
               (map :type)
               (some (partial contains? #{"byte" "alphanumeric"})))
      (:data clj-img-data))))

(defn draw-line [ctx begin & rest]
  (.beginPath ctx)
  (.moveTo ctx (:x begin) (:y begin))
  (doseq [{:keys [x y]} rest]
    (.lineTo ctx x y))
  (set! (.-lineWidth ctx) 10)
  (set! (.-strokeStyle ctx) "white")
  (.stroke ctx))

(defn draw-brackets [ctx [cw ch]]
  (let [padding (/ cw 8)
        length  (/ cw 10)]
    (draw-line ctx
               {:x (- cw padding)
                :y (- ch (+ padding length))}
               {:x (- cw padding)
                :y (- ch padding)}
               {:x (- cw (+ padding length))
                :y (- ch padding)})
    (draw-line ctx
               {:x padding
                :y (- ch (+ padding length))}
               {:x padding
                :y (- ch padding)}
               {:x (+ padding length)
                :y (- ch padding)})
    (draw-line ctx
               {:x (- cw (+ padding length))
                :y padding}
               {:x (- cw padding)
                :y padding}
               {:x (- cw padding)
                :y (+ padding length)})
    (draw-line ctx
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
        scale                   1
        canvas-style            (.-style canvas)]

    (set! (.-width canvas) canvas-css-width)
    (set! (.-height canvas) canvas-css-height)

    [canvas-css-width canvas-css-height]))

(defn draw-text [ctx [cw ch]]
  (let [scale     1
        font-size (* 16 scale)]
    (set! (.-font ctx) (str "bold " font-size "px Roboto"))
    (set! (.-fillStyle ctx) "white")
    (set! (.-textAlign ctx) "center")
    (.fillText ctx "Point camera at QR code"
               (/ cw 2) (+ font-size (/ ch 40)))))

(defn tick [video canvas control timestamp]
  (when-not (get @control :stop)
    (when (>= (.-readyState video) (.-HAVE_FUTURE_DATA video))
      (let [canvas-size (resize-canvas video canvas)
            ctx (.getContext canvas "2d")]
        (draw video ctx canvas-size)
        (try
          (let [{:keys [data width height]} (get-image-data ctx canvas-size)]
            (when-let [voucher-code (read-qr-response (js/jsQR data width height))]
              (image-recognized voucher-code)))
          (catch :default e
            (js/console.log "Error while reading video stream: " e)
            nil))
        (draw-brackets ctx canvas-size)
        (draw-text ctx canvas-size)))
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
