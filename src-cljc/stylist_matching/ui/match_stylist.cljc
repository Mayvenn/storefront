(ns stylist-matching.ui.match-stylist
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn match-stylist-button-molecule
  [{:match-stylist.button/keys [id label target]}]
  (when id
    (component/html
     (ui/white-button
      (merge {:style     {:border-radius "3px"}
              :class     "my1 col-12"
              :key       id
              :data-test id}
             (apply utils/route-to target))
      [:div.flex.items-center.justify-between
       [:div.col-2]
       [:div.flex.items-center.justify-center.p3 label]
       [:div.col-2.p2.right-align (ui/forward-caret {:width 16 :height 16 :color "gray"})]] ))))

(defn match-stylist-title-molecule
  [{:match-stylist.title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.center
      [:div.h1.mb2.mt1.light primary]
      [:div.h5.my2.light secondary]])))

(defn organism
  [data _ _]
  (component/create
   [:div.m5.flex.flex-column.flex-auto.items-center.justify-between.mt6
    [:div.col-10
     (match-stylist-title-molecule data)]
    [:div.col-12
     (match-stylist-button-molecule data)]]))
