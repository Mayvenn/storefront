(ns checkout.cart.summary
  (:require [checkout.cart.items :as cart-items]
            [clojure.string :as string]
            [spice.core :as spice]
            [storefront.accessors.adjustments :as adjustments]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn ^:private summary-row
  ([content amount] (summary-row {} content amount))
  ([row-attrs content amount]
   [:tr.h5
    (merge
     {:class "purple"}
     row-attrs)
    [:td.pyp1 content]
    [:td.pyp1.right-align.medium
     (mf/as-money-or-free amount)]]))

(defn promo-entry
  [{:keys [focused coupon-code field-errors updating? applying? error-message] :as promo-data}]
  [:form.mt2
   {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
   (ui/input-group
    {:keypath       keypaths/cart-coupon-code
     :wrapper-class "flex-grow-5 clearfix"
     :class         "h6"
     :data-test     "promo-code"
     :focused       focused
     :label         "Promo code"
     :value         coupon-code
     :errors        (when (get field-errors ["promo-code"])
                      [{:long-message error-message
                        :path         ["promo-code"]}])
     :data-ref      "promo-code"}
    {:ui-element ui/teal-button
     :content    "Apply"
     :args       {:on-click    (utils/send-event-callback events/control-cart-update-coupon)
                  :class       "flex justify-center items-center"
                  :width-class "flex-grow-3"
                  :data-test   "cart-apply-promo"
                  :disabled?   updating?
                  :spinning?   applying?}})])

(defn order-total-title [freeinstall-line-item-data]
  (if freeinstall-line-item-data
    "Hair + Install Total"
    "Total"))

(defn summary-total-section [{:keys [freeinstall-line-item-data order]}]
  [:div.py2.h2
   [:div.flex
    [:div.flex-auto.light (order-total-title freeinstall-line-item-data)]
    [:div.right-align.medium
     (some-> order :total mf/as-money)]]
   (when freeinstall-line-item-data
     [:div
      [:div.flex.justify-end
       [:div.h6.bg-purple.white.px1.nowrap.medium.mb1
        "Includes Free Install"]]
      [:div.flex.justify-end
       [:div.h6.light.dark-gray.px1.nowrap.italic
        "You've saved "
        [:span.bold {:data-test "total-savings"}
         (mf/as-money (:total-savings freeinstall-line-item-data))]]]])])

(defcomponent component
  [{:keys [freeinstall-line-item-data
           order
           store-credit
           shipping-cost
           adjustments-including-tax
           black-friday-time?
           promo-data
           subtotal] :as data} owner _]

  [:div {:data-test "cart-order-summary"}
   [:div.hide-on-dt.border-top.border-light-gray]
   [:div.py1.border-bottom.border-light-gray
    [:table.col-12
     [:tbody
      (summary-row {:class     "black"
                    :data-test "subtotal"} "Subtotal" subtotal)
      (when shipping-cost
        (summary-row {:class "black"} "Shipping" shipping-cost))

      (when (or black-friday-time?
                (orders/no-applied-promo? order))
        [:tr.h5
         [:td
          {:col-span "2"}
          (promo-entry promo-data)]])

      (for [[i {:keys [name price coupon-code] :as adjustment}] (map-indexed vector adjustments-including-tax)]
        (when (adjustments/non-zero-adjustment? adjustment)
          (summary-row
           {:key       (str i "-" name)
            :data-test (text->data-test-name name)}
           [:div.flex.items-center.align-middle
            (when (= "Bundle Discount" name)
              (svg/discount-tag {:class  "mxnp6"
                                 :height "2em" :width "2em"}))
            (orders/display-adjustment-name name)
            (when coupon-code
              [:a.ml1.h6.gray.flex.items-center
               (merge {:data-test "cart-remove-promo"}
                      (utils/fake-href events/control-checkout-remove-promotion
                                       {:code coupon-code}))
               (svg/close-x {:class "stroke-white fill-gray"})])]
           (if (and freeinstall-line-item-data
                    (= "freeinstall" coupon-code))
             (- 0 (:price freeinstall-line-item-data))
             price))))

      (when (pos? store-credit)
        (summary-row "Store Credit" (- store-credit)))]]]
   (summary-total-section data)])

(defn query [data]
  (let [order                      (get-in data keypaths/order)
        shipping-item              (orders/shipping-item order)
        freeinstall-line-item-data (cart-items/freeinstall-line-item-query data)]
    {:freeinstall-line-item-data freeinstall-line-item-data
     :order                      order
     :shipping-cost              (* (:quantity shipping-item) (:unit-price shipping-item))
     :adjustments-including-tax  (orders/all-order-adjustments order)
     :black-friday-time?         (experiments/black-friday-time? data)
     :promo-data                 {:coupon-code   (get-in data keypaths/cart-coupon-code)
                                  :applying?     (utils/requesting? data request-keys/add-promotion-code)
                                  :focused       (get-in data keypaths/ui-focus)
                                  :error-message (get-in data keypaths/error-message)
                                  :field-errors  (get-in data keypaths/field-errors)}
     :subtotal                   (cond-> (orders/products-subtotal order)
                                   freeinstall-line-item-data
                                   (+ (spice/parse-double (:price freeinstall-line-item-data))))}))
