(ns stylist-profile.ui.ratings-bar-chart
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]))

(c/defcomponent rating-bar-organism
  [{:ratings-bar-chart.bar/keys [primary secondary amount]} _ {:keys [id]}]
  (c/html
   [:div.flex.px8.items-center
    {:key id}
    [:div.flex.items-baseline
     [:div.bold.proxima.title-3 primary]
     [:div.px2
      (svg/whole-star {:class  "fill-gray"
                       :height "13px"
                       :width  "13px"})]]
    [:div.flex-grow-1.flex
     [:div.bg-s-color
      {:style {:width  amount
               :height "12px"}}
      ui/nbsp]
     [:div.bg-white
      {:style {:width  (str "calc(100% - " amount")")
               :height "12px"}}
      ui/nbsp]]
    [:div.proxima.content-3.pl2
     {:style {:width "20px"}}
     secondary]]))

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.bg-cool-gray.flex-column.center.py5.mt3
     [:a.block.shout.bold.proxima.title-3 {:name (:ratings-bar-chart/id data)} "Ratings"]
     (c/elements rating-bar-organism data
                 :ratings-bar-chart/bars)]))
