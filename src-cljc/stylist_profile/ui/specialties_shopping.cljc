(ns stylist-profile.ui.specialties-shopping
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent specialties-shopping-specialty-organism
  [{:keys [title subtitle content cta-label disabled? cta-target]} _ {:keys [id]}]
  [:div.flex.flex-auto.justify-between.border-bottom.border-cool-gray.py2
   {:key id}
   [:div
    [:div
     [:span title]
     ui/nbsp
     [:span.dark-gray.shout.content-3 subtitle]]
    [:div.dark-gray.content-3.mr6 content]]
   [:div.my1
    (ui/button-small-secondary {:disabled? disabled?
                                :on-click  (apply utils/send-event-callback cta-target)
                                :data-test id}
                               cta-label)]])

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
