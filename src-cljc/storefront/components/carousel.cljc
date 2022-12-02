(ns storefront.components.carousel
  (:require [clojure.string :as string]
            [storefront.component :as c]
            [storefront.components.ui :as ui]))

(c/defcomponent example-exhibit-component
  [{:keys [class]} _ _]
  [:div {:class class
         :style {:height "500px"}}])

(c/defcomponent carousel-image-component
  [{:keys [src type alt]} _ _]
  (c/html
   (cond
     (= "video" type)
     [:video.container-size.contents
      {:autoplay    "autoPlay"
       :loop        "loop"
       :muted       "muted"
       :playsinline "playsInline"
       :src         src}]

     :else
     (ui/img {:src      src
              :alt      alt
              :class    "container-size contents"
              :max-size 800}))))

(c/defcomponent component
  [{:keys [exhibits]} _ {:carousel/keys [exhibit-component]}]
  [:div.carousel-2022.hide-scroll-bar
   [:div.spacer]
   (map-indexed (fn [index exhibit]
                  [:div.exhibit
                   {:key   index}
                   (c/build exhibit-component exhibit)])
                exhibits)
   [:div.spacer]])
