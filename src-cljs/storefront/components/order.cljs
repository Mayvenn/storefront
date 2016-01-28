(ns storefront.components.order
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.components.order-summary :refer [display-line-items display-order-summary]]))

(defn order-shipped? [{:keys [state shipments]}]
  (and (= state "submitted")
       (some (comp #{"shipped"} :state) shipments)))

(def trackable-shipments (comp (partial filter :tracking-number) :shipments))

(defn order-label [order]
  (let [order-state (:state order)]
    (cond
      (order-shipped? order)
      [:p.order-label.shipped-label.top-pad "Shipped"]

      (= order-state "submitted")
      [:p.order-label.pending-label.top-pad "Pending"]

      :else
      [:p.order-label.refunded-label.top-pad "Refunded"])))

(defn order-component [data owner]
  (om/component
   (html
    [:div
     (when-let [order (get (get-in data keypaths/past-orders)
                           (get-in data keypaths/past-order-id))]
       (list
        [:h2.header-bar-heading (:number order)]
        [:div.order-container
         [:div.order-shipment-state-container
          (order-label order)]
         (when (and (order-shipped? order)
                    (seq (trackable-shipments order)))
           [:div.order-shipment-tracking-container
            [:h6 "Tracking"]
            [:div.tracking
             (for [shipment (trackable-shipments order)]
               [:span (:carrier shipment) " " (:tracking-number shipment)])]])]
        [:div.order-container
         (display-line-items data order)
         (display-order-summary data order)]))])))
