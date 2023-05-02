(ns catalog.ui.pre-accordion
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils] ))

(defn block
  [icon primary content]
  [:div.flex.proxima.my2
   {:key primary}
   [:div
    (svg/symbolic->html [icon {:style {:height "22px"
                                       :width  "22px"}
                               :class "fill-p-color mr1"}])]
   [:div.text-xs.flex-auto
    [:div.bold primary]
    (when (seq content)
      (map-indexed (fn [ix c] [:div {:key (str primary " " ix)} c]) 
                   content))]])
   
(c/defcomponent component
  "A light purple panel for introducing an accordion."
  [{:keys [primary blocks-left blocks-right link-target link-text]} _ _]
  (when primary
    [:div.bg-pale-purple.p4.proxima
     [:div.text-base primary]
     [:div.flex
      (for [[id side] [["l" blocks-left] ["r" blocks-right]]]
        [:div.col-6 {:key id}
         (for [{:keys [icon primary content]} side]
           (block icon primary content))])]
     (ui/button-small-underline-primary
      (apply utils/fake-href link-target)
      link-text)]) )











