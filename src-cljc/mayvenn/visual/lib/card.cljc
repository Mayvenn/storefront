(ns mayvenn.visual.lib.card
  (:require [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]))

(c/defcomponent look-suggestion-1
  [{:as data :quiz.result/keys [id index-label ucare-id primary secondary tertiary tertiary-note cta-label cta-target]} _ _]
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
      (actions/small-primary (with :action data))
      #_
      (ui/button-small-primary (merge {:data-test id
                                       :class     "mt2 col-8"}
                                      (apply utils/fake-href cta-target))
                               cta-label)]]]])
