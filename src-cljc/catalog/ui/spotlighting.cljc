(ns catalog.ui.spotlighting
  (:require [storefront.component :as c]
            [storefront.assets :as assets]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

;; GROT
(defn ^:private spotlight-with-caption-old [{:keys [title image-src nav-event alt]}]
 [:a.block.black
  (apply utils/route-to nav-event)
  (ui/circle-ucare-img {:width "108" :alt alt} image-src)
  [:div.center.mt3 title]])
;; GROT
(c/defcomponent organism-old
  [{:keys [title spotlights]} _ _]
  [:div.flex.flex-column.items-stretch.col-8-on-tb.col-6-on-dt.mx-auto
   [:div.center.title-3.proxima.shout.my6 title]
   (into [:div.flex.justify-evenly] (map spotlight-with-caption-old) spotlights)])

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
