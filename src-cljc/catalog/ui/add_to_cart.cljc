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
    (for [{:keys [icon copy]} promises]
      [(svg/symbolic->html [icon {:style {:grid-column "1 / 2"
                                          :height      "20px"}
                                  :class "fill-p-color col-12"}])
       [:div
        {:style {:grid-column "2 / 3"}}
        copy]]))
   (ui/button-small-underline-primary
    (merge
     (apply utils/route-to learn-more-target)
     {:style {:grid-column "2 / 3"}})
    learn-more-copy)])

(defn add-to-cart-quadpay-molecule
  [{:add-to-cart.quadpay/keys [price loaded?]}]
  #?(:cljs
     [:div
      (component/build quadpay/component
                       {:quadpay/order-total price
                        :quadpay/show?       loaded?
                        :quadpay/directive   :just-select}
                       nil)]))

(defcomponent organism
  "Add to Cart organism"
  [data _ _]
  [:div
   [:div.px3.my1
    (cta-molecule data)]
   [:div.px5.my1
    (sub-cta-molecule data)
    (add-to-cart-quadpay-molecule data)]])
