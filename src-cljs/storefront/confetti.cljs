(ns storefront.confetti)

;; This implementation is based on the dom-confetti library found at:
;; https://github.com/daniel-lundin/dom-confetti

(defn ^:private create-elements
  [{:keys [node element-count colors width height]}]
  (mapv
   (fn [i]
     (let [color   (nth colors (mod i (count colors)))
           element (js/document.createElement "div")]
       (set! element.style.width width)
       (set! element.style.backgroundColor color)
       (set! element.style.height height)
       (set! element.style.position "absolute")
       (set! element.style.willChange "transform, opacity")
       (set! element.style.visibility "hidden")

       (.appendChild node element)
       element))
   (range 0 element-count)))

(defn ^:private random-physics [{:keys [angle spread start-velocity]}]
  (let [rad-angle  (* angle (/ js/Math.PI 180))
        rad-spread (* spread (/ js/Math.PI 180))]
    {:x                0
     :y                0
     :wobble           (* 10 (js/Math.random))
     :wobble-speed     (+ 0.1 (* 0.1 (js/Math.random)))
     :velocity         (+ (* start-velocity 0.5)
                          (* (js/Math.random) start-velocity))
     :angle-2d         (- (* rad-spread 0.5)
                          (* rad-spread (js/Math.random))
                          rad-angle)
     :angle-3d         (- (* (js/Math.random) (/ js/Math.PI 2))
                          (/ js/Math.PI 4))
     :tilt-angle       (* (js/Math.random) js/Math.PI)
     :tilt-angle-speed (+ 0.1 (* (js/Math.random) 0.3))}))

(defn advance-physics
  [drag-friction
   {:keys [angle-2d
           angle-3d
           velocity
           wobble-speed
           tilt-angle-speed]
    :as   physics}]
  (-> physics
      (update :x + (* velocity (js/Math.cos angle-2d)))
      (update :y + (+ (* velocity (js/Math.sin angle-2d)) 3))
      (update :z + (* velocity (js/Math.sin angle-3d)))
      (update :wobble + wobble-speed)
      (update :velocity - (* velocity drag-friction))
      (update :tilt-angle + tilt-angle-speed)))

(defn ^:private update-fetti
  [progress drag-friction {:keys [element physics] :as fetti}]
  (let [{:keys [wobble x y tilt-angle]
         :as   physics'} (advance-physics drag-friction physics)
        wobble-x         (+ x (* 10 (js/Math.cos wobble)))
        wobble-y         (+ y (* 10 (js/Math.sin wobble)))
        transform        (str "translate3d(" wobble-x "px, " wobble-y "px, 0) rotate3d(1, 1, 1, " tilt-angle "rad)")]
    (set! element.style.visibility "visible")
    (set! element.style.transform transform)
    (set! element.style.opacity (- 1 progress))

    (assoc fetti :physics physics')))

(defn ^:private animate [{:keys [node drag-friction duration delay]} fettis]
  (let [start-time-state (atom nil)
        fettis           (atom fettis)
        make-update-fn   (fn make-update-fn [resolve-fn]
                           (fn update-fn [time]
                             (let [start-time          (or @start-time-state (reset! start-time-state time))
                                   elapsed-time        (- time start-time)
                                   progress            (if (= start-time time) 0 (/ elapsed-time duration))
                                   [fetti-to-update
                                    fetti-left-behind] (split-at (js/Math.ceil (/ elapsed-time delay)) @fettis)
                                   updated-fetti       (mapv (partial update-fetti progress drag-friction) fetti-to-update)]
                               (reset! fettis (concat updated-fetti fetti-left-behind))
                               (if (< elapsed-time duration)
                                 (js/requestAnimationFrame update-fn)
                                 (doseq [fetti @fettis]
                                   (when (= node (.-parentNode (:element fetti)))
                                     (.removeChild node (:element fetti)))
                                   (resolve-fn))))))]
    (js/Promise. (fn [resolve] (js/requestAnimationFrame (make-update-fn resolve))))))

(defn burst
  ([node]
   (burst node {}))
  ([node {:keys [element-count colors width height
                 angle spread start-velocity duration
                 drag-friction delay]
          :or   {element-count  100
                 colors         ["#40CBAC" "#7E006D" "#FFC0C6"]
                 width          "8px"
                 height         "10px"
                 angle          90
                 spread         40
                 start-velocity 50
                 drag-friction  0.1
                 duration       4000
                 delay          0}}]
   (->> (create-elements {:node          node
                          :element-count element-count
                          :colors        colors
                          :width         width
                          :height        height})
        (mapv (fn [e] {:element e
                       :physics (random-physics {:angle          angle
                                                 :spread         spread
                                                 :start-velocity start-velocity})}))
        (animate {:node          node
                  :drag-friction drag-friction
                  :duration      duration
                  :delay         delay}))))
