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
   [:div.bg-purple
    {:style {:width (str (calculate-width steps-completed)"%")
             :height "6px"}}]])

(defn component
  [{:keys [height current-step back-link title subtitle shopping-bag?]} _ _]
  (component/create
   [:div.absolute.top-0.left-0.right-0
    [:div.flex.flex-column
     (progress-bar (dec current-step))

     [:div.flex.items-center
      {:style {:height "65px"}}
      [:a.col-1.pl3.inherit-color
       (merge {:style {:height "30px"}
               :data-test "adventure-back"}
              (if (map? back-link)
                (utils/route-to (:event back-link) (:args back-link))
                (utils/route-to back-link)))
       (ui/back-arrow {:width "14"})]
      [:div.flex-auto.center
       [:div.h6.bold title]
       [:div.h8 subtitle]]
      [:div.col-1
       {:style {:height "46px"}}

       (when shopping-bag?
         [:a (merge {:data-test "adventure-cart"}
                    (utils/route-to events/navigate-cart))
          (ui/ucare-img
           {:width "20"}
           "02f9e7fb-510f-458e-8be7-090399aad4de")])]]]]))

(defn built-component
  [data opts]
  (component/build component data opts))
