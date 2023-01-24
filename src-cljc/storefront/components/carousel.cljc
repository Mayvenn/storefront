(ns storefront.components.carousel
  (:require [clojure.string :as string]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.transitions :as t]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            [storefront.events :as events]))

;; ---- Behavior

;; TODO(corey) assumes that the carousel is a product carousel
(defmethod t/transition-state e/carousel|jumped
  [_ _ {:keys [id idx]} state]
  (assoc-in state [:models :carousels id :idx] idx))

;; ---- Read Model

(defn <-
  [state id]
  (or
   (get-in state [:models :carousels id])
   {:idx 0}))

;; ---- Stock Components

(c/defcomponent example-exhibit-component
  [{:keys [class]} _ _]
  [:div
   {:class class
    :style {:aspect-ratio "1 / 1"}}])

(c/defcomponent product-carousel-highlight
  [{:keys [src type alt]} _ _]
  (c/html
   [:div
    {:style {:aspect-ratio "3 / 4"
             :overflow     "hidden"}}
    (cond
      (= "video" type)
      [:video.container-size.contents
       {:autoPlay    "autoPlay"
        :loop        "loop"
        :muted       "muted"
        :playsInline "playsInline"
        :style       {:object-fit "cover"}
        :aria-label  alt
        :src         src}]

      :else
      (ui/img {:src      src
               :alt      alt
               :class    "container-size contents"
               :style    {:object-fit "cover"}
               :max-size 800}))]))

(c/defcomponent product-carousel-thumbnail
  [{:keys [src type alt]} _ _]
  (c/html
   [:div
    {:style {:aspect-ratio "3 / 4"
             :overflow     "hidden"}}
    (cond
      (= "video" type)
      [:div
       {:style {:display "contents"}}
       [:video.container-size.contents
        {:style      {:object-fit "cover"}
         :aria-label alt
         :src        src
         ;; if we want to change the still frame, use this instead
         ;; :src         (str src "#t=5.1")
         }]
       (svg/white-play-video {:class "absolute"
                              :style {:width     "50px"
                                      :height    "50px"
                                      :left      "50%"
                                      :top       "50%"
                                      :transform "translate(-50%, -50%)"
                                      :opacity   "80%"}})]

      :else
      (ui/img {:src      src
               :alt      alt
               :class    "container-size contents"
               :style    {:object-fit          "cover"
                          :backface-visibility "hidden"}
               :max-size 200}))]))

(c/defcomponent slider-image-exhibit
  [{:keys [src type alt]} _ _]
  (c/html
   [:div
    {:style {:overflow "hidden"}}
    (ui/img {:src      src
             :alt      alt
             :class    "container-size contents"
             :style    {:object-fit "cover"}})]))

(defn ^:private scroll-to-exhibit-idx [carousel target-idx]
  #?(:cljs
     (let [dt-exhibits-el  (c/get-ref carousel "dt-exhibits")
           dt-exhibits-els (some-> dt-exhibits-el .-children array-seq)
           mb-exhibits-el  (c/get-ref carousel "mb-exhibits")
           mb-exhibits-els (some->> mb-exhibits-el
                                    .-children
                                    array-seq
                                    (filter #(-> %
                                                 .-classList
                                                 (.contains "exhibit"))))]
       (when (and (seq dt-exhibits-els)
                  (< -1 target-idx (count dt-exhibits-els)))
         (.scrollTo dt-exhibits-el
                    0
                    (-> dt-exhibits-els
                        (nth target-idx)
                        .-offsetTop)
                    ;; This is a better scrolling behavior, but it breaks Safari
                    #_#js{:top      (-> exhibits-els
                                        (nth target-idx)
                                        .-offsetTop)
                          :behavior "smooth"}))

       (when (and (seq mb-exhibits-els)
                  (< -1 target-idx (count mb-exhibits-els)))
         (.scrollTo mb-exhibits-el
                    (- (-> mb-exhibits-els
                           (nth target-idx)
                           .-offsetLeft)
                       (-> mb-exhibits-el
                           .-offsetLeft))
                    0)))))

(defn increment-selected-exhibit [carousel]
  (let [{:carousel/keys [id]}        (c/get-opts carousel)
        current-selected-exhibit-idx (:selected-exhibit-idx (c/get-props carousel))
        target-selected-exhibit-idx  (inc current-selected-exhibit-idx)
        exhibits-count               (-> carousel c/get-props :exhibits count)]
    (when (< target-selected-exhibit-idx exhibits-count)
      (publish events/carousel|jumped {:id  id
                                       :idx target-selected-exhibit-idx}))))

(defn decrement-selected-exhibit [carousel]
  (let [{:carousel/keys [id]}        (c/get-opts carousel)
        current-selected-exhibit-idx (:selected-exhibit-idx (c/get-props carousel))
        target-selected-exhibit-idx  (dec current-selected-exhibit-idx)]
    (when (>= target-selected-exhibit-idx 0)
      (publish events/carousel|jumped {:id  id
                                       :idx target-selected-exhibit-idx}))))

