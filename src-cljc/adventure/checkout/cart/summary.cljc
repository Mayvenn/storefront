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

(defn summary-total-section [{:keys [freeinstall-line-item-data order]}]
  [:div.py2.h2
   [:div.flex
    [:div.flex-auto.light "Hair + Install Total"]
    [:div.right-align.medium
     (some-> order :total mf/as-money)]]
   [:div
    [:div.flex.justify-end
     [:div.h6.bg-purple.white.px1.nowrap.medium.mb1
      "Includes Free Install"]]
    [:div.flex.justify-end
     [:div.h6.light.dark-gray.px1.nowrap.italic
      "You've saved "
      [:span.bold {:data-test "total-savings"}
       (mf/as-money (:total-savings freeinstall-line-item-data))]]]]])

(def add-more-items-section
  [:div.p2.flex.flex-wrap
   [:div.col-5.h5 "Hair + Install Total"]
   [:div.col-7.h6.dark-gray.right-align
    "Add 3 hair items to calculate total price"]])

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

       (when (empty? (orders/product-items order))
         (summary-row
          {:key       (str -1 "-" name)
           :data-test "freeinstall"}
          [:div.flex.items-center.align-middle
           "Free Install"]
          (- 0 (:price freeinstall-line-item-data))))

       (when (pos? store-credit)
         (summary-row "Store Credit" (- store-credit)))]]]
    (if (pos? (:number-of-items-needed freeinstall-line-item-data))
      add-more-items-section
      (summary-total-section data))]))

(defn query [data]
  (let [order                      (get-in data keypaths/order)
        shipping-item              (orders/shipping-item order)
        freeinstall-line-item-data (cart-items/freeinstall-line-item-query data)]
    {:freeinstall-line-item-data freeinstall-line-item-data
     :order                      order
     :shipping-cost              (* (:quantity shipping-item) (:unit-price shipping-item))
     :adjustments-including-tax  (orders/all-order-adjustments order)
     :subtotal                   (cond-> (orders/products-subtotal order)
                                   freeinstall-line-item-data
                                   (+ (spice/parse-double (:price freeinstall-line-item-data))))}))
