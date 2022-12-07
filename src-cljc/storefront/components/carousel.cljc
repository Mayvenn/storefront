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
                                      :left      "50%"
                                      :top       "50%"
                                      :transform "translate(-50%, -50%)"
                                      :opacity   "80%"}})]

      :else
      (ui/img {:src      src
               :alt      alt
               :class    "container-size contents"
               :style    {:object-fit "cover"}
               :max-size 800}))]))

(c/defcomponent mobile-component
  [{:keys [exhibits]} _ {:carousel/keys [exhibit-highlight-component]}]
  [:div.carousel-2022.hide-scroll-bar.hide-on-tb-dt
   [:div.spacer]
   (map-indexed (fn [index exhibit]
                  [:div.exhibit
                   {:key index}
                   (c/build exhibit-highlight-component exhibit)])
                exhibits)
   [:div.spacer]])

(defn select-exhibit [this target-id]
  #?(:cljs
     (let [exhibits-el  (c/get-ref this "exhibits")
           exhibits-els (-> exhibits-el .-children array-seq)]
       (when (< -1 target-id (count exhibits-els))
         (.scrollTo exhibits-el
                    0
                    (-> exhibits-els
                        (nth target-id)
                        .-offsetTop))
         (c/set-state! this :selected-exhibit-idx target-id)))))

(defn increment-selected-exhibit [this]
  (->> (c/get-state this)
       :selected-exhibit-idx
       inc
       (select-exhibit this)))

(defn decrement-selected-exhibit [this]
  (->> (c/get-state this)
       :selected-exhibit-idx
       dec
       (select-exhibit this)))

(c/defdynamic-component desktop-component
  (constructor
   [this props]
   (c/create-ref! this "exhibits")
   {:selected-exhibit-idx 0})

  (render
   [this]
   (let [{:keys [exhibits]}              (c/get-props this)
         {:carousel/keys
          [exhibit-highlight-component
           exhibit-thumbnail-component]} (c/get-opts this)
         {:keys [selected-exhibit-idx]}  (c/get-state this)]
     (c/html
      [:div.carousel-2022.hide-on-mb
       [:div.exhibit-highlight
        (c/build exhibit-highlight-component (get exhibits selected-exhibit-idx))]
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
         {:ref   (c/use-ref this "exhibits")
          :style {:flex-basis 0
                  :flex-grow  1
                  :overflow   "hidden"
                  :gap        "0.5rem"
                  :position   "relative"}}
         (map-indexed (fn [index exhibit]
                        [:div.exhibit.relative
                         (merge {:key      index
                                 :on-click (partial select-exhibit this index)}
                                (when (= index selected-exhibit-idx)
                                  {:class "border border-p-color"}))
                         (c/build (or exhibit-thumbnail-component exhibit-highlight-component) exhibit)])
                      exhibits)]
        [:a.center
         (merge {:on-click (partial increment-selected-exhibit this)}
                (if (= (count exhibits) (inc selected-exhibit-idx))
                  {:style {:filter "opacity(0.25)"}}
                  {:class "pointer"}))
         (svg/dropdown-arrow {:class  "fill-black"
                              :height "16px"
                              :width  "16px"})]]]))))

(c/defcomponent component
  [props _ opts]
  [:div
   (c/build desktop-component props {:opts opts})
   (c/build mobile-component props {:opts opts})])