(defn attach-intersection-observers [carousel-el observer]
  #?(:cljs
     (when observer ;; WARNING: it should be impossible for observer to be nil, but it's confusingly showing up nil on acceptance
       (doseq [exhibit-el (some->> carousel-el
                                   .-children
                                   array-seq
                                   (filter #(-> % .-classList (.contains "exhibit"))))]
         (.observe observer exhibit-el)))))


(defn carousel-with-sidebar [carousel]
  #?(:cljs
     (let [{:keys [exhibits
                   selected-exhibit-idx]} (c/get-props carousel)
           {:carousel/keys
            [exhibit-highlight-component
             exhibit-thumbnail-component
             id]}                         (c/get-opts carousel)]
       [:div.carousel-2022-with-sidebar
        [:div.exhibit-highlight
         (c/build exhibit-highlight-component (nth exhibits selected-exhibit-idx))]
        [:div.exhibits.flex.flex-column
         [:a.center.flip-vertical
          (merge {:on-click   (partial decrement-selected-exhibit carousel)
                  :href       "javascript:void(0);"
                  :aria-label "Move to previous image"}
                 (if (= 0 selected-exhibit-idx)
                   {:style {:filter "opacity(0.25)"}}
                   {:class "pointer"}))
          (svg/dropdown-arrow {:class  "fill-black"
                               :height "16px"
                               :width  "16px"})]
         [:div.flex.flex-column
          {:ref   (c/use-ref carousel "dt-exhibits")
           :style {:flex-basis 0
                   :flex-grow  1
                   :overflow   "hidden"
                   :gap        "0.5rem"
                   :position   "relative"}}
          (map-indexed (fn [index exhibit]
                         [:a.exhibit.relative.grid.pointer
                          {:key        index
                           :href       "javascript:void(0);"
                           :aria-label "View image"
                           :on-click   #(publish events/carousel|jumped {:id  id
                                                                         :idx index})
                           :style      {:grid-template-areas "\"thumbnail\""}}
                          [:div
                           (merge
                            {:style {:grid-area "thumbnail"
                                     :z-index   1}}
                            (when (= index selected-exhibit-idx)
                              {:class "border border-width-3 border-s-color"}))]
                          [:div
                           {:style {:grid-area "thumbnail"}}
                           (c/build (or exhibit-thumbnail-component exhibit-highlight-component) exhibit)]])
                       exhibits)]
         [:a.center
          (merge {:on-click   (partial increment-selected-exhibit carousel)
                  :href       "javascript:void(0);"
                  :aria-label "Move to next image"}
                 (if (= (count exhibits) (inc selected-exhibit-idx))
                   {:style {:filter "opacity(0.25)"}}
                   {:class "pointer"}))
          (svg/dropdown-arrow {:class  "fill-black"
                               :height "16px"
                               :width  "16px"})]]])))

(defn slider-carousel
  "A carousel "
  [carousel]
  #?(:cljs
     (let [{:keys [exhibits]} (c/get-props carousel)
           {:carousel/keys
            [exhibit-highlight-component]} (c/get-opts carousel)]
       [:div.carousel-2022-slider.hide-scroll-bar
        {:ref (c/use-ref carousel "mb-exhibits")}
        [:div.spacer]
        (map-indexed (fn [index exhibit]
                       [:div.exhibit
                        {:key        index
                         :data-index index}
                        (c/build exhibit-highlight-component exhibit)])
                     exhibits)
        [:div.spacer]])))

(def intersection-debounce-timer (atom nil))

(c/defdynamic-component component
  (constructor
   [this props]
   (c/create-ref! this "dt-exhibits")
   (c/create-ref! this "mb-exhibits"))
  (did-mount
   [this]
   (let [{:carousel/keys [id]} (c/get-opts this)]
     #?(:cljs
        (let [mb-exhibits-el (c/get-ref this "mb-exhibits")
              observer       (js/IntersectionObserver.
                              (fn carousel-intersection-observer-handler [entries]
                                ;; TODO (le): Currently there is a bug when on desktop slider-mode-only.
                                ;; When we scroll one element to the right, there is a loop which causes scrolling all
                                ;; the way to the right of the carousel, with two indecies per 400ms step.

                                ;; This is caused by intersection observer only including entries whose intersection
                                ;; have changed. Because we are showing three images on the screen at a time
                                ;; when we scroll one image to the right, the next "selected exhibit"'s intersection
                                ;; has not changed but rather the element two exhibits down has.
                                ;; This causes this code to identify the proper index as the right-most exhibit.
                                ;; Then it sends the `carousel|jumped` event to be published which
                                ;; then causes the `:selected-exhibit-idx` to be set to the right-most exhibit.
                                ;; Which causes component `did-update` to fire, which scrolls to that incorrect index.
                                ;; Scrolling to that incorrect index then causes the visibility to change, starting the
                                ;; loop over again.
                                (js/clearTimeout @intersection-debounce-timer)
                                (reset! intersection-debounce-timer
                                        (js/setTimeout
                                         (fn carousel-intersection-observer-timer []
                                           (some->> entries
                                                    (filter #(.-isIntersecting %))
                                                    first
                                                    .-target
                                                    .-dataset
                                                    .-index
                                                    spice.core/parse-int
                                                    ((fn [index]
                                                       (publish events/carousel|jumped {:id  id
                                                                                        :idx index})))))
                                         400)))
                              #js {:root      mb-exhibits-el
                                   :threshold 0.9})]
          (attach-intersection-observers mb-exhibits-el observer)))))
  (did-update
   [this]
   (scroll-to-exhibit-idx this (:selected-exhibit-idx (c/get-props this))))
  (render
   [this]
   (let [{:carousel/keys [slider-only-mode]} (c/get-opts this)]
     (c/html
      [:div
       (when (not slider-only-mode)
         [:div.hide-on-mb
          (carousel-with-sidebar this)])
       [:div {:class (when (not slider-only-mode) "hide-on-tb-dt")}
        (slider-carousel this)]]))))
