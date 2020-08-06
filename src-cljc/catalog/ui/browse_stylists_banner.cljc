(ns catalog.ui.browse-stylists-banner
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]))

(defcomponent organism
  [{:browse-stylists-banner/keys [id icon title subtitle image-ucare-id button-copy nav-event] :as data} _ _]
  (when id
    [:a.block.inherit-color
     (merge {:data-test id}
            (apply utils/route-to nav-event))
     [:div.flex.justify-center
      {:class (:browse-stylists-banner/class data)}
      [:div.p5.center.col-12.flex.flex-column.items-center
       (svg/symbolic->html icon)
       [:div.title-2.canela title]
       [:div.mt2.content-3 subtitle]
       [:div.mt4.col-10.col-8-on-tb
        (ui/button-medium-primary {} button-copy)]]]]))
