(ns catalog.ui.spotlighting
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private spotlight-with-caption [{:keys [title image-src nav-event]}]
  [:a.block.black
   (apply utils/route-to nav-event)
   ;; (ui/circle-picture {:width "108px"} (str "//ucarecdn.com/" image-src "/"))
   (ui/circle-ucare-img {:width 108} image-src)
   [:div.center.mt3 title]])

(c/defcomponent organism
  [{:keys [title spotlights]} _ _]
  [:div.flex.flex-column.items-stretch.col-8-on-tb.col-6-on-dt.mx-auto
   [:div.center.title-3.proxima.shout.my6 title]
   (into [:div.flex.justify-evenly] (map spotlight-with-caption) spotlights)])
