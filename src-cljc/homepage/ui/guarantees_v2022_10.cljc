(ns homepage.ui.guarantees-v2022-10
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(defn ^:private icon-list-item
  [id {:guarantees.icon/keys [title body symbol]}]
  (c/html
   [:div.mx2.col-3.flex.flex-column-on-mb
    {:key id}
    [:div (svg/symbolic->html symbol)]
    [:div.shout title]
    #_(when body
      [:p body])]))

(c/defcomponent organism
  [data _ _]
  [:div.p2.center.bg-cool-gray
   (into [:div.col-8-on-dt.flex.mx-auto]
         (map-indexed icon-list-item (:list/icons data)))])
