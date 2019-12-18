(ns stylist-matching.ui.match-stylist
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn match-stylist-button-molecule
  [{:match-stylist.button/keys [id label target]}]
  (when id
    (component/html
     (ui/button-large-primary
      (merge {:class     "col-12"
              :key       id
              :data-test id}
             (apply utils/route-to target))
      label))))

(defn match-stylist-title-molecule
  [{:match-stylist.title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.center
      [:div.title-1.canela.mb2.mt1 primary]
      [:div.content-2.proxima.my5 secondary]])))

(defcomponent organism
  [data _ _]
  [:div.m5.flex.flex-column.flex-auto.items-center.mt6
   [:div.col-10
    (match-stylist-title-molecule data)]
   [:div.col-12
    (match-stylist-button-molecule data)]])
