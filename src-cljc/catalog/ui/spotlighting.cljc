(ns catalog.ui.spotlighting
  (:require [storefront.component :as c]
            [storefront.assets :as assets]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private spotlight-with-caption [{:keys [title icon image-src nav-event alt]}]
  (let [width "108"]
    [:a.block.black.mx3
     (merge (apply utils/route-to nav-event)
            {:style {:width (str width "px")}})
       (if image-src
         (ui/circle-ucare-img {:width "108" :alt alt} image-src)
         [:img {:src   (assets/path icon)
                :width 108
                :alt   alt}])
     [:div.center.mt3 title]]))

(c/defcomponent organism
  [{:keys [spotlights]} _ _]
  (into [:div.flex.overflow-scroll.hide-scroll-bar]
        (map spotlight-with-caption)
        spotlights))
