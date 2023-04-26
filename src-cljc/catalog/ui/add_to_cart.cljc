(ns catalog.ui.add-to-cart
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

;; Why does clicking this button cause a "Each child in a list
;; should have a unique key prop." warning?
(defn cta-molecule
  [{:cta/keys [id label target spinning? disabled?]}]
  (when (and id label target)
    (ui/button-large-primary
     (merge
      {:data-test id
       :key       id
       :spinning? (boolean spinning?)
       :disabled? (boolean disabled?)}
      #?(:clj {:disabled? true})
      (apply utils/fake-href target))
     label)))

(defn sub-cta-molecule
  [{:sub-cta/keys [promises learn-more-copy learn-more-target]}]
  [:div.grid.gap-3.my6
   {:style {:grid-template-columns "25px auto"}}
   (concat
    (map-indexed
     (fn [ix {:keys [icon copy promise-target promise-target-copy]}]
       [(svg/symbolic->html [icon {:key   (str "icon-" ix)
                                   :style {:grid-column "1 / 2"
                                           :height      "20px"}
                                   :class "fill-p-color col-12"}])
        [:div
         {:key (str "copy-" ix)}
         [:div
          {:style {:grid-column "2 / 3"}}
          copy]
         (when promise-target
           (ui/button-small-underline-black
            (merge
             (apply utils/route-to promise-target)
             {:style {:grid-column "2 / 3"}})
            promise-target-copy))]])
     promises))
   (ui/button-small-underline-primary
    (merge
     (apply utils/route-to learn-more-target)
     {:style {:grid-column "2 / 3"}})
    learn-more-copy)])

(defn shipping-delay-hack
  [{:shipping-delay/keys [show?]}]
  (when show?
    [:div.bg-warning-yellow.border.border-warning-yellow.mx3.my5
     [:div.bg-lighten-4.p3
      [:span.bold "Shipping Delay: "]
      [:span "There is a slight delay in shipping for this product. Ships by Monday (5/1). We apologize for any inconvenience."]]]))

(defcomponent organism
  "Add to Cart organism"
  [data _ _]
  [:div
   [:div.px3.my1
    (cta-molecule data)]
   [:div
    (shipping-delay-hack data)]
   [:div.px5.my1
    (sub-cta-molecule data)]])
