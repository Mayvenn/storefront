(ns stylist-matching.ui.shopping-method-choice
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn shopping-method-choice-button-molecule
  [{:shopping-method-choice.button/keys [id target ucare-id label]}]
  (when id
    (component/html
     [:div.point.flex.items-center.justify-between.border.border-cool-gray.border-width-2
      (merge {:class     "my1 p2"
              :key       id
              :data-test id}
             (apply utils/route-to target))
      (ui/ucare-img {:width 60
                     :picture-classes "flex items-center"} ucare-id)
      [:div.flex-auto.left-align.p3 label]
      [:div.flex (ui/forward-caret {:width 14 :height 14 :color "black"})]])))

(defn buttons-list-molecule
  [{:list/keys [buttons]}]
  (when (seq buttons)
    (component/html
     [:div.mt1
      (for [button buttons]
        (shopping-method-choice-button-molecule button))])))

(defn shopping-method-choice-error-title-molecule
  [{:shopping-method-choice.error-title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.center.flex.flex-column.items-center
      [:div.h2.my2.light.col-10.p-color primary]
      [:div.h5.my2.light.col-10 secondary]])))

(defn shopping-method-choice-title-molecule
  [{:shopping-method-choice.title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.left-align
      [:div.title-1.canela.my2.light primary]
      [:div.h5.my4.light secondary]])))

(defcomponent organism
  [data _ _]
  [:div.m5.pt4.px1
   (shopping-method-choice-title-molecule data)
   (shopping-method-choice-error-title-molecule data)
   (buttons-list-molecule data)])
