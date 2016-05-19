(ns storefront.components.order-summary
  (:require [storefront.components.formatters :refer [as-money as-money-or-free]]
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
            [storefront.accessors.orders :as orders]
            [storefront.keypaths :as keypaths]))

(defn- field [name value & [classes]]
  [:div.line-item-attr {:class classes}
   [:span.cart-label name]
   [:span.cart-value value]])

(defn- display-adjustment-row [{:keys [name price coupon-code]}]
  (when-not (= price 0)
    [:tr.order-summary-row.adjustment {:key name}
     [:td
      [:h5 (orders/display-adjustment-name name)
       (when coupon-code
         (let [on-click (utils/send-event-callback events/control-checkout-remove-promotion {:code coupon-code})]
           [:a {:href "" :on-click on-click} "Remove"]))]]
     [:td
      [:h5 (as-money price)]]]))

(defn- display-adjustments [adjustments]
  (map display-adjustment-row adjustments))

(defn- display-shipment [data shipping]
  (when-let [shipping-methods (get-in data keypaths/shipping-methods)]
    (let [shipping-method (orders/shipping-method-details shipping-methods shipping)]
      [:tr.order-summary-row
       [:td
        [:h5 (:name shipping-method)]]
       [:td
        [:h5 (as-money (* (:quantity shipping) (:unit-price shipping)))]]])))

(defn- display-line-item [data interactive? {product-id :product-id variant-id :id :as line-item}]
  [:div.line-item {:key variant-id}
   [:a [:img {:src (products/thumbnail-url (get-in data keypaths/products) product-id)
              :alt (:product-name line-item)}]]
   [:div.line-item-detail.interactive
    [:h4
     [:a (products/summary line-item)]]
    (when interactive?
      (let [update-spinner-key (conj request-keys/update-line-item variant-id)
            removing? (utils/requesting? data (conj request-keys/delete-line-item variant-id))]
        [:.quantity-adjustments
         (om/build counter-component
                   data
                   {:opts {:path (conj keypaths/cart-quantities variant-id)
                           :inc-event events/control-cart-line-item-inc
                           :dec-event events/control-cart-line-item-dec
                           :spinner-key update-spinner-key}})
         [:a.delete
          {:href "#"
           :class (when removing? "saving")
           :on-click (if removing?
                       utils/noop-callback
                       (utils/send-event-callback events/control-cart-remove
                                                  variant-id))}
          "Remove"]]))
    (when-let [length (-> line-item :variant-attrs :length)]
      (field "Length: " length))
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
          quantity    (orders/product-quantity order)
          shipping-item (orders/shipping-item order)
          store-credit (-> order :cart-payments :store-credit)]
      [:tbody
       [:tr.cart-subtotal.order-summary-row
        [:td
         [:h5 (str "Subtotal (" quantity " Item"
                   (when (> quantity 1) "s") ")")]]
        [:td
         [:h5 (as-money (orders/products-subtotal order))]]]
       (display-adjustments adjustments)
       (when shipping-item
         (display-shipment data shipping-item))
       [:tr.cart-total.order-summary-row
        [:td [:h5 "Order Total"]]
        [:td [:h5 (as-money (:total order))]]]
       (when store-credit
         [:tr.store-credit-used.order-summary-row.adjustment
          [:td [:h5 "Store Credit"]]
          [:td [:h5 (as-money (- (:amount store-credit)))]]])
       (when store-credit
         [:tr.balance-due.order-summary-row.cart-total
          [:td [:h5 (if (= "paid" (:payment-state order)) "Amount charged" "Balance Due")]]
          [:td [:h5 (as-money (- (:total order) (:amount store-credit)))]]])])]])

(defn display-cart-summary [data order]
  [:div
   [:h4.order-summary-header "Order Summary"]
   [:table.order-summary-total
    (let [adjustments (orders/all-order-adjustments order)
          quantity    (orders/product-quantity order)
          shipping-item (orders/shipping-item order)
          store-credit (-> order :cart-payments :store-credit)]
      [:tbody
       [:tr.cart-subtotal.order-summary-row
        [:td
         [:h5 (str "Subtotal (" quantity " Item"
                   (when (> quantity 1) "s") ")")]]
        [:td
         [:h5 (as-money (orders/products-subtotal order))]]]
       (display-adjustments adjustments)
       (when shipping-item
         (display-shipment data shipping-item))
       [:tr.cart-total.order-summary-row
        [:td [:h5 "Order Total"]]
        [:td [:h5 (as-money (:total order))]]]])]])

(defn redesigned-display-order-summary [shipping-methods order]
  (let [adjustments   (orders/all-order-adjustments order)
        quantity      (orders/product-quantity order)
        shipping-item (orders/shipping-item order)
        store-credit  (-> order :cart-payments :store-credit)
        row (fn row
              ([name amount] (row {} name amount))
              ([row-attrs name amount]
               [:tr.h4.line-height-4
                (merge row-attrs
                       (when (neg? amount)
                         {:class "green"}))
                [:td name]
                [:td.right-align.medium
                 {:class (when-not (neg? amount)
                           "navy")}
                 (as-money-or-free amount)]]))]
    [:div
     [:table.col-12
      [:tbody
       (row "Subtotal"
            (orders/products-subtotal order))

       (for [{:keys [name price coupon-code]} adjustments]
         (when-not (= price 0)
           (row {:key name}
                [:div
                 (orders/display-adjustment-name name)
                 (when coupon-code
                   [:a.ml1.h5.silver
                    (utils/fake-href events/control-checkout-remove-promotion {:code coupon-code})
                    "Remove"])]
                price)))

       (when (and shipping-item shipping-methods)
         (row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

       (when store-credit
         (row "Store Credit" (- (:amount store-credit))))]]
     [:.border-top.border-light-silver.mt2.py2.h1
      [:.flex
       [:.flex-auto.extra-light "Total"]
       [:.right-align.dark-gray
        (as-money (- (:total order) (:amount store-credit 0.0)))]]] ]))

(defn redesigned-display-line-items [products order]
  (for [{product-id :product-id variant-id :id :as line-item} (orders/product-items order)]
    [:.mb1.border-bottom.border-light-silver.py2 {:key variant-id}
     [:a.col.col-4.mbp2
      [:img.border.border-light-silver.rounded-1 {:src (products/thumbnail-url products product-id)
                                                :alt (:product-name line-item)
                                                :style {:width "7.33em"
                                                        :height "7.33em"}}]]
     [:.h4.col.col-8.black.py1
      [:a.black.medium.titleize (products/summary line-item)]
      [:.mt1.line-height-2
       (when-let [length (-> line-item :variant-attrs :length)]
         [:.h5 "Length: " length])
       [:.h5 "Price: " (as-money (:unit-price line-item))]
       [:.h5 "Quantity: " (:quantity line-item)]]]
     [:.clearfix]]))
