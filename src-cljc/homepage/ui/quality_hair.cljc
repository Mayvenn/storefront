(ns homepage.ui.quality-hair
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private quality-hair-cta-molecule
  [{:quality-hair.cta/keys [target label id]}]
  [:div.pt3
   (ui/button-medium-underline-primary
    (assoc (apply utils/route-to target)
           :data-test id
           :data-ref  id)
    label)])

(defn ^:private quality-hair-body-molecule
  [{:quality-hair.body/keys [primary]}]
  [:div.title-2.canela primary])

(defn ^:private quality-hair-title-molecule
  [{:quality-hair.title/keys [primary secondary]}]
  [:div.py1.shout
   [:div.title-1.proxima
    {:style {:font-size "19px"}}
    primary]
   [:div.title-1.canela.mt2.mb4
    {:style {:font-size "72px"}}
    secondary]])

(c/defcomponent organism
  [data _ _]
  [:div.col-6-on-dt.col-9.mx-auto ;; .my5.py4
   [:div.col-9-on-dt.mx-auto.flex.flex-column.center.left-align-on-dt.myj3
    (quality-hair-title-molecule data)
    (quality-hair-body-molecule data)
    (quality-hair-cta-molecule data)]])
