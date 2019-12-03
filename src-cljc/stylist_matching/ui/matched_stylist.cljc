(ns stylist-matching.ui.matched-stylist
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [stylist-matching.ui.stylist-cards :as stylist-cards]))

(defn matched-stylist-title-molecule
  [{:matched-stylist.title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.center {:data-test id}
      [:div.pb4.h3.bold primary]
      [:div.h5.line-height-3.center secondary]])))

(defn matched-stylist-card-cta-molecule
  [{:matched-stylist.cta-title/keys [id primary secondary label target]}]
  (when id
    (component/html
     [:div.py2
      [:h3.bold primary]
      [:div.my3 secondary]
      (ui/p-color-button
       {:data-test id
        :class     "bold"
        :href      target}
       label)])))

(defcomponent organism
  [data _ _]
  [:div.col-10.mx-auto
   {:key (:react/key data)}
   (matched-stylist-title-molecule data)
   [:div.bg-white.rounded.left-align.black.my3
    (stylist-cards/control-stylist-card-header-molecule data)]
   (matched-stylist-card-cta-molecule data)])
