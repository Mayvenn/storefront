(ns storefront.components.carousel
  (:require [clojure.string :as string]
            [storefront.component :as c]
            [storefront.components.ui :as ui]))

(c/defcomponent example-exhibit-component
  [{:keys [class]} _ _]
  [:div
   [:div.hide-on-mb {:class class}]
   [:div.hide-on-tb-dt {:class class
                        :style {:height "500px"}}]])

(c/defcomponent carousel-image-component
  [{:keys [src type alt]} _ _]
  (c/html
   (cond
     (= "video" type)
     [:video.container-size.contents
      {:autoPlay    "autoPlay"
       :loop        "loop"
       :muted       "muted"
       :playsInline "playsInline"
       :src         src}]

     :else
     (ui/img {:src      src
              :alt      alt
              :class    "container-size contents"
              :max-size 800}))))

(c/defcomponent mobile-component
  [{:keys [exhibits]} _ {:carousel/keys [exhibit-component]}]
  [:div.carousel-2022.hide-scroll-bar.hide-on-tb-dt
   [:div.spacer]
   (map-indexed (fn [index exhibit]
                  [:div.exhibit
                   {:key index}
                   (c/build exhibit-component exhibit)])
                exhibits)
   [:div.spacer]])

(c/defdynamic-component desktop-component
  (render
   [this]
   (let [{:keys [exhibits]}    (c/get-props this)
         {:carousel/keys
          [exhibit-component]} (c/get-opts this)]
     (c/html
      [:div.carousel-2022.hide-on-mb
       [:div.selected-exhibit
        "heyyyyy"]
       [:div.exhibits
        (map-indexed (fn [index exhibit]
                       [:div.exhibit
                        {:key index}
                        (c/build exhibit-component exhibit)])
                     exhibits)]]))))

(c/defcomponent component
  [props _ opts]
  [:div
   (c/build desktop-component props {:opts opts})
   (c/build mobile-component props {:opts opts})])
