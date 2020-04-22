(ns catalog.ui.category-hero
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private category-hero-tag-molecule
  [{:category-hero.tag/keys [primary]}]
  (when primary
    [:div.s-color.title-3.proxima.bold.shout
     primary]))

(defn ^:private category-hero-title-molecule
  [{:category-hero.title/keys [primary]}]
  [:div.h1.title-1.canela primary])

(defn ^:private category-hero-icon-molecule
  [{:category-hero.icon/keys [image-src]}]
  (when image-src
    [:div.mt4 [:img {:src   image-src
                     :style {:width "54px"}}]]))

(defn ^:private category-hero-body-molecule
  [{:category-hero.body/keys [primary]}]
  [:div.content-2.proxima
   primary])

(defn ^:private category-hero-action-molecule
  [{:category-hero.action/keys [label target]}]
  (when (and label target)
    [:div.mt3
     (ui/button-small-underline-black
      {:on-click (apply utils/send-event-callback target)}
      label)]))

(c/defcomponent organism
  [data _ _]
  [:div.bg-warm-gray.center.mx-auto
   [:div.px2.py10
    (category-hero-tag-molecule data)
    (category-hero-title-molecule data)
    (category-hero-icon-molecule data)
    [:div.my3.mx6-on-mb.col-8-on-tb-dt.mx-auto-on-tb-dt
     (category-hero-body-molecule data)
     (category-hero-action-molecule data)]]])
