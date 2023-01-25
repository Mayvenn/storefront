(ns homepage.ui.promises
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(defn ^:private icon-list-item
  [id {:promises.icon/keys [title symbol]}]
  (c/html
   [:div.flex.flex-column-on-mb.items-center.gap-1.justify-center-on-dt
    {:key id
     :style {:flex "1 1 0"}}
    (case (namespace symbol)
      "svg" (svg/symbolic->html [symbol {:style {:max-width  "24px"
                                                 :max-height "24px"}}]))
    [:div.shout title]]))

(c/defcomponent organism
  [data _ _]
  [:div.flex.p2.center.bg-cool-gray.gap-2.content-4-on-mb
   (map-indexed icon-list-item (:list/icons data))])
