(ns storefront.components.carousel
  (:require #?@(:cljs [[goog.functions]
                       [goog.object :as gobj]])
            [clojure.string :as string]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as m]
            [spice.core :as spice]))

(def spacer
  [:div {:style {:min-width "calc(5% - 0.5rem)"}}])

(c/defcomponent example-exhibit-component
  [{:keys [class test-index]} _ _]
  [:div {:class class
         :style {:height "500px"}} test-index])

(c/defcomponent carousel-image-component
  [{:keys [src type alt]} _ _]
  (c/html
   (cond
     (= "video" type)
     [:video.container-size
      {:autoPlay    "autoPlay"
       :loop        "loop"
       :muted       "muted"
       :playsInline "playsInline"
       :src         src
       :style       {:object-fit "cover"}}]

     :else
     (ui/img {:src      src
              :alt      alt
              :class    "container-size"
              :style    {:object-fit "cover"}
              :max-size 800}))))

(def scroll-debounce-timer (atom nil))

(defn scroll-into-view
  [container-el target-el]
  (.scrollTo container-el
             ;; `offsetLeft` appears to measure from the left side of the browser viewport. I am
             ;; unsure that this scrolling exactly matches to `scrollTo`'s expected coordinate
             ;; assumptions, which might concern itself with the container, rather than the
             ;; browswer's relative position.
             (-> target-el .-offsetLeft)
             0))

(defn intersection-handler [component entries]
  (let [intersected-el               (some->> entries
                                              (filter #(.-isIntersecting %))
                                              first
                                              .-target)
        group-index                  (some->> intersected-el
                                              .-dataset
                                              .-groupIndex
                                              spice/parse-int)
        exhibit-index                (some->> intersected-el
                                              .-dataset
                                              .-exhibitIndex
                                              spice/parse-int)
        current-exhibit-groups-count (:exhibit-groups-count (c/get-state component))]
    (apply c/set-state! (concat [component]
                                (when exhibit-index
                                  [:centered-exhibit-index exhibit-index])
                                (when (and group-index
                                           (>= (inc group-index)
                                               current-exhibit-groups-count))
                                  [:exhibit-groups-count (inc current-exhibit-groups-count)])))))

(defn attach-intersection-observers [carousel-el observer]
  #?(:cljs
     (when observer ;; WARNING: it should be impossible for observer to be nil, but it's confusingly showing up nil on acceptance
       (.disconnect observer)
       (doseq [exhibit-el (some->> carousel-el
                                   .-children
                                   array-seq
                                   (filter (fn [child] (-> child
                                                           .-classList
                                                           array-seq
                                                           set
                                                           (get "carousel-exhibit")))))]
         (.observe observer exhibit-el)))))

(c/defdynamic-component component
  (constructor
     [this props]
     (c/create-ref! this "carousel")
     {:exhibit-groups-count 3})

  (did-mount
   [this]
   #?(:clj nil
      :cljs
      (let [carousel-el (c/get-ref this "carousel")
            observer    (js/IntersectionObserver.
                         (partial intersection-handler this)
                         #js {:root      carousel-el
                              :threshold 0.9})]
        (attach-intersection-observers carousel-el observer)
        (gobj/set this "intersectionObserver" observer)

        ;; The Scroll Observer will fire after a scroll (debounced), scrolling the user back to the "home" exhibit group
        ;; TODO: In other places, we use goog.events/listen. Is there an advantage?
        (.addEventListener carousel-el "scroll"
                           (fn []
                             (js/clearTimeout @scroll-debounce-timer)
                             (reset! scroll-debounce-timer
                                     (js/setTimeout
                                      (fn []
                                        (let [carousel-bounding-box            (.getBoundingClientRect carousel-el)
                                              {:keys [centered-exhibit-index]} (c/get-state this)
                                              exhibit-count                    (-> this c/get-props :exhibits count)]
                                          #_(-> carousel-el .-style .-scroll-snap-type (set! "none"))
                                          (some->> carousel-el
                                                   .-children
                                                   array-seq
                                                   (filter (fn [child]
                                                             (let [dataset (.-dataset child)]
                                                               (and (= "1" (.-groupIndex dataset))
                                                                    (= (str centered-exhibit-index) (.-exhibitIndex dataset))))))
                                                   first
                                                   (scroll-into-view carousel-el))
                                          (c/set-state! this :exhibit-groups-count 3)
                                          #_(-> carousel-el .-style .-scroll-snap-type (set! "x mandatory"))))
                                      500))))
        ;; Scroll to first exhibit of "home" exhibit group
        (some->> carousel-el
                 .-children
                 array-seq
                 (filter (fn [child]
                           (let [dataset (.-dataset child)]
                             (and (= "1" (.-groupIndex dataset))
                                  (= "0" (.-exhibitIndex dataset))))))
                 first
                 (scroll-into-view carousel-el)))))

  (did-update
   [this]
   #?(:cljs (attach-intersection-observers (c/get-ref this "carousel") (.-intersectionObserver this))))

  (render
   [this]
   (let [{:keys [exhibits]}                                    (c/get-props this)
         {:carousel/keys [exhibit-component]}                  (c/get-opts this)
         {:keys [centered-exhibit-index exhibit-groups-count]} (c/get-state this)]
     (c/html
      [:div
       #_[:div "centered index " centered-exhibit-index]
       #_[:div "groups count " exhibit-groups-count]
       [:div.hide-scroll-bar
        {:id    "carousel" ;; TODO allow for multiple carousels on a page
         :style {:display          "flex"
                 :gap              "0.5rem"
                 :scroll-snap-type "x mandatory"
                 :overflow-x       "auto"
                 :overflow-y       "clip"}
         :ref   (c/use-ref this "carousel")}
        [:div {:style {:min-width "calc(5% - 0.5rem)"
                       :order     -1}}]
        (map-indexed (fn exhibit [dom-index exhibit]
                       (let [exhibit-idx (rem dom-index (count exhibits))
                             group-idx   (quot dom-index (count exhibits))
                             key         (str group-idx "-" exhibit-idx)]
                         [:div
                          {:key                key
                           :class              (str "carousel-exhibit")
                           :data-group-index   group-idx
                           :data-exhibit-index exhibit-idx
                           :style              {:width             "90%"
                                                :scroll-snap-align "center"
                                                :flex-shrink       0}}
                          (c/build exhibit-component (merge exhibit {:test-index key}))]))
                     (->> exhibits
                          cycle
                          (take (* (count exhibits) exhibit-groups-count))))
        [:div {:style {:min-width "calc(5% - 0.5rem)"
                       :order     9999}}]]]))))
