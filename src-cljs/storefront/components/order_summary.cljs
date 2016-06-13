(ns storefront.components.order-summary
  (:require [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.components.formatters :refer [as-money as-money-without-cents as-money-or-free]]
            [storefront.components.ui :as ui]
            [storefront.components.utils :as utils]
            [storefront.events :as events]))

(defn ^:private summary-row
  ([name amount] (summary-row {} name amount))
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

(defn display-order-summary [order]
  (let [adjustments   (orders/all-order-adjustments order)
        quantity      (orders/product-quantity order)
        shipping-item (orders/shipping-item order)
        store-credit  (-> order :cart-payments :store-credit)]
    [:div
     [:.py2.border-top.border-bottom.border-light-silver
      [:table.col-12
       [:tbody
        (summary-row "Subtotal"
                                      (orders/products-subtotal order))

        (for [{:keys [name price coupon-code]} adjustments]
          (when-not (= price 0)
            (summary-row
             {:key name}
             [:div
              (orders/display-adjustment-name name)
              (when coupon-code
                [:a.ml1.h5.silver
                 (utils/fake-href events/control-checkout-remove-promotion {:code coupon-code})
                 "Remove"])]
             price)))

        (when shipping-item
          (summary-row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

        (when store-credit
          (summary-row "Store Credit" (- (:amount store-credit))))]]]
     [:.py2.h1
      [:.flex
       [:.flex-auto.light "Total"]
       [:.right-align.dark-gray
        (as-money (- (:total order) (:amount store-credit 0.0)))]]] ]))

(defn ^:private display-line-item [{:keys [id product-name variant-attrs unit-price] :as line-item}
                                   thumbnail
                                   quantity-line]
  [:.clearfix.mb1.border-bottom.border-light-silver.py2 {:key id}
   [:a.left.mr1
    [:img.border.border-light-silver.rounded
     {:src   thumbnail
      :alt   product-name
      :style {:width  "7.33em"
              :height "7.33em"}}]]
   [:.overflow-hidden.h4.black.p1
    [:a.black.medium.titleize (products/summary line-item)]
    [:.mt1.h5.line-height-2
     (when-let [length (:length variant-attrs)]
       [:div "Length: " length])
     [:div "Price: "
      (as-money-without-cents unit-price)]
     quantity-line]]])

(defn display-line-items [line-items products]
  (for [{:keys [quantity product-id] :as line-item} line-items]
    (display-line-item
     line-item
     (products/thumbnail-url products product-id)
     [:div "Quantity: " quantity])))

(defn display-adjustable-line-items [line-items products update-line-item-requests delete-line-item-requests]
  (for [{:keys [product-id quantity] variant-id :id :as line-item} line-items]
    (let [updating? (get update-line-item-requests variant-id)
          removing? (get delete-line-item-requests variant-id)]
      (display-line-item
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
