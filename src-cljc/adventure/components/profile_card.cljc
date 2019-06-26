(ns adventure.components.profile-card
  (:require [storefront.accessors.stylists :as stylists]
            [storefront.component :as component]
            [storefront.components.formatters :as formatters]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
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
                         (stylists/->display-name stylist)]
     :subtitle          (let [{:keys [name address-1 address-2 city state zipcode] :as salon} (:salon stylist)]
                          [:div.py1
                           [:div name]
                           [:a.navy
                            {:href (str "https://www.google.com/maps/place/"
                                        (string/join "+" (list address-1 address-2 city state zipcode)))}
                            (when address-1
                              [:div address-1
                               (when address-2
                                 [:span ", " address-2])])
                            [:div city ", " state " " zipcode]]])
     :rating            (:rating stylist)
     :detail-line       (ui/link :link/phone :a.navy {} (formatters/phone-number (:phone (:address stylist))))
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
