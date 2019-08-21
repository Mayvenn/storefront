(ns adventure.components.header
  (:require [storefront.component :as component]
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
    {:style {:width (str (calculate-width steps-completed)"%")
             :height "6px"}}]])

(defn shopping-bag []
  {:id    "adventure-cart"
   :opts  (utils/route-to events/navigate-cart)
   :value (ui/ucare-img
           {:width "20"}
           "02f9e7fb-510f-458e-8be7-090399aad4de")})

(defn component
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
  (component/create
   (let [right-corner (cond (seq right-corner) right-corner
                            shopping-bag?      (shopping-bag)
                            :else              nil)
         container    (if unstick? :div#header :div#header.absolute.top-0.left-0.right-0.center)]
     [container
      header-attrs
      [:div.flex.flex-column
       (if progress
         (progress-bar (dec progress))
         [:div {:style {:height "6px"}}])
       [:div.relative.mt1
        {:style {:height "59px"}}
        [:div.absolute.left-0.right-0.top-0.flex.items-center.justify-between ;; Buttons (cart and back)
         [:div
          (when back-navigation-message
            [:a.block.p3.inherit-color
             (merge {:data-test "adventure-back"}
                    (if cold-load-nav-message
                      (apply utils/route-to cold-load-nav-message)
                      (utils/route-back {:navigation-message back-navigation-message})))
             [:div.flex.items-center.justify-center {:style {:height "24px" :width "20px"}}
              (ui/back-arrow {:width "14"})]])]
         [:div
          (when-let [{:keys [id opts value]} right-corner]
            [:a.block.p3
             (merge {:data-test id} opts)
             (when id value)])]]
        [:div.flex
         (if logo?
           [:div.mr4.mx-auto
            (ui/ucare-img {:width "140"} "1970d88b-3798-4914-8a91-74288b09cc77")]
           [:div.mt1.mx-auto
            [:div.h6.medium title]
            [:div.h8 subtitle]])]]]])))

(defn built-component
  [data opts]
  (component/build component data opts))
