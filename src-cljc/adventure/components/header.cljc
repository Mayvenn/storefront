(ns adventure.components.header
  (:require #?@(:cljs [[om.core :as om]])
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

(defn calculate-width [number-of-steps]
  (reduce
   (fn [acc _]
     (let [new-fill (* (- 100 acc) (/ 1 16.0))]
       (+ acc new-fill)))
          1
          (range 1 (inc number-of-steps))))

(defn progress-bar [steps-completed]
  [:div.col-12.col
   {:style {:height "6px"}}
   [:div.bg-aubergine
    {:style {:width (str (calculate-width steps-completed)"%")
             :height "6px"}}]])

(defn component
  [{:keys [current-step back-link title subtitle]} _ _]
  (component/create
   [:div.absolute.top-0.left-0.right-0
    [:div.flex.flex-column
     (progress-bar (dec current-step))

     [:div.flex.items-center
      {:style {:height "46px"}}
      [:a.col-1.pl3.inherit-color
       (utils/route-to back-link)
       (svg/left-caret {:class  "stroke-white"
                        :style  {:stroke-width "3"}
                        :height "1em"
                        :width  "1em"})]
      [:div.flex-auto.center
       [:div.h6 subtitle]
       [:div.h5.bold title]]
      [:div.col-1]]]]))

(defn built-component
  [data opts]
  (component/build component data opts))
