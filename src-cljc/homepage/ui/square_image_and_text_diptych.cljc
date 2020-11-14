(ns homepage.ui.square-image-and-text-diptych
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defcomponent organism
  [{:keys [ucare-id
           primary
           secondary
           target
           link-text
           id
           pic-right-on-desktop?]} _ _]
  [:div.bg-refresh-gray.px2.flex-on-tb-dt.py1-on-mb
   (merge
    {:key id}
    (when pic-right-on-desktop?
      {:style {:flex-direction "row-reverse"}}))
   [:div.col-6-on-tb-dt
    (ui/defer-ucare-img
      {:class "block col-12"}
      ucare-id)]
   [:div.bg-white.p5.col-6-on-tb-dt.flex.flex-column.justify-center
    [:div.col-9-on-tb-dt.mx-auto
     [:div.proxima.title-1.bold.shout.center-align-on-mb.m3 primary]
     [:div.center-align-on-mb.m3 secondary]
     [:div.center-align-on-mb.m3
      (ui/button-small-underline-primary (merge
                                          (apply utils/route-to target)
                                          {:data-test id})
                                         link-text)]]]])
