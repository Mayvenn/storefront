(ns storefront.components.carousel
  (:require [storefront.component :as c]))

(def spacer
  [:div {:style {:min-width "calc(5% - 0.5rem)"}}])

(c/defcomponent example-exhibit-component
  [{:keys [class]} _ _]
  [:div {:class class
         :style {:height "500px"}}])

(c/defcomponent component
  [{:keys [exhibits] :as props} _ {:carousel/keys [exhibit-component] :as opts}]
  [:div.hide-scroll-bar
   {:style {:display "flex"
            :gap "0.5rem"
            :scroll-snap-type "x mandatory"
            :overflow-x "auto"}}
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
