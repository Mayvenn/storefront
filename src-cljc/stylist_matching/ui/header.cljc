(ns stylist-matching.ui.header
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]))

(defn header-back-navigation-molecule
  [{:header.back-navigation/keys [id target]}]
  (when id
    (component/html
     [:a.block.p3.inherit-color
      (merge {:data-test id}
             (utils/route-back {:navigation-message target}))
      [:div.flex.items-center.justify-center
       {:style {:height "24px" :width "20px"}}
       (svg/thick-left-arrow {})]])))

(defn header-cart-molecule
  [{:header.cart/keys [id]}]
  (when id
    (component/html
     [:div
      [:a.block.p3
       (merge {:data-test id}
              (apply utils/route-to [events/navigate-cart]))
       (ui/ucare-img
        {:width "20"}
        "02f9e7fb-510f-458e-8be7-090399aad4de")]])))

(defn header-title-molecule
  [{:header.title/keys [id primary]}]
  (when id
    (component/html
     [:div.h5.medium
      {:data-test id}
      primary])))

(defn organism
  [data _ _]
  (component/create
   [:div#header
    [:div.flex.items-center.justify-between.bg-lavender-dark
     (header-back-navigation-molecule data)
     (header-title-molecule data)
     (header-cart-molecule data)]]))

