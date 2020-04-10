(ns homepage.ui.hair-quality
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private hair-high-cta-molecule
  [{:hair-high.cta/keys [target label id]}]
  [:div.pt3
   (ui/button-small-underline-primary
    (assoc (apply utils/route-to target)
           :data-test  id
           :data-ref id)
    label)])

(defn ^:private hair-high-body-atom
  [{:hair-high.body/keys [primary]}]
  [:div.col-9.mx-auto.title-2.canela primary])

(defn ^:private hair-high-title-molecule
  [{:hair-high.title/keys [primary secondary]}]
  [:div.title-1.canela
   [:div.py1.shout
    [:div.title-1.proxima {:style {:font-size "19px"}} primary]
    [:div.canela.mt2.mb4 {:style {:font-size "72px"}} secondary]]])

(c/defcomponent organism
  [data _ _]
  [:div
   [:div.center.col-6-on-dt.mx-auto.my5.pt4
    (hair-high-title-molecule data)
    (hair-high-body-atom data)
    (hair-high-cta-molecule data)]])
