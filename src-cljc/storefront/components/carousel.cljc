(ns storefront.components.carousel
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]))

(def spacer
  [:div {:style {:min-width "calc(5% - 0.5rem)"}}])

(c/defcomponent example-exhibit-component
  [{:keys [class]} _ _]
  [:div {:class class
         :style {:height "500px"}}])

(c/defcomponent carousel-image-component
  [{:keys [src alt]} _ _]
  (ui/img {:src      src
           :alt      alt
           :class    "container-size"
           :style    {:object-fit "cover"}
           :max-size 800}))

(c/defcomponent component
  [{:keys [exhibits] :as props} _ {:carousel/keys [exhibit-component] :as opts}]
  [:div.hide-scroll-bar
   {:style {:display "flex"
            :gap "0.5rem"
            :scroll-snap-type "x mandatory"
            :overflow-x "auto"
            :overflow-y "clip"}}
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
