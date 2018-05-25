(ns install.licensed-stylists
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [goog.events.EventType :as EventType]])
            [spice.core :as spice]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.messages :as messages]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]))

(defn ^:private stylist-slide [{:keys [stylist-headshot]}]
  [:div.flex.relative.justify-center.mxn2
   [:div.z5.stacking-context.absolute
    {:style {:margin-top  "14px"
             :margin-left "92px"}}
    (ui/ucare-img {:width "40"} "3cd2b6e9-8470-44c2-ad1f-b1e182d38cb0")]
   (ui/ucare-img {:width "210"} stylist-headshot)])

(defn ^:private stylist-slides [stylists]
  (map stylist-slide stylists))

(defn ^:private stylist-info [{:keys [stylist-name salon-name salon-address stylist-bio gallery]}]
  [:div.py2
   [:div.h3 stylist-name]
   [:div.teal.h6.pt2.bold
    "MAYVENN CERTIFIED STYLIST"]
   [:div.h6.bold.dark-gray salon-name]
   [:div.h6.bold.dark-gray salon-address]

   [:div.h6.dark-gray.pt3 stylist-bio]
   [:div
    [:a.teal.medium.h6.border-teal.border-bottom.border-width-2 "View Hair Gallery"]
    #_[:div (-> gallery :images first :ucare-id str)]]])

(defn stylist-details-before-change [prev next]
  (messages/handle-message events/carousel-certified-stylist-slide))

(defn stylist-details-after-change [index]
  (messages/handle-message events/carousel-certified-stylist-index {:index index}))

(defn component
  [{:keys [stylists carousel-certified-stylist-index carousel-certified-stylist-sliding?]} owner opts]
  (component/create
   [:div
    [:div.center.pt6.px6
     [:div.pt4.teal.letter-spacing-6.bold.h6 "LICENSED STYLISTS"]
     [:div.h2 "Mayvenn Certified Stylists"]
     [:div.h6.pt2 "We have partnered with a select group of experienced and licensed stylists in Fayetteville, NC to give you a FREE high quality standard install."]]

    [:div.pt6
     [:div.container
      (component/build carousel/component
                       {:slides   (stylist-slides stylists)
                        :settings {:swipe        true
                                   :arrows       true
                                   :dots         false
                                   :slidesToShow 1
                                   :centerMode   true
                                   :infinite     true
                                   :className    "faded-inactive-slides-carousel"
                                   :beforeChange stylist-details-before-change
                                   :afterChange  stylist-details-after-change}}
                       {})]
     [:div.center.px6
      [:div {:class (if carousel-certified-stylist-sliding? "transition-2 transparent" "transition-1 opaque")}
       (stylist-info (get stylists carousel-certified-stylist-index))]]]]))

(defmethod transitions/transition-state events/carousel-certified-stylist-slide [_ _event _args app-state]
  (assoc-in app-state keypaths/carousel-certified-stylist-sliding? true))

(defmethod transitions/transition-state events/carousel-certified-stylist-index [_ _event {:keys [index]} app-state]
  (-> app-state
      (assoc-in keypaths/carousel-certified-stylist-index index)
      (update-in keypaths/carousel dissoc :certified-stylist-sliding?)))




