(ns storefront.components.my-orders
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.components.formatters :refer [as-money locale-date]]
            [storefront.components.order :refer [order-label]]
            [storefront.keypaths :as keypaths]))

(defn display-order [data order-id]
  (when-let [order ((get-in data keypaths/past-orders) order-id)]
    [:div.loose-table-row
     [:div.left-content
      [:p (-> order :completed_at locale-date)]
      [:p.top-pad
       [:a.order-link
        (utils/route-to data events/navigate-order {:order-id order-id})
        order-id]]]
     [:div.right-content
      [:p.order-money (:display_order_total_after_store_credit order)]
      (order-label order)]]))

(defn my-orders-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "My Account"]
     [:p.store-credit-balance-wrapper
      (when false ;; FIXME available store credit for user is 0?
        {:class "no-credit"})
      "Available store credit: "
      [:span.store-credit-balance (as-money 10.0 ;; FIXME get available store credit
                                            )]]
     [:div.account-my-orders
      [:h4.account-orders-header "My Recent Orders"]
      (let [order-ids (get-in data keypaths/my-order-ids)]
        (when order-ids
          (if (empty? order-ids)
            [:p "You have no orders yet"]
            (map (partial display-order data) order-ids))))]])))
