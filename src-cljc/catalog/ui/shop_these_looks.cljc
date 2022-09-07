(ns catalog.ui.shop-these-looks
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]))

(defn entry [src copy target]
  [:div.col-12.m2.grid
   {:style {:grid-template-rows "auto 70px"
            :justify-items      "center"}}
   (ui/img {:src   src
            :class "container-size"
            :style {:grid-row    "1 / 3"
                    :grid-column "1 / 2"
                    :object-fit  "cover"}})
   [:div
    {:style {:max-width   "170px"
             :grid-row    "2 / 3"
             :grid-column "1 / 2"}}
    (ui/button-medium-secondary {} copy)]])

(c/defcomponent organism
  [{:shop-these-looks/keys [row-1 row-2]} _ _]
  [:div.hide-on-mb.my10.max-1080.mx-auto
   [:div.title-1.canela.center.mb4 "Shop these Looks"]
   [:div.flex
    {:style {:height "600px"}}
    (for [e row-1]
      (entry (:shop-these-looks.entry.img/src e)
             (:shop-these-looks.entry.cta/copy e)
             (:shop-these-looks.entry.cta/target e)))]
   [:div.flex
    {:style {:height "300px"}}
    (for [e row-2]
      (entry (:shop-these-looks.entry.img/src e)
             (:shop-these-looks.entry.cta/copy e)
             (:shop-these-looks.entry.cta/target e)))]]
  )
