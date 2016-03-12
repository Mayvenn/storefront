(ns storefront.components.stylist.stats
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn stylist-dashboard-stats-component [data owner]
  (om/component
   (html
    [:div.p1.bg-teal.bg-lighten-top-3.white.center.sans-serif
     [:div.h4.mt3 "LIFETIME COMMISSIONS"]
     [:div.p3.h00 (f/as-money-without-cents (get-in data keypaths/stylist-commissions-paid-total))]
     (letfn [(circle [idx selected]
               [:div.bg-white.circle
                {:class (str
                         (when-not (zero? idx) "ml2") " "
                         (when-not (= idx selected) "bg-lighten-4"))
                 :style {:width "8px" :height "8px"}}])]
       [:div.flex.justify-center.p1.mt2
        (circle 0 1)
        (circle 1 1)
        (circle 2 1)])])))
