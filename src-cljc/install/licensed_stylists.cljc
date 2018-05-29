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
            [storefront.platform.component-utils :as utils]
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

(defn ^:private gallery-slide [ucare-id]
  [:div (ui/aspect-ratio 1 1
                         (ui/ucare-img {:class "col-12"} ucare-id))])

(defn ^:private gallery-slides [gallery]
  (map gallery-slide gallery))

(defn ^:private stylist-info [{:keys [stylist-name salon-name salon-address stylist-bio gallery]} stylist-gallery-open?]
  [:div.py2
   [:div.h3 stylist-name]
   [:div.teal.h6.pt2.bold
    "MAYVENN CERTIFIED STYLIST"]
   [:div.h6.bold.dark-gray salon-name]
   [:div.h6.bold.dark-gray salon-address]
   [:div.h6.dark-gray.pt3 stylist-bio]
   [:div
    [:a.teal.medium.h6.border-teal.border-bottom.border-width-2
     (utils/fake-href events/control-stylist-gallery-open)
     "View Hair Gallery"]

    (when stylist-gallery-open?
      (let [close-attrs (utils/fake-href events/control-stylist-gallery-close)]
        (ui/modal
         {:close-attrs close-attrs
          :col-class "col-12"}
         [:div.relative.mx-auto
          {:style {:max-width "750px"}}
          (component/build carousel/component
                           {:slides   (gallery-slides (:images gallery))
                            :settings {:slidesToShow 1}}
                           {})
          [:div.absolute
           {:style {:top "1.5rem" :right "1.5rem"}}
           (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                            :close-attrs close-attrs})]])))]])

(defn stylist-details-before-change [prev next]
  (messages/handle-message events/carousel-certified-stylist-slide))

(defn stylist-details-after-change [index]
  (messages/handle-message events/carousel-certified-stylist-index {:index index}))

(defn component
  [{:keys [stylists carousel-certified-stylist-index carousel-certified-stylist-sliding? stylist-gallery-open?]} owner opts]
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
       (stylist-info (get stylists carousel-certified-stylist-index)
                     stylist-gallery-open?)]]]]))

(defmethod transitions/transition-state events/carousel-certified-stylist-slide [_ _event _args app-state]
  (assoc-in app-state keypaths/carousel-certified-stylist-sliding? true))

(defmethod transitions/transition-state events/carousel-certified-stylist-index [_ _event {:keys [index]} app-state]
  (-> app-state
      (assoc-in keypaths/carousel-certified-stylist-index index)
      (update-in keypaths/carousel dissoc :certified-stylist-sliding?)))

(defmethod transitions/transition-state events/control-stylist-gallery-open [_ _event _args app-state]
  (assoc-in app-state keypaths/carousel-stylist-gallery-open? true))

(defmethod transitions/transition-state events/control-stylist-gallery-close [_ _event _args app-state]
  (update-in app-state keypaths/carousel-stylist-gallery dissoc :open?))
