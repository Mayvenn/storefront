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

(defcomponent appointments-component
  [{:stylist-gallery-appointments/keys []} _ _]
  [:div.m2.content-3
   (svg/exclamation-circle {:class "mx1" :height "10px"
                            :width "10px"})
   "Click below to add photos to your past appointments!"])

(defcomponent template
  [{:stylist-gallery-appointments/keys [id no-appts] :as data} _ opts]
  (when id
    [:div
     {:key id}
     (if no-appts
       (component/build no-appointments-component data opts)
       (component/build appointments-component data opts))]))
