(ns stylist-matching.ui.header
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]))

(defn header-back-navigation-molecule
  [{:header.back-navigation/keys [id back target]}]
  (when id
    (component/html
     [:a.block.p3
      (merge {:data-test id}
             (apply utils/route-back-or-to back target))
      [:div.flex.items-center.justify-center
       {:style {:height "24px" :width "20px"}}
       (svg/thick-left-arrow {})]])))

(defn header-cart-molecule
  [{:header.cart/keys [id value color]}]
  (when id
    (component/html
     [:a.block
      (merge {:data-test id}
             (apply utils/route-to [events/navigate-cart]))
      [:div.relative.flex.items-center.justify-center
       (svg/bag {:class (str "fill-" color)})
       (when value
         [:div.absolute.top-0.left-0.right-0.bottom-0.flex.items-end.justify-center.h7
          {:data-test (str id "-populated")
           :class     color}
          value])]])))

(defn header-title-molecule
  [{:header.title/keys [id primary]}]
  (when id
    (component/html
     [:div.h5.medium
      {:data-test id}
      primary])))

(defcomponent organism
  [data _ _]
  [:div#header
   [:div.flex.items-center.justify-between.bg-lavender-dark.white
    [:div.col-1
     (header-back-navigation-molecule data)]
    [:div
     (header-title-molecule data)]
    [:div.col-1.mr2
     (header-cart-molecule data)]]])

