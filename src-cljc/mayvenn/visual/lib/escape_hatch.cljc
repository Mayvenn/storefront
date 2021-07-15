(ns mayvenn.visual.lib.escape-hatch
  (:require [storefront.component :as c]
            [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.dividers :as dividers]
            [mayvenn.visual.ui.actions :as actions]))

(c/defcomponent variation-1
  [data _ _]
  [:div
   [:div
    {:style {:min-height "140px"}}]
   [:div.bg-white.absolute.bottom-0.left-0.right-0
    dividers/green
    [:div.flex.flex-column.items-center.py5
     ;; Title/primary here is a nonstandard ui element
     ;; TODO(corey) discover what this is and name it
     [:div.center
      (:title/primary data)]
     [:div.col-5.flex.justify-center.items-center.py1
      (actions/small-secondary (with :action data))]]]])
