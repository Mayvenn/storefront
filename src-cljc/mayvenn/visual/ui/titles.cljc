(ns mayvenn.visual.ui.titles
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(defn proxima
  "Usages:
  - call out boxes"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.center
    (when icon
      (svg/symbolic->html icon))
    [:div.title-2.proxima.shout
     (when id
       {:data-test id})
     primary]
    (when secondary
      [:div.mt2.content-2
       secondary])]))

(defn proxima-left
  "Usages:
  - stylist cards"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.left-align
    (when icon
      (svg/symbolic->html icon))
    [:div.title-2.proxima.shout
     (when id
       {:data-test id})
     primary]
    (when secondary
      [:div.mt2.content-2
       secondary])]))

(defn canela
  [{:keys [icon primary secondary target]}]
  (c/html
   [:div.center
    (svg/symbolic->html icon)
    [:div.title-2.canela primary]
    [:div.mt2.content-3 secondary]]))
