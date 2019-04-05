(ns adventure.stylist-matching.stylist-detail-line
  (:require [storefront.component :as component]
            [spice.date :as date]
            [storefront.components.ui :as ui]))

(defn component
  [{:keys [salon-description stylist-since-copy licensed] :as stylist-detail-line-data} ]
  [:div
   (into [:div.flex.flex-wrap]
         (comp
          (remove nil?)
          (interpose [:div.mxp3 "·"]))
         [(when licensed
            [:div "Licensed"])
          (when salon-description
            [:div salon-description])
          (when stylist-since-copy
            [:div
             stylist-since-copy])])])

(defn query
  [stylist]
  (let [salon-type (:salon-type (:salon stylist))]
    {:salon-type         salon-type
     :salon-description  (case salon-type
                           "salon"   "In-Salon"
                           "in-home" "In-Home"
                           nil)
     :stylist-since-copy (str (ui/pluralize-with-amount
                               (- (date/year (date/now)) (:stylist-since stylist))
                               "yr")
                              " Experience")
     :licensed           (:licensed stylist)}))
