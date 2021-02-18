;; TODO: consider renaming now that it isn't used to add services
(ns stylist-profile.ui.specialties-shopping
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent specialties-shopping-specialty-organism
  [{:keys [title content]} _ {:keys [id]}]
  [:div.flex.flex-auto.justify-between.border-bottom.border-cool-gray.py2
   {:key id}
   [:div
    [:div
     [:span title]
     ui/nbsp]
    [:div.dark-gray.content-3.mr6 content]]])

(defn specialties-shopping-title-molecule
  [{:specialties-shopping.title/keys [id primary]}]
  (c/html
   [:div.title-3.proxima.shout
    {:data-test id
     :key       id}
    primary]))

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.mt4
     (specialties-shopping-title-molecule data)
     (c/elements specialties-shopping-specialty-organism data
                 :specialties-shopping/specialties)]))
