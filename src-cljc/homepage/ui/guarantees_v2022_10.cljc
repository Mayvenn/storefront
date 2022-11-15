(ns homepage.ui.guarantees-v2022-10
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(defn ^:private icon-list-item
  [id {:guarantees.icon/keys [title symbol]}]
  (c/html
   [:div.flex.flex-column-on-mb.items-center.gap-1
    {:key id}
    (svg/symbolic->html [symbol {:style {:max-width "24px"
                                         :max-height "24px"}
                                 #_#_:class "container-size"}])
    [:div.shout title]]))

(c/defcomponent organism
  [data _ _]
  (into [:div.flex.p2.center.bg-cool-gray.gap-2.justify-around.content-4-on-mb]
        (map-indexed icon-list-item (:list/icons data))))
