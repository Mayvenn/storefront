(ns checkout.ui.empty-cart
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private empty-cart-action-molecule
  [{:empty-cart.action/keys [id label target]}]
  (when (and id label target)
    (ui/button-large-primary
     (merge {:data-test id} (apply utils/route-to target))
     [:div.flex.items-center.justify-center.inherit-color label])))

(defn ^:private empty-cart-body-molecule
  [{:empty-cart.body/keys [primary]}]
  [:div.col-8.mx-auto.mt2.mb6 primary])

(defn ^:private empty-cart-title-molecule
  [{:empty-cart.title/keys [primary]}]
  [:h1.canela.title-1.mb4 primary])

(c/defcomponent organism
  [data _ _]
  (ui/narrow-container
   [:div.center {:data-test "empty-cart"}
    [:div {:style {:margin-top "70px"}}
     (empty-cart-title-molecule data)
     (empty-cart-body-molecule data)]
    [:div.col-9.mx-auto
     (empty-cart-action-molecule data)]]))
