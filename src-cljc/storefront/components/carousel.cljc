(ns storefront.components.carousel
  (:require [clojure.string :as string]
            [storefront.component :as c]
            [storefront.components.ui :as ui]))

(def spacer
  [:div {:style {:min-width "calc(5% - 0.5rem)"}}])

(c/defcomponent example-exhibit-component
  [{:keys [class]} _ _]
  [:div {:class class
         :style {:height "500px"}}])

(c/defcomponent carousel-image-component
  [{:keys [src type alt]} _ _]
  (c/html
   (cond
     (= "video" type)
     [:video.container-size
      {:autoplay    "autoplay"
       :loop        "loop"
       :muted       "muted"
       :playsinline "playsinline"
       :src         src
       :style       {:object-fit "cover"}}]

     :else
     (ui/img {:src      src
              :alt      alt
              :class    "container-size"
              :style    {:object-fit "cover"}
              :max-size 800}))))

(c/defcomponent component
  [{:keys [exhibits]} _ {:carousel/keys [exhibit-component]}]
  [:div.hide-scroll-bar
   {:style {:display "flex"
            :gap "0.5rem"
            :scroll-snap-type "x mandatory"
            :overflow-x "auto"
            :overflow-y "hidden"}}
   spacer
   (map-indexed (fn [index exhibit]
                  [:div
                   {:key index
                    :style {:width "90%"
                            :scroll-snap-align "center"
                            :flex-shrink 0}}
                   (c/build exhibit-component exhibit)])
                exhibits)
   spacer])
