(ns homepage.ui.quality-stylists
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private quality-stylists-cta-molecule
  [{:quality-stylists.cta/keys [target label id]}]
  [:div.pt3
   (ui/button-small-underline-primary
    (assoc (apply utils/route-to target)
           :data-test  id
           :data-ref id)
    label)])

(defn ^:private quality-stylists-body-atom
  [{:quality-stylists.body/keys [primary]}]
  [:div.col-9.mx-auto.title-2.canela primary])

(defn ^:private quality-stylists-title-molecule
  [{:quality-stylists.title/keys [primary secondary]}]
  [:div.title-1.canela
   [:div.py1.shout
    [:div.title-1.proxima {:style {:font-size "19px"}} primary]
    [:div.canela.mt2.mb4 {:style {:font-size "72px"}} secondary]]])

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.center.col-6-on-dt.mx-auto.my5.pt4
     (quality-stylists-title-molecule data)
     (quality-stylists-body-atom data)
     (quality-stylists-cta-molecule data)]))
