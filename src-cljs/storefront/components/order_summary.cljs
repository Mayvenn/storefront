(ns storefront.components.order-summary
  (:require [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.assets :as assets]
            [storefront.components.money-formatters
             :refer
             [as-money as-money-or-free as-money-without-cents]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.utils.query :as query]))

(defn ^:private summary-row
  ([name amount] (summary-row {} name amount))
  ([row-attrs name amount]
   [:tr.h5.line-height-4
    (merge row-attrs
           (when (neg? amount)
             {:class "teal"}))
    [:td name]
    [:td.right-align.medium
     {:class (when-not (neg? amount)
               "navy")}
     (as-money-or-free amount)]]))

(def essence-faux-line-item
  [:div
   [:div.flex.border.border-orange.py1
    [:div.flex-none.mx1 {:style {:width "7.33em"}}
     [:div.to-lg-hide
      [:img {:src (assets/path "/images/essence/essence@2x.png") :width "94px" :height "96px"}]]
     [:div.lg-up-hide
      [:img {:src (assets/path "/images/essence/essence@2x.png") :width "72px" :height "70px"}]]]
    [:div.flex-auto.mr1
     [:div.h6.mb1.line-height-2
      [:div.bold.shout.mb1.h5 "bonus offer!"]
      "A one-year subscription to " [:span.bold "ESSENCE "] "magazine is "
      [:span.underline "included"]
      " with your order."]
     [:a.h6.navy
      (utils/fake-href events/control-essence-offer-details)
      "Offer and Rebate Details ➤"]]]
   [:div.border-bottom.border-dark-silver ui/nbsp]])

(defn display-order-summary [order]
  (let [adjustments   (orders/all-order-adjustments order)
        quantity      (orders/product-quantity order)
        shipping-item (orders/shipping-item order)
        store-credit  (-> order :cart-payments :store-credit)]
    [:div
     [:.py2.border-top.border-bottom.border-dark-silver
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
                [:a.ml1.h6.light-gray
                 (utils/fake-href events/control-checkout-remove-promotion {:code coupon-code})
                 "Remove"])]
             price)))

        (when shipping-item
          (summary-row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

        (when store-credit
          (summary-row "Store Credit" (- (:amount store-credit))))]]]
     [:.py2.h2
      [:.flex
       [:.flex-auto.light "Total"]
       [:.right-align.gray
        (as-money (- (:total order) (:amount store-credit 0.0)))]]] ]))

(defn ^:private display-line-item [{:keys [id product-name variant-attrs unit-price] :as line-item}
                                   thumbnail
                                   quantity-line]
  [:.clearfix.mb1.border-bottom.border-dark-silver.py2 {:key id}
   [:a.left.mr1
    [:img.border.border-dark-silver.rounded
     {:src   thumbnail
      :alt   product-name
      :style {:width  "7.33em"
              :height "7.33em"}}]]
   [:.overflow-hidden.h5.dark-gray.p1
    [:a.dark-gray.medium.titleize (products/product-title line-item)]
    [:.mt1.h6.line-height-2
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
          [:.h3 {:style {:width "1.2em"}} ui/spinner]
          [:a.light-gray (utils/fake-href events/control-cart-remove variant-id) "Remove"])
        [:.h3
         (when-let [variant (query/get {:id variant-id}
                                       (:variants (get products product-id)))]
           (ui/counter quantity
                       updating?
                       (utils/send-event-callback events/control-cart-line-item-dec {:variant variant})
                       (utils/send-event-callback events/control-cart-line-item-inc {:variant variant})))]]))))
