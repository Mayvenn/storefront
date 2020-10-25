(ns stylist-profile.ui.specialties-list
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(c/defcomponent specialty-list-specialty-organism
  [{:specialties-list.specialty.title/keys [primary]} _ {:keys [id]}]
  [:div.col-6.h6.flex.items-center.content-2
   {:key id}
   (svg/check-mark {:class "black mr2"
                    :style {:width  12
                            :height 12}})
   primary])

(defn specialties-list-title-molecule
  [{:specialties-list.title/keys [id primary]}]
  (c/html
   [:div.title-3.proxima.shout
    {:data-test id
     :key       id}
    primary]))

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.mt4
     (specialties-list-title-molecule data)
     [:div.flex.flex-wrap
      (c/elements specialty-list-specialty-organism data
                  :specialties-list/specialties)]]))
