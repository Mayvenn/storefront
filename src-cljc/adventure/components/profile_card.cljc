(ns adventure.components.profile-card
  (:require [storefront.accessors.stylists :as stylists]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [spice.date :as date]))

(defn component [{:keys [image-url title subtitle rating detail-line detail-attributes]} _ _]
  (component/create
   [:div.flex.bg-white.px1.mxn2.rounded.py3
    ;; TODO: image-url should be format/auto?
    [:div.mr2 (ui/circle-picture {:width "104px"} image-url)]
    [:div.flex-grow-1.left-align.dark-gray.h7.line-height-4
     [:div.h3.black.line-height-1 title]
     [:div.pyp2 (ui/star-rating rating)]
     [:div.bold subtitle]
     [:div detail-line]
     [:div
      (into [:div.flex.flex-wrap]
            (comp
             (remove nil?)
             (map (fn [x] [:div x]))
             (interpose [:div.mxp3 "·"]))
            detail-attributes)]]]))

;; TODO: find a better place for this query function
(defn stylist-profile-card-data [stylist]
  (let [salon          (:salon stylist)]
    {:image-url         (-> stylist :portrait :resizable-url)
     :title             [:div {:data-test "stylist-name"}
                         (stylists/->display-name stylist {:full? true})]
     :subtitle          (str (:city salon) ", " (:state salon))
     :rating            (:rating stylist)
     :detail-line       (str (:name salon))
     :detail-attributes [(when (:licensed stylist)
                           "Licensed")
                         (case (:salon-type salon)
                           "salon"   "In-Salon"
                           "in-home" "In-Home"
                           nil)
                         (when (:stylist-since stylist)
                           (str (ui/pluralize-with-amount
                                 (- (date/year (date/now)) (:stylist-since stylist))
                                 "yr")
                                " Experience"))]}))
