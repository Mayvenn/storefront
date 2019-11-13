;; GROT: Replace with new header stylist-matching.ui.header
(ns adventure.components.header
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn calculate-width [number-of-steps]
  (reduce
   (fn [acc _]
     (let [new-fill (* (- 100 acc) (/ 1 8.0))]
       (+ acc new-fill)))
   1
   (range 1 (inc number-of-steps))))

;; TODO: Deal with nil progress here instead of in component
(defn progress-bar [steps-completed]
  [:div.col-12.col
   {:style {:height "6px"}}
   [:div.bg-purple
    {:style {:width (str (calculate-width steps-completed) "%")
             :height "6px"}}]])

(defn shopping-bag []
  {:id    "mobile-cart"
   :opts  (utils/route-to events/navigate-cart)
   :value (ui/ucare-img
           {:width "20"}
           "02f9e7fb-510f-458e-8be7-090399aad4de")})

(defcomponent component
  [{:keys [back-navigation-message
           header-attrs
           progress
           right-corner
           shopping-bag?
           subtitle
           title
           logo?
           cold-load-nav-message
           unstick?]
    :or   {shopping-bag? true}} _ _]
  (let [right-corner (cond (seq right-corner) right-corner
                           shopping-bag?      (shopping-bag)
                           :else              nil)
        container    (if unstick? :div#header :div#header.absolute.top-0.left-0.right-0.center)]
    [container
     header-attrs
     (if progress
       (progress-bar (dec progress))
       [:div {:style {:height "6px"}}])
     [:div.flex.items-center.justify-between ;; Buttons (cart and back)
      {:style {:height "66px"}}
      [:div
       (if back-navigation-message
         [:a.block.p3.inherit-color
          (merge {:data-test "adventure-back"}
                 (if cold-load-nav-message
                   (apply utils/route-to cold-load-nav-message)
                   (utils/route-back {:navigation-message back-navigation-message})))
          [:div.flex.items-center.justify-center {:style {:height "24px" :width "20px"}}
           (ui/back-arrow {:width "14"})]]
         [:div {:style {:width "50px"}}])]
      (if logo?
        [:div.mx-auto
         (ui/ucare-img {:width "140"} "1970d88b-3798-4914-8a91-74288b09cc77")]
        [:div.mx-auto
         [:div.h6.medium title]
         [:div.h8 subtitle]])
      [:div
       (if right-corner
         (let [{:keys [id opts value]} right-corner]
           [:a.block.p3.flex.items-center
            (merge {:data-test id} opts)
            (when id value)])
         [:div {:style {:width "50px"}}])]]]))

(defn built-component
  [data opts]
  (component/build component data opts))
