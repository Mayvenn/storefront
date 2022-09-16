(ns catalog.ui.add-to-cart
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
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
   [:div.px3.py1
    (cta-molecule data)
    (add-to-cart-quadpay-molecule data)]])
