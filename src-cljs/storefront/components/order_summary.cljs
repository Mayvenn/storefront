(ns storefront.components.order-summary
  (:require [storefront.components.formatters :refer [as-money as-money-or-free]]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
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

(defn redesigned-order-summary-row
  ([name amount] (redesigned-order-summary-row {} name amount))
  ([row-attrs name amount]
   [:tr.h4.line-height-4
    (merge row-attrs
           (when (neg? amount)
             {:class "green"}))
    [:td name]
    [:td.right-align.medium
     {:class (when-not (neg? amount)
               "navy")}
     (as-money-or-free amount)]]))

(defn redesigned-display-order-summary [order]
  (let [adjustments   (orders/all-order-adjustments order)
        quantity      (orders/product-quantity order)
        shipping-item (orders/shipping-item order)
        store-credit  (-> order :cart-payments :store-credit)]
    [:div
     [:.py2.border-top.border-bottom.border-light-silver
      [:table.col-12
       [:tbody
        (redesigned-order-summary-row "Subtotal"
                                      (orders/products-subtotal order))

        (for [{:keys [name price coupon-code]} adjustments]
          (when-not (= price 0)
            (redesigned-order-summary-row
             {:key name}
             [:div
              (orders/display-adjustment-name name)
              (when coupon-code
                [:a.ml1.h5.silver
                 (utils/fake-href events/control-checkout-remove-promotion {:code coupon-code})
                 "Remove"])]
             price)))

        (when shipping-item
          (redesigned-order-summary-row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

        (when store-credit
          (redesigned-order-summary-row "Store Credit" (- (:amount store-credit))))]]]
     [:.py2.h1
      [:.flex
       [:.flex-auto.extra-light "Total"]
       [:.right-align.dark-gray
        (as-money (- (:total order) (:amount store-credit 0.0)))]]] ]))

(defn ^:private redesigned-display-line-item [{:keys [id product-name variant-attrs unit-price] :as line-item}
                                              thumbnail
                                              quantity-line]
  [:.clearfix.mb1.border-bottom.border-light-silver.py2 {:key id}
   [:a.left.mr1
    [:img.border.border-light-silver.rounded-1
     {:src   thumbnail
      :alt   product-name
      :style {:width  "7.33em"
              :height "7.33em"}}]]
   [:.overflow-hidden.h4.black.p1
    [:a.black.medium.titleize (products/summary line-item)]
    [:.mt1.h5.line-height-2
     (when-let [length (:length variant-attrs)]
       [:div "Length: " length])
     [:div "Price: " (as-money unit-price)]
     quantity-line]]])

(defn redesigned-display-line-items [line-items products]
  (for [{:keys [quantity product-id] :as line-item} line-items]
    (redesigned-display-line-item
     line-item
     (products/thumbnail-url products product-id)
     [:div "Quantity: " quantity])))

(defn redesigned-display-adjustable-line-items [line-items products update-line-item-requests delete-line-item-requests]
  (for [{:keys [product-id quantity] variant-id :id :as line-item} line-items]
    (let [updating? (get update-line-item-requests variant-id)
          removing? (get delete-line-item-requests variant-id)]
      (redesigned-display-line-item
       line-item
       (products/thumbnail-url products product-id)
       [:.mt2.flex.items-center.justify-between
        (if removing?
          [:.h2 {:style {:width "1.2em"}} ui/spinner]
          [:a.silver (utils/fake-href events/control-cart-remove variant-id) "Remove"])
        [:.h2
         (ui/counter quantity
                     updating?
                     (utils/send-event-callback events/control-cart-line-item-dec
                                                {:variant-id variant-id})
                     (utils/send-event-callback events/control-cart-line-item-inc
                                                {:variant-id variant-id}))]]))))
