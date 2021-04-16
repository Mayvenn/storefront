(ns mayvenn.visual.ui.titles
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(defn proxima
  [{:keys [icon primary secondary target]}]
  (c/html
   [:div.center
    (svg/symbolic->html icon)
    [:div.title-2.proxima.shout primary]
    [:div.mt2.content-2 secondary]]))

(defn canela
  [{:keys [icon primary secondary target]}]
  (c/html
   [:div.center
    (svg/symbolic->html icon)
    [:div.title-2.canela primary]
    [:div.mt2.content-3 secondary]]))
