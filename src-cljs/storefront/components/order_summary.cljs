(ns storefront.components.order-summary
  (:require [storefront.components.formatters :refer [as-money]]
            [storefront.components.utils :as utils]
            [storefront.accessors.products :as products]
            [storefront.accessors.taxons :as taxons]
            [storefront.accessors.shipping :as shipping]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [clojure.string :as string]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.counter :refer [counter-component]]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [storefront.accessors.orders :as orders]
            [storefront.keypaths :as keypaths]))

(defn- field [name value & [classes]]
  [:div.line-item-attr {:class classes}
   [:span.cart-label name]
   [:span.cart-value value]])

(defn- display-adjustment-row [data {:keys [name price coupon-code]}]
  (when-not (= price 0)
    [:tr.order-summary-row.adjustment
     [:td
      [:h5 (orders/display-adjustment-name name)
       (when coupon-code
         (let [on-click (utils/send-event-callback data events/control-checkout-remove-promotion {:code coupon-code})]
           [:a {:href "" :on-click on-click} "Remove"]))]]
     [:td
      [:h5 (as-money price)]]]))

(defn- display-adjustments [data adjustments]
  (map (partial display-adjustment-row data) adjustments))

(defn- display-shipment [data shipping]
  (let [shipping-method (->> (get-in data keypaths/shipping-methods)
                             (filter #(= (:sku shipping) (:sku %)))
                             first)]
    [:tr.order-summary-row
     [:td
      [:h5 (:name shipping-method)]]
     [:td
      [:h5 (as-money (* (:quantity shipping) (:unit-price shipping)))]]]))

(defn- display-variant-options [variant-attrs]
  (field "Length:" (:length variant-attrs)))

(defn- display-line-item [data interactive? {product-id :product-id variant-id :id :as line-item}]
  [:div.line-item
   [:a [:img {:src (first (products/thumbnail-urls data product-id))
              :alt (:product-name line-item)}]]
   [:div.line-item-detail.interactive
    [:h4
     [:a (products/summary line-item)]]
    (when interactive?
      (let [update-spinner-key (conj request-keys/update-line-item variant-id)
            delete-request (query/get
                            {:request-key
                             (conj request-keys/delete-line-item variant-id)}
                            (get-in data keypaths/api-requests))]
        [:.quantity-adjustments
         (om/build counter-component
                   data
                   {:opts {:path (conj keypaths/cart-quantities variant-id)
                           :inc-event events/control-cart-line-item-inc
                           :dec-event events/control-cart-line-item-dec
                           :spinner-key update-spinner-key}})
         [:a.delete
          {:href "#"
           :class (when delete-request "saving")
           :on-click (if delete-request
                       utils/noop-callback
                       (utils/send-event-callback data
                                                  events/control-cart-remove
                                                  variant-id))}
          "Remove"]]))
    (map display-variant-options (:variant-attrs line-item))
    (when (not interactive?)
      (field "Quantity:" (:quantity line-item)))
    (field "Price:" (as-money (:unit-price line-item)) "item-form" "price")]
   [:div {:style {:clear "both"}}]])

(defn display-line-items [data order & [interactive?]]
  (map (partial display-line-item data interactive?) (orders/product-items order)))

(defn display-order-summary [data order]
  [:div
   [:h4.order-summary-header "Order Summary"]
   [:table.order-summary-total
    (let [adjustments (orders/all-order-adjustments order)
          quantity    (orders/product-quantity order)]
      [:tbody
       (when-not (empty? adjustments)
         (list
          [:tr.cart-subtotal.order-summary-row
           [:td
            [:h5 (str "Subtotal (" quantity " Item"
                      (when (> quantity 1) "s") ")")]]
           [:td
            [:h5 (as-money (orders/products-subtotal order))]]]
          (display-adjustments data adjustments)
          (when-let [shipping-item (orders/shipping-item order)]
            (display-shipment data shipping-item))))
       [:tr.cart-total.order-summary-row
        [:td [:h5 "Order Total"]]
        [:td [:h5 (as-money (:total order))]]]
       (when-let [store-credit (-> order :cart-payments :store-credit)]
         (list
          [:tr.store-credit-used.order-summary-row.adjustment
           [:td [:h5 "Store Credit"]]
           [:td [:h5 (as-money (- (:amount store-credit)))]]]
          [:tr.balance-due.order-summary-row.cart-total
           [:td [:h5 (if (= "paid" (:payment-state order)) "Amount charged" "Balance Due")]]
           [:td [:h5 (as-money (- (:total order) (:amount store-credit)))]]]))])]])
