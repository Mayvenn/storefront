(ns stylist-matching.ui.matched-stylist
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
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
      (ui/button-large-primary
       {:data-test id
        :class     "bold"
        :href      target}
       label)])))

(defn stylist-card-address-marker-molecule
  [{:stylist-card.address-marker/keys [id value]}]
  (when id
    (component/html
     [:div.h7.col-12.flex.items-center
      value])))

(defn stylist-card-title-molecule
  [{:stylist-card.title/keys [id primary]}]
  (when id
    (component/html
     [:div.h3.line-height-1.light
      {:data-test id}
      primary])))

(defn stylist-card-thumbnail-molecule
  "We want ucare-ids here but we do not have them"
  [{:stylist-card.thumbnail/keys [id ucare-id] :as data}]
  (when id
    (component/html
     (ui/circle-picture {:width "104px"}
                        (ui/square-image {:resizable-url ucare-id}
                                         72)))))

(defn stylist-card-header-molecule
  [{:stylist-card/keys [target id] :as data}]
  (when id
    [:div.col-12.flex.items-start.p2
     (assoc (apply utils/route-to target) :data-test id)
     [:div.flex.justify-center.items-center
      (stylist-card-thumbnail-molecule data)]
     [:div.medium.px2
      (stylist-card-title-molecule data)
      [:span.h7.flex.items-center.pyp2
       (stylist-cards/stylist-card-stars-rating-molecule data)]
      (stylist-card-address-marker-molecule data)]]))

(defcomponent organism
  [data _ _]
  [:div.col-10.mx-auto
   {:key (:react/key data)}
   (matched-stylist-title-molecule data)
   [:div.bg-white.rounded.left-align.black.my3
    (stylist-card-header-molecule data)]
   (matched-stylist-card-cta-molecule data)])
