(ns storefront.components.gallery-appointments-v202105
  (:require [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defcomponent no-appointments-component
  [{:keys [target]} _ _]
  [:div.canela.title-2.my10.dark-gray.center.flex.flex-column.items-center
   "No appointments found"
   [:div.col-5.my2
    (ui/button-small-primary {:on-click (apply utils/send-event-callback target)}
                                  "Go to my gallery")]])

(defcomponent appointments-component
  [data _ _]
  [:div.m2.content-3
   (svg/exclamation-circle {:class "mx1" :height "10px"
                            :width "10px"})
   "Click below to add photos to your past appointments!"])

(defcomponent template
  [{:keys [no-appts] :as data} _ opts]
  (if no-appts
    (component/build no-appointments-component data opts)
    (component/build appointments-component data opts)))

(defn query [state]
  (let [stylist-experience (get-in state keypaths/user-stylist-experience)]
    {:no-appts (not= "aladdin" stylist-experience)
     :target   [events/navigate-gallery-edit]}))
