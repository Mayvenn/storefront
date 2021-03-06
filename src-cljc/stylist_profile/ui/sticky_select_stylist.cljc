(ns stylist-profile.ui.sticky-select-stylist
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn sticky-select-stylist-cta-molecule
  [{:sticky-select-stylist.cta/keys [id label target]}]
  (when (and id label target)
    (ui/button-medium-primary
     (merge {:data-test id}
            (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color.button-font-2
      label])))

(c/defcomponent organism
  ;; TODO(corey) find a way to implement without dup responsive buttons
  [data _ _]
  [:div
   [:div.hide-on-mb
    [:div.bg-pale-purple.fixed.max-580.border-none.col-12.bottom-0.py3
     [:div.flex.justify-center.my4
      (sticky-select-stylist-cta-molecule
       (update data :sticky-select-stylist.cta/id str "-desktop"))]]]
   [:div.hide-on-tb-dt
    [:div.bg-pale-purple.col-12.fixed.bottom-0.center.mtj3
     [:div.flex.justify-center.my4
      (sticky-select-stylist-cta-molecule data)]]]])
