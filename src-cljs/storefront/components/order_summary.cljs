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
            [storefront.utils.query :as query]
            [spice.core :as spice]
            [clojure.string :as string]))

(defn ^:private summary-row
  ([name amount] (summary-row {} name amount))
  ([row-attrs name amount]
   [:tr.h5
    (merge (when-not (pos? amount)
             {:class "teal"})
           row-attrs)
    [:td.pyp3 name]
    [:td.pyp3.right-align.medium
     {:class (when (pos? amount)
               "navy")}
     (as-money-or-free amount)]]))

(def essence-faux-line-item
  [:div.pb3.border-bottom.border-gray
   [:div.clearfix.border.border-orange.py3
    [:div.left.mr1
     [:img.ml1.block {:src (assets/path "/images/essence/essence@2x.png") :style {:width "7em"}}]]
    [:div.overflow-hidden
     [:div.mx1
      [:div.mb1
       [:div.bold.shout.h5 "bonus gift!"]
       [:div.h6.mt1
        "A one-year subscription to " [:span.bold "ESSENCE "]
        "magazine is " [:span.underline "included"] " with your order."]]
      [:a.h6.navy
       (utils/fake-href events/control-essence-offer-details)
       "Offer and Rebate Details ➤"]]]]])

(defn display-order-summary [order {:keys [read-only? available-store-credit use-store-credit?]}]
  (let [adjustments              (orders/all-order-adjustments order)
        shipping-item            (orders/shipping-item order)
        store-credit             (min (:total order) (or available-store-credit
                                                         (-> order :cart-payments :store-credit :amount)
                                                         0.0))
        text->data-test-name (fn [name]
                               (-> name
                                   (string/replace #"[0-9]" (comp spice/number->word int))
                                   string/lower-case
                                   (string/replace #"[^a-z]+" "-")))]
    [:div
     [:.py2.border-top.border-bottom.border-gray
      [:table.col-12
       [:tbody
        (summary-row "Subtotal" (orders/products-subtotal order))
        (for [{:keys [name price coupon-code]} adjustments]
          (when (or (not (= price 0))
                    (= coupon-code "amazon"))
            (summary-row
             {:key name}
             [:div {:data-test (text->data-test-name name)}
              (orders/display-adjustment-name name)
              (when (and (not read-only?) coupon-code)
                [:a.ml1.h6.gray
                 (utils/fake-href events/control-checkout-remove-promotion {:code coupon-code})
                 "Remove"])]
             price)))

        (when shipping-item
          (summary-row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

        (when (and use-store-credit? (pos? store-credit))
          (summary-row "Store Credit" (- store-credit)))]]]
     [:.py2.h2
      [:.flex
       [:.flex-auto.light "Total"]
       [:.right-align
        (cond-> (:total order)
          use-store-credit? (- store-credit)
          true              as-money)]]] ]))

(defn ^:private display-line-item [{:keys [id variant-attrs unit-price] :as line-item}
                                   thumbnail
                                   quantity-line]
  [:div.clearfix.border-bottom.border-gray.py3 {:key id}
   [:a.left.mr1
    [:img.block.border.border-gray.rounded
     (assoc thumbnail :style {:width  "7.33em"
                              :height "7.33em"})]]
   [:div.overflow-hidden
    [:div.ml1
     [:a.medium.titleize.h5 (products/product-title line-item)]
     [:div.h6.mt1.line-height-1
      (when-let [length (:length variant-attrs)]
        [:div.pyp2 "Length: " length])
      [:div.pyp2 "Price Each: " (as-money-without-cents unit-price)]
      quantity-line]]]])

(defn adjustable-quantity-line [line-item sku removing? updating?]
  [:.mt1.flex.items-center.justify-between
   (if removing?
     [:.h3 {:style {:width "1.2em"}} ui/spinner]
     [:a.gray.medium (utils/fake-href events/control-cart-remove (:id line-item)) "Remove"])
   [:.h3
    (ui/counter {:spinning? updating?
                 :data-test (:sku sku)}
                (:quantity line-item)
                (utils/send-event-callback events/control-cart-line-item-dec {:variant line-item})
                (utils/send-event-callback events/control-cart-line-item-inc {:variant line-item}))]])

(defn display-line-items-products [line-items products skus]
  (for [{:keys [quantity product-id] :as line-item} line-items]
    (let [product       (products/find-product-by-sku-id products (:sku line-item))
          line-item-sku (get skus (:sku line-item))]
      (display-line-item
       line-item
       (products/medium-img product line-item-sku)
       [:div.pyp2 "Quantity: " quantity]))))

(defn display-adjustable-line-items-products [line-items products skus update-line-item-requests delete-line-item-requests]
  (for [{:keys [product-id quantity] variant-id :id :as line-item} line-items]
    (let [product       (products/find-product-by-sku-id products (:sku line-item))
          line-item-sku (get skus (:sku line-item))]
      (display-line-item
       line-item
       (products/medium-img product line-item-sku)
       (adjustable-quantity-line line-item
                                 line-item-sku
                                 (get delete-line-item-requests variant-id)
                                 (get update-line-item-requests (:sku line-item)))))))
