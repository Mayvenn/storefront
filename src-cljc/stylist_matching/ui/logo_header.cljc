(ns stylist-matching.ui.logo-header
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]))

(defn logo-header-logo-molecule
  [{:logo-header.logo/keys [id]}]
  (when id
    (component/html
     [:div.mt2.mr4
      (ui/ucare-img {:width "140"} "1970d88b-3798-4914-8a91-74288b09cc77")])))

(defn organism
  [data _ _]
  (component/create
   [:div#header
    [:div.flex.items-center.justify-center
     (logo-header-logo-molecule data)]]))
