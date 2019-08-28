(ns storefront.components.order-summary
  (:require [storefront.accessors.adjustments :as adjustments]
            [storefront.accessors.orders :as orders]
            [storefront.components.money-formatters :as mf]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
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
     (mf/as-money-or-free amount)]]))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn display-order-summary [order {:keys [read-only? available-store-credit use-store-credit?]}]
  (let [adjustments-including-tax (orders/all-order-adjustments order)
        shipping-item             (orders/shipping-item order)
        store-credit              (min (:total order) (or available-store-credit
                                                          (-> order :cart-payments :store-credit :amount)
                                                          0.0))]
    [:div
     [:.py2.border-top.border-bottom.border-gray
      [:table.col-12
       [:tbody
        (summary-row "Subtotal" (orders/products-subtotal order))
        (for [{:keys [name price coupon-code] :as adjustment} adjustments-including-tax]
          (when (adjustments/non-zero-adjustment? adjustment)
            (summary-row
             {:key name}
             [:div {:data-test (text->data-test-name name)}
              (orders/display-adjustment-name name)
              (when (and (not read-only?) coupon-code)
                [:a.ml1.h6.gray
                 (merge {:data-test "cart-remove-promo"}
                        (utils/fake-href events/control-checkout-remove-promotion
                                         {:code coupon-code}))
                 "Remove"])]
             price)))

        (when shipping-item
          (summary-row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

        (when (and (pos? store-credit)
                   (not (orders/applied-install-promotion order)))
          (summary-row "Store Credit" (- store-credit)))]]]
     [:.py2.h2
      [:.flex
       [:.flex-auto.light "Total"]
       [:.right-align
        (cond-> (:total order)
          use-store-credit? (- store-credit)
          true              mf/as-money)]]]]))


