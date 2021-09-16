(ns mayvenn.visual.lib.image-grid
  (:require
   [storefront.component :as component :refer [defcomponent]]
   [mayvenn.visual.tools :refer [with]]
   [storefront.components.ui :as ui]))

(defcomponent hero-molecule
  [{:keys [image-url badge-url gap-in-num-px]} _ _]
  [:div.relative.flex-auto
   {:style {:margin-top (str gap-in-num-px "px")
            :margin-right (str gap-in-num-px "px")}}
   (ui/img {:class    "container-size"
            :style    {:object-position "50% 25%"
                       :object-fit      "cover"}
            :src      image-url
            :max-size 749})
   [:div.absolute.bottom-0.m1.justify-start
    {:style {:height "22px"
             :width  "22px"}}
    badge-url]])

(defcomponent height-adjusting-hair-image-molecule
  [{:keys [image-url length gap-in-num-px height-in-num-px]} _ {:keys [id]}]
  [:div.relative
   (ui/img
    {:key           id
     :class         "block"
     :style         {:margin-top (str gap-in-num-px "px")}
     :height        (str height-in-num-px "px")
     :width         (str height-in-num-px "px")
     :square-size   height-in-num-px
     :src           image-url})
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
        size-of-column-image (-> height-in-num-px
                                 (/ column-count)
                                 #?(:clj  identity
                                    :cljs Math/ceil))]
    [:div.flex
     {:style {:height (str (+ height-in-num-px
                              ;; NOTE: Rather than trying to account for the gaps in the columns,
                              ;;       just make the hero a bit larger. This causes everything to line up more smoothly because
                              ;;       you can pick a height that is divisible by the number of elements without having to mind the gap.
                              (* gap-in-num-px
                                 column-count)) "px")}}
     (component/build hero-molecule (with :hero data))
     [:div.flex.flex-column.justify-between
      (component/elements height-adjusting-hair-image-molecule
                          (update (with :hair-column data)
                                  :images
                                  (partial map
                                           (fn [image]
                                             (merge {:gap-in-num-px    gap-in-num-px
                                                     :height-in-num-px size-of-column-image}
                                                    image))))
                          :images)]]))
