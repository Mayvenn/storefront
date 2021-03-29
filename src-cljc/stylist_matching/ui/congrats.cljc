(ns stylist-matching.ui.congrats
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

(def ^:private congrats-heart-atom
  (svg/heart {:class  "fill-p-color"
              :width  "41px"
              :height "37px"}))

(defn ^:private congrats-title-molecule
  [{:stylist-matching.ui.congrats.title/keys [primary secondary]}]
  [:div.center
   [:div.canela.title-1 primary]
   [:div.proxima.content-2.mtj1
    (for [[idx line] (map-indexed vector secondary)]
      [:div
       {:key (str "secondary-" idx)}
       line])]])

(defn ^:private congrats-cta-molecule
  [{:stylist-matching.ui.congrats.cta/keys [id label target]}]
  (when (and id label target)
    (ui/button-medium-primary
     (merge {:data-test id} (apply utils/route-to target))
     label)))

(c/defcomponent organism
  [data _ _]
  ;; overflow to avoid margin collapsing
  [:div.ptj3.flex.flex-column.items-center.center
   [:div.ptj3.pbj2 congrats-heart-atom]
   [:div.col-9
    (congrats-title-molecule data)]
   [:div.col-8.pt3.ptj3
    (congrats-cta-molecule data)]])
