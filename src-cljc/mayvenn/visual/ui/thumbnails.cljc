(ns mayvenn.visual.ui.thumbnails
  (:require [storefront.components.ui :as ui]))

(defn stickered-square
  [{:keys [id ucare-id sticker-label]}]
  [:div.relative.pt1
   {:key   id
    :style {:height "45px"
            :width  "48px"}}
   (when-let [sticker-id (some->> sticker-label (str id "-"))]
     [:div.absolute.z1.circle.border.border-gray.bg-white.proxima.title-3.flex.items-center.justify-center
      {:key       sticker-id
       :data-test sticker-id
       :style     {:height "26px"
                   :width  "26px"
                   :right  "-10px"
                   :top    "-5px"}}
      sticker-label])
   (when-let [image-id (some->> ucare-id (str id "-"))]
     [:div.flex.items-center.justify-center
      {:style     {:height "45px"
                   :width  "48px"}
       :key       image-id
       :data-test image-id}
      (ui/ucare-img {:width "48"
                     :class "block border border-cool-gray"}
                    ucare-id)])])
