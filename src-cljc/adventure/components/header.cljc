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
     (let [new-fill (* (- 100 acc) (/ 1 8.0))]
       (+ acc new-fill)))
          1
          (range 1 (inc number-of-steps))))

(defn progress-bar [steps-completed]
  [:div.col-12.col
   {:style {:height "6px"}}
   [:div.bg-purple
    {:style {:width (str (calculate-width steps-completed)"%")
             :height "6px"}}]])

(defn component
  [{:keys [header-attrs progress back-navigation-message title subtitle shopping-bag?]} _ _]
  (component/create
   [:div.absolute.top-0.left-0.right-0
    header-attrs
    [:div.flex.flex-column
     (progress-bar (dec progress))
     [:div.relative.mt1
      {:style {:height "59px"}}
      [:div.absolute.left-0.right-0.top-0.flex.items-center.justify-between ;; Buttons (cart and back)
       [:a.block.p3.inherit-color
        (merge {:data-test "adventure-back"}
               (utils/route-back {:navigation-message back-navigation-message}))
        [:div.flex.items-center.justify-center {:style {:height "24px" :width "20px"}}
         (ui/back-arrow {:width "14"})]]
       (if shopping-bag?
         [:a.block.p3 (merge {:data-test "adventure-cart"}
                             (utils/route-to events/navigate-cart))
          (ui/ucare-img
           {:width "20"}
           "02f9e7fb-510f-458e-8be7-090399aad4de")]
         [:div])]
      [:div.center.mt1
       [:div.h6.medium title]
       [:div.h8 subtitle]]]]]))

(defn built-component
  [data opts]
  (component/build component data opts))
