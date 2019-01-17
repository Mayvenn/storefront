(ns adventure.checkout.cart.summary
  (:require [adventure.checkout.cart.items :as cart-items]
            [clojure.string :as string]
            [spice.core :as spice]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
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
     (when-not (pos? amount)
       {:class "teal"})
     row-attrs)
    [:td.pyp1 content]
    [:td.pyp1.right-align.medium
     (mf/as-money-or-free amount)]]))

(defn non-zero-adjustment? [{:keys [price coupon-code]}]
  (or (not (= price 0))
      (#{"amazon" "freeinstall" "install"} coupon-code)))

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

(defn component
  [{:keys [freeinstall-line-item-data
           order
           store-credit
           shipping-cost
           adjustments-including-tax
           promo-data
           subtotal] :as data} owner _]
  (component/create
   [:div {:data-test "cart-order-summary"}
    [:div.hide-on-dt.border-top.border-light-gray]
    [:div.py1.border-bottom.border-light-gray
     [:table.col-12
      [:tbody
       (summary-row {:data-test "subtotal"} "Subtotal" subtotal)
       (when shipping-cost
         (summary-row {:class "black"} "Shipping" shipping-cost))

       (for [[i {:keys [name price coupon-code] :as adjustment}] (map-indexed vector adjustments-including-tax)]
         (when (non-zero-adjustment? adjustment)
           (summary-row
            {:key       (str i "-" name)
             :data-test (text->data-test-name name)}
            [:div.flex.items-center.align-middle
             (when (= "Bundle Discount" name)
               (svg/discount-tag {:class  "mxnp6"
                                  :height "2em" :width "2em"}))
             (orders/display-adjustment-name name)]
            (if (and freeinstall-line-item-data
                     (= "freeinstall" coupon-code))
              (- 0 (:price freeinstall-line-item-data))
              price))))

       (when (pos? store-credit)
         (summary-row "Store Credit" (- store-credit)))]]]
    (summary-total-section data)]))

(defn query [data]
  (let [order                      (get-in data keypaths/order)
        shipping-item              (orders/shipping-item order)
        freeinstall-line-item-data (cart-items/freeinstall-line-item-query data)]
    {:freeinstall-line-item-data freeinstall-line-item-data
     :order                      order
     :shipping-cost              (* (:quantity shipping-item) (:unit-price shipping-item))
     :adjustments-including-tax  (orders/all-order-adjustments order)
     :promo-data                 {:coupon-code   (get-in data keypaths/cart-coupon-code)
                                  :applying?     (utils/requesting? data request-keys/add-promotion-code)
                                  :focused       (get-in data keypaths/ui-focus)
                                  :error-message (get-in data keypaths/error-message)
                                  :field-errors  (get-in data keypaths/field-errors)}
     :subtotal                   (cond-> (orders/products-subtotal order)
                                   freeinstall-line-item-data
                                   (+ (spice/parse-double (:price freeinstall-line-item-data))))}))
