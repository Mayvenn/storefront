(ns catalog.ui.return-address-labels
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

;; This is named after the little customized address labels nonprofits mail you.
;; It is used on the category page to make the nav links below the spotlight.

(defn ^:private image-with-label [{:keys [title image-src nav-event]}]
  [:a.block.black.bg-cool-gray.flex.items-center
   (apply utils/route-to nav-event)
   (ui/img {:src         image-src
            :square-size 40
            :max-size    "40px"
            :class       "mp3"})
   [:div.proxima.content-3.p1 title]])

(c/defcomponent organism
  [{:keys [title labels]} _ _]
  [:div.my6.mx-auto.px2.col-10-on-tb-dt
   [:div.canela.title-2.center.mb4 title]
   (into [:div.grid.grid-cols-2-on-mb.grid-cols-3-on-tb-dt.gap-2] (map image-with-label) labels)])
