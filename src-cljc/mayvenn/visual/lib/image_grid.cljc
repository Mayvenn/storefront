(ns mayvenn.visual.lib.image-grid
  (:require
   [clojure.string :as string]
   [storefront.component :as component :refer [defcomponent]]
   [mayvenn.visual.tools :refer [with]]
   [storefront.components.ui :as ui]))

(defcomponent ^:private hero-molecule
  [{:keys [image-url badge-url gap-in-num-px alt]} _ _]
  [:div.relative.flex-auto
   {:style {:margin-top   (str gap-in-num-px "px")
            :margin-right (str gap-in-num-px "px")}}
   (ui/img {:class    "container-size"
            :style    {:object-position "50% 25%"
                       :object-fit      "cover"}
            :src      image-url
            :max-size 749
            :alt      alt})
   [:div.absolute.bottom-0.m1.justify-start
    {:style {:height "22px"
             :width  "22px"}}
    badge-url]])

(defcomponent ^:private height-adjusting-hair-image-molecule
  [{:keys [image-url length alt gap-in-num-px height-in-num-px]} _ {:keys [id]}]
  [:div.relative
   (ui/img
    {:key         id
     :class       "block"
     :style       {:margin-top (str gap-in-num-px "px")}
     :height      (str height-in-num-px "px")
     :width       (str height-in-num-px "px")
     :alt         "" ; decorative image gets empty alt text
     :square-size height-in-num-px
     :src         image-url})
   [:div.absolute.top-0.right-0.content-4.m1
    length]])

(defcomponent hero-with-little-hair-column-molecule
  "Expects data shaped like
  {:height-in-num-px
   :gap-in-num-px
   :hero/image-url
   :hero/badge-url
   :hair-column/images [{:image-url blah
                         :length    blah}]} "
  [{:as data :keys [height-in-num-px gap-in-num-px]} _ _]
  (let [column-count         (count (:hair-column/images data))
        size-of-column-image (if (zero? column-count)
                               0
                               (-> height-in-num-px
                                   (/ column-count)
                                   #?(:clj  identity
                                      :cljs Math/ceil)))]
    [:div.flex
     {:style {:height (str (+ height-in-num-px
                              ;; NOTE: Rather than trying to account for the gaps in the columns,
                              ;;       just make the hero a bit larger. This causes everything to line up more smoothly because
                              ;;       you can pick a height that is divisible by the number of elements without having to mind the gap.
                              (* gap-in-num-px
                                 column-count)) "px")}}
     (component/build hero-molecule (with :hero data))
     (when (pos? column-count)
       [:div.flex.flex-column.justify-between
        (component/elements height-adjusting-hair-image-molecule
                            (update (with :hair-column data)
                                    :images
                                    (partial map
                                             (fn [image]
                                               (merge {:gap-in-num-px    gap-in-num-px
                                                       :height-in-num-px size-of-column-image}
                                                      image))))
                            :images)])]))

(defn ^:private hair-image-molecule
  [{:keys [image-url length grid-area id]}]
  [:div.relative
   {:id    id
    :style {:grid-area grid-area}}
   (ui/img
    {:key   id
     :alt   "" ; decorative images get empty alt text
     :class "container-size"
     :src   image-url})
   [:div.absolute.top-0.right-0.content-4.m1 length]])

(defcomponent square-hero-with-right-tiled-column-molecule
  "This is very similar to hero-with-little-hair-column-molecule, except that its size is determined entirely by its container
   and it tries very hard to make every image a square. Downside: depending on the number of images, the aspect ratio changes.

   Expects data shaped like:
  {:gap-px
   :hero/image-url
   :hero/badge-url
   :hair-column/images [{:image-url blah
                         :length    blah}]} "
  [{:as data :keys [gap-px :hair-column/images]} _ _]
  (let [row-count (count images)
        col-count (inc row-count)]
    [:div {:style {:display               "grid"
                   :gap                   (str gap-px "px")
                   :grid-template-columns (str "repeat( " col-count ", 1fr)")
                   :grid-template-rows    (str "repeat("  row-count ", 1fr)")
                   :aspect-ratio          (str col-count "/" row-count)}}
     ;; Hero
     [:div.relative.flex-auto {:style {:grid-area (str "1 / 1 / " col-count " / " col-count)}}
      (ui/img {:class    "container-size"
               :style    {:object-position "50% 25%"
                          :object-fit      "cover"}
               :src      (:hero/image-url data)
               :alt      ""
               :max-size 749})
      [:div.absolute.bottom-0.m1.justify-start
       {:style {:height "22px"
                :width  "22px"}}
       (:hero/badge-url data)]]

     ;; Tiles
     (->> images
          (map-indexed (fn [ix image]
                         (let [row-start (inc ix)
                               col-start col-count
                               row-end   (inc row-start)
                               col-end   (inc col-start)]
                           (assoc image
                                  :grid-area (string/join " / " [row-start col-start row-end col-end])
                                  :id ix))))
          (map hair-image-molecule))]))
