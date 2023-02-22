(ns catalog.ui.shop-these-looks
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn entry [ix src copy target args highlight?]
  [:a.grid.shop-these-looks-entry
   (merge (utils/route-to target args)
          {:key   ix
           :style {:grid-template-rows "auto 100px"
                   :justify-items      "center"
                   :align-items        "center"}
           :class (str "shop-these-looks-" (if highlight? "highlight" "lowlight"))})
   (ui/img {:src   src
            :class "container-size"
            :alt   ""
            :style {:grid-row    "1 / 3"
                    :grid-column "1 / 2"
                    :object-fit  "cover"}})
   [:div
    {:style {:max-width   "170px"
             :grid-row    "2 / 3"
             :grid-column "1 / 2"}}
    [:div.btn-medium.btn-outline.button-font-1.shout copy]]])

(c/defcomponent organism
  [{:shop-these-looks/keys [row-1 row-2]} _ _]
  [:div.my10.max-1080.mx-auto
   [:h2.title-1.canela.center.mb4 "Get Inspired by Luxe Looks"]
   [:div.shop-these-looks
    [:div.shop-these-looks-spacer]
    (map-indexed (fn [ix e]
                   (entry ix
                          (:shop-these-looks.entry.img/src e)
                          (:shop-these-looks.entry.cta/copy e)
                          (:shop-these-looks.entry.cta/target e)
                          (:shop-these-looks.entry.cta/args e)
                          true))
                 row-1)
    (map-indexed (fn [ix e]
                   (entry ix
                          (:shop-these-looks.entry.img/src e)
                          (:shop-these-looks.entry.cta/copy e)
                          (:shop-these-looks.entry.cta/target e)
                          (:shop-these-looks.entry.cta/args e)
                          false))
                 row-2)
    [:div.shop-these-looks-spacer]]])
