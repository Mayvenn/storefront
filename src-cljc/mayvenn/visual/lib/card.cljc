(ns mayvenn.visual.lib.card
  (:require [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]))

(c/defcomponent look-suggestion-1
  [{:as   data
    :keys [id index-label ucare-id primary secondary tertiary tertiary-note]} _ _]
  [:div.left-align.px3.mt5.mb3
   [:div.shout.proxima.title-3.mb1 index-label]
   [:div.bg-white
    [:div.flex.p3
     [:div.mr4
      (ui/img {:src ucare-id :width "80px"})]
     [:div.flex.flex-column
      [:div primary]
      [:div secondary]
      [:div.content-1 tertiary [:span.ml2.s-color.content-2 tertiary-note]]
      (actions/small-primary (with :action data))]]]])
