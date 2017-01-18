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
   [:tr.h5
    (merge (when (neg? amount)
             {:class "teal"})
           row-attrs)
    [:td.pyp3 name]
    [:td.pyp3.right-align.medium
     {:class (when-not (neg? amount)
               "navy")}
     (as-money-or-free amount)]]))

(def essence-faux-line-item
  [:div
   [:div.flex.border.border-orange.py2
    [:div.flex-none.mx1 {:style {:width "7.33em"}}
     [:div.hide-on-mb
      [:img {:src (assets/path "/images/essence/essence@2x.png") :width "110px" :height "112px"}]]
     [:div.hide-on-tb-dt
      [:img {:src (assets/path "/images/essence/essence@2x.png") :width "123px" :height "125px"}]]]
    [:div.flex-auto.mr1
     [:div.h6.mb1
      [:div.bold.shout.mb1.h5 "bonus gift!"]
      "A one-year subscription to " [:span.bold "ESSENCE "] "magazine is "
      [:span.underline "included"]
      " with your order."]
     [:a.h6.navy
      (utils/fake-href events/control-essence-offer-details)
      "Offer and Rebate Details" ui/nbsp "➤"]]]
   [:div.border-bottom.border-gray ui/nbsp]])

(defn display-order-summary [order {:keys [read-only?]} price-strikeout?]
  (let [adjustments   (orders/all-order-adjustments order)
        quantity      (orders/product-quantity order)
        shipping-item (orders/shipping-item order)
        store-credit  (-> order :cart-payments :store-credit)]
    [:div
     [:.py2.border-top.border-bottom.border-gray
      [:table.col-12
       [:tbody
        (summary-row "Subtotal" (orders/products-subtotal order))
        (for [{:keys [name price coupon-code] :as adj} adjustments]
          (when-not (= price 0)
            (summary-row
             (merge {:key name}
                    ;; coupon-code present, but nil means this adjustment is the bundle discount
                    (when (and price-strikeout? (= [:coupon-code nil] (find adj :coupon-code)))
                      {:class "red"}))
             [:div
              (orders/display-adjustment-name name)
              (when (and (not read-only?) coupon-code)
                [:a.ml1.h6.gray
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
       [:.right-align
        (as-money (- (:total order) (:amount store-credit 0.0)))]]] ]))

(defn ^:private display-line-item [{:keys [id variant-attrs unit-price] :as line-item}
                                   thumbnail
                                   quantity-line
                                   bundle-quantity
                                   price-strikeout?]
  [:.clearfix.mb1.border-bottom.border-gray.py3 {:key id}
   [:a.left.mr1
    [:img.border.border-gray.rounded
     (assoc thumbnail :style {:width  "7.33em"
                              :height "7.33em"})]]
   [:.overflow-hidden.h5.p1
    [:a.medium.titleize (products/product-title line-item)]
    [:.mt1.h6.line-height-1
     (when-let [length (:length variant-attrs)]
       [:div.pyp2 "Length: " length])
     (if price-strikeout?
       [:div.pyp2 "Price Each: " (ui/strike-price {:price            unit-price
                                                   :bundle-quantity  bundle-quantity
                                                   :price-strikeout? price-strikeout?
                                                   :bundle-eligible? (products/bundle? line-item)})]
       [:div.pyp2 "Price: " (as-money-without-cents unit-price)])
     quantity-line]]])

(defn display-line-items [line-items products price-strikeout?]
  (let [bundle-quantity (orders/line-item-quantity (filter products/bundle? line-items))]
    (for [{:keys [quantity product-id] :as line-item} line-items]
      (display-line-item
       line-item
       (products/small-img products product-id)
       [:div.pyp2 "Quantity: " quantity]
       bundle-quantity
       price-strikeout?))))

(defn display-adjustable-line-items [line-items products update-line-item-requests delete-line-item-requests price-strikeout?]
  (let [bundle-quantity (orders/line-item-quantity (filter products/bundle? line-items))]
    (for [{:keys [product-id quantity] variant-id :id :as line-item} line-items]
      (let [updating? (get update-line-item-requests variant-id)
            removing? (get delete-line-item-requests variant-id)]
        (display-line-item
         line-item
         (products/small-img products product-id)
         [:.mt1.flex.items-center.justify-between
          (if removing?
            [:.h3 {:style {:width "1.2em"}} ui/spinner]
            [:a.gray.medium (utils/fake-href events/control-cart-remove variant-id) "Remove"])
          [:.h3
           (when-let [variant (query/get {:id variant-id}
                                         (:variants (get products product-id)))]
             (ui/counter quantity
                         updating?
                         (utils/send-event-callback events/control-cart-line-item-dec {:variant variant})
                         (utils/send-event-callback events/control-cart-line-item-inc {:variant variant})))]]
         bundle-quantity
         price-strikeout?)))))
