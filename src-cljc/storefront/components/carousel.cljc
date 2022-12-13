(ns storefront.components.carousel
  (:require [clojure.string :as string]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]))

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
        {:style {:object-fit "cover"}
         :src   src
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

(defn select-exhibit [this target-id]
  #?(:cljs
     (let [dt-exhibits-el                              (c/get-ref this "dt-exhibits")
           dt-exhibits-els                             (-> dt-exhibits-el .-children array-seq)
           mb-exhibits-el                              (c/get-ref this "mb-exhibits")
           mb-exhibits-els                             (->> mb-exhibits-el
                                                            .-children
                                                            array-seq
                                                            (filter #(-> %
                                                                         .-classList
                                                                         (.contains "exhibit"))))
           {:keys [selected-exhibit-changed-callback]} (c/get-opts this)]
       (when (< -1 target-id (count dt-exhibits-els))
         (.scrollTo dt-exhibits-el
                    0
                    (-> dt-exhibits-els
                        (nth target-id)
                        .-offsetTop)
                    ;; This is a better scrolling behavior, but it breaks Safari
                    #_#js{:top      (-> exhibits-els
                                        (nth target-id)
                                        .-offsetTop)
                          :behavior "smooth"})
         (.scrollTo mb-exhibits-el
                    (-> mb-exhibits-els
                        (nth target-id)
                        .-offsetLeft)
                    0)
         (c/set-state! this :selected-exhibit-idx target-id)))))

(defn increment-selected-exhibit [this]
  (let [{:keys [selected-exhibit-changed-callback]} (c/get-opts this)
        current-selected-exhibit-idx                (->>  this
                                                          c/get-props
                                                          :selected-exhibit-idx)
        target-selected-exhibit-idx                 (inc current-selected-exhibit-idx)
        exhibits-count                              (-> this c/get-props :exhibits count)]
    (when (< target-selected-exhibit-idx exhibits-count)
      (selected-exhibit-changed-callback target-selected-exhibit-idx))))

(defn decrement-selected-exhibit [this]
  (let [{:keys [selected-exhibit-changed-callback]} (c/get-opts this)
        current-selected-exhibit-idx                (->>  this
                                                          c/get-props
                                                          :selected-exhibit-idx)
        target-selected-exhibit-idx                 (dec current-selected-exhibit-idx)]
    (when (>= target-selected-exhibit-idx 0)
      (selected-exhibit-changed-callback target-selected-exhibit-idx))))

(defn attach-intersection-observers [carousel-el observer]
  #?(:cljs
     (when observer ;; WARNING: it should be impossible for observer to be nil, but it's confusingly showing up nil on acceptance
       (doseq [exhibit-el (some->> carousel-el
                                   .-children
                                   array-seq
                                   (filter #(-> % .-classList (.contains "exhibit"))))]
         (.observe observer exhibit-el)))))

(def intersection-debounce-timer (atom nil))

(c/defdynamic-component component
  (constructor
   [this props]
   (c/create-ref! this "dt-exhibits")
   (c/create-ref! this "mb-exhibits"))
  (did-mount
   [this]
   #?(:cljs
      (let [mb-exhibits-el (c/get-ref this "mb-exhibits")
            observer       (js/IntersectionObserver.
                            (fn [entries]
                              (js/clearTimeout @intersection-debounce-timer)
                              (reset! intersection-debounce-timer
                                      (js/setTimeout
                                       (fn []
                                         (some->> entries
                                                  (filter #(.-isIntersecting %))
                                                  first
                                                  .-target
                                                  .-dataset
                                                  .-index
                                                  spice.core/parse-int
                                                  ((:selected-exhibit-changed-callback (c/get-opts this)))))
                                       400)))
                            #js {:root      mb-exhibits-el
                                 :threshold 0.9})]
        (attach-intersection-observers mb-exhibits-el observer))))
  (did-update
   [this]
   (select-exhibit this (:selected-exhibit-idx (c/get-props this))))
  (render
   [this]
   (let [{:keys [exhibits selected-exhibit-idx]}              (c/get-props this)
         {:carousel/keys [exhibit-highlight-component
                          exhibit-thumbnail-component]
          :keys          [selected-exhibit-changed-callback]} (c/get-opts this)]
     (c/html
      [:div
       [:div.carousel-2022.hide-on-mb
        [:div.exhibit-highlight
         (c/build exhibit-highlight-component (nth exhibits selected-exhibit-idx))]
        [:div.exhibits.flex.flex-column
         [:a.center.flip-vertical
          (merge {:on-click (partial decrement-selected-exhibit this)}
                 (if (= 0 selected-exhibit-idx)
                   {:style {:filter "opacity(0.25)"}}
                   {:class "pointer"}))
          (svg/dropdown-arrow {:class  "fill-black"
                               :height "16px"
                               :width  "16px"})]
         [:div.flex.flex-column
          {:ref   (c/use-ref this "dt-exhibits")
           :style {:flex-basis 0
                   :flex-grow  1
                   :overflow   "hidden"
                   :gap        "0.5rem"
                   :position   "relative"}}
          (map-indexed (fn [index exhibit]
                         [:a.exhibit.relative.grid.pointer
                          {:key      index
                           :on-click (partial selected-exhibit-changed-callback index)
                           :style    {:grid-template-areas "\"thumbnail\""}}
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
          (merge {:on-click (partial increment-selected-exhibit this)}
                 (if (= (count exhibits) (inc selected-exhibit-idx))
                   {:style {:filter "opacity(0.25)"}}
                   {:class "pointer"}))
          (svg/dropdown-arrow {:class  "fill-black"
                               :height "16px"
                               :width  "16px"})]]]
       [:div.carousel-2022.hide-scroll-bar.hide-on-tb-dt
        {:ref (c/use-ref this "mb-exhibits")}
        [:div.spacer]
        (map-indexed (fn [index exhibit]
                       [:div.exhibit
                        {:key        index
                         :data-index index}
                        (c/build exhibit-highlight-component exhibit)])
                     exhibits)
        [:div.spacer]]]))))
