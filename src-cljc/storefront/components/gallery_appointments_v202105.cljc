(ns storefront.components.gallery-appointments-v202105
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defcomponent no-appointments-component
  [{:stylist-gallery-appointments/keys [target]} _ _]
  [:div.canela.title-2.my10.dark-gray.center.flex.flex-column.items-center
   "No appointments found"
   [:div.col-5.my2
    (ui/button-small-primary {:on-click (apply utils/send-event-callback target)}
                                  "Go to my gallery")]])

(defcomponent appointment-component
  [{:keys [id target title-primary title-secondary detail]} _ _]
  [:div.inline-block
   {:style {:margin "1px"}}
   [:div.bg-pale-purple.white.relative
    (merge {:key   id
            :style {:padding-top "100%"}}
           (utils/route-to target))
    [:div.absolute
     {:style {:top  "4px"
              :left "8px"}}
     [:div.shout.proxima.title-3.mbn2 title-secondary]
     [:div.canela.title-2 title-primary]]
    [:div.absolute.content-2
     {:style {:bottom "4px"
              :left   "8px"}}
     detail]
    [:div.absolute
     {:style {:top "50%"
              :left "50%"
              :transform "translate(-50%, -50%)"
              :font-size "xxx-large"}}
     "+"]]])

(defcomponent appointments-component
  [{:stylist-gallery-appointments/keys [appointments]} _ _]
  [:div.content-3
   [:div.m2.content-4
    (svg/exclamation-circle {:class "mx1" :height "10px"
                             :width "10px"})
    "Click below to add photos to your past appointments!"]
   [:div
    {:style {:display "grid"
             :grid-template-columns "auto auto auto"}}
    (for [appointment appointments]
      (component/build appointment-component appointment))]])

(defcomponent template
  [{:stylist-gallery-appointments/keys [id no-appts] :as data} _ opts]
  (when id
    [:div
     {:key id}
     (if no-appts
       (component/build no-appointments-component data opts)
       (component/build appointments-component data opts))]))
