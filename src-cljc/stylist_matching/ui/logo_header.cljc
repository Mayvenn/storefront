(ns stylist-matching.ui.logo-header
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            
            ))

(defn logo-header-logo-molecule
  [{:logo-header.logo/keys [id]}]
  (when id
    (component/html
     [:a.mt2.mr4
      (merge (utils/route-to events/navigate-home)
             {:data-test id})
      (ui/ucare-img {:width "140"} "1970d88b-3798-4914-8a91-74288b09cc77")])))

(defcomponent organism
  [data _ _]
  [:div#header
    [:div.flex.items-center.justify-center
     (logo-header-logo-molecule data)]])
