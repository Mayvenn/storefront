(ns mayvenn.visual.ui.image-grids
  (:require
   [storefront.component :as component :refer [defcomponent]]
   [mayvenn.visual.tools :refer [with]]
   [storefront.components.ui :as ui]))

(defcomponent hero-molecule
  [{:keys [image-url badge-url]} _ _]
  [:div.relative
   {:style {:padding-right "3px"}}
   (ui/img {:class    "container-size"
            :style    {:object-position "50% 25%"
                       :object-fit      "cover"}
            :src      image-url
            :max-size 749})
   [:div.absolute.bottom-0.m1.justify-start
    {:style {:height "22px"
             :width  "22px"}}
    badge-url]])

(defcomponent hair-image-molecule
  [{:keys [image-url length]} _ {:keys [id]}]
  [:div.relative
   [:img.block
    {:key id
     :src image-url}]
   [:div.absolute.top-0.right-0.content-4.m1
    length]])


(defcomponent hero-with-little-column-molecule
  "Expects data shaped like
  {:height-px
   :hero/image-url
   :hero/badge-url
   :column/images [{:image-url blah
                    :length blah}]} "
  [{:as data :keys [height-px]} _ _]
  [:div.flex
   {:style {:height (str height-px "px")}}
   (component/build hero-molecule (with :hero data))
   [:div.flex.flex-column.justify-between.mlp2
    (component/elements hair-image-molecule
                        (with :column data)
                        :images)]])
