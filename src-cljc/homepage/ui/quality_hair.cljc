(ns homepage.ui.quality-hair
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private quality-hair-cta-molecule
  [{:quality-hair.cta/keys [target label id]}]
  [:div.pt3
   (ui/button-small-underline-primary
    (assoc (apply utils/route-to target)
           :data-test  id
           :data-ref id)
    label)])

(defn ^:private quality-hair-body-atom
  [{:quality-hair.body/keys [primary]}]
  [:div.col-9.mx-auto.title-2.canela primary])

(defn ^:private quality-hair-title-molecule
  [{:quality-hair.title/keys [primary secondary]}]
  [:div.title-1.canela
   [:div.py1.shout
    [:div.title-1.proxima {:style {:font-size "19px"}} primary]
    [:div.canela.mt2.mb4 {:style {:font-size "72px"}} secondary]]])

(c/defcomponent organism
  [data _ _]
  [:div.col-6-on-dt
   [:div.center.my5.pt4
    (quality-hair-title-molecule data)
    (quality-hair-body-atom data)
    (quality-hair-cta-molecule data)]])
