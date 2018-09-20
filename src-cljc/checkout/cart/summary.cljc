(ns checkout.cart.summary
  (:require [checkout.cart.items :as cart-items]
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
    (merge (when (neg? amount)
             {:class "teal"})
           row-attrs) [:td.pyp1 content]
    [:td.pyp1.right-align.medium
     (mf/as-money-or-free amount)]]))

(defn non-zero-adjustment? [{:keys [price coupon-code]}]
  (or (not (= price 0))
      (#{"amazon" "freeinstall" "install"} coupon-code)))

(defn component
  [{:keys [free-install-line-item
           order
           store-credit
           shipping-item
           adjustments-including-tax
           promo-data]} owner _]
  (component/create
   [:div {:data-test "cart-order-summary"}
    [:div.hide-on-dt.border-top.border-light-gray]
    [:div.py1.border-bottom.border-light-gray
     [:table.col-12
      [:tbody
       (summary-row "Subtotal" (orders/products-subtotal order))
       (when shipping-item
         (summary-row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

       (when (orders/no-applied-promo? order)
         (let [{:keys [focused coupon-code field-errors updating? applying? error-message]} promo-data]
           [:tr.h5
            [:td
             {:col-span "2"}
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
                :args       {:on-click   (utils/send-event-callback events/control-cart-update-coupon)
                             :class      "flex justify-center items-center"
                             :size-class "flex-grow-3"
                             :data-test  "cart-apply-promo"
                             :disabled?  updating?
                             :spinning?  applying?}})]]]))

       (for [{:keys [name price coupon-code] :as adjustment} adjustments-including-tax]
         (when (non-zero-adjustment? adjustment)
           (summary-row
            {:key name}
            [:div.flex.items-center.align-middle {:data-test (text->data-test-name name)}
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
            (if (and free-install-line-item
                     (= "freeinstall" coupon-code))
              (- 0 (:price free-install-line-item))
              price))))

       (when (pos? store-credit)
         (summary-row "Store Credit" (- store-credit)))]]]
    [:div.py2.h2
     [:div.flex
      [:div.flex-auto.light (if free-install-line-item
                              "Hair + Install Total"
                              "Total")]
      [:div.right-align.medium
       (some-> order :total mf/as-money)]]
     (when free-install-line-item
       [:div
        [:div.flex.justify-end
         [:div.h6.bg-purple.white.px1.nowrap.medium.mb1
          "Includes Free Install"]]
        [:div.flex.justify-end
         [:div.h6.light.dark-gray.px1.nowrap.italic
          "You've saved "
          [:span.bold (mf/as-money (:total-savings free-install-line-item))]]]])]]))

(defn query [data]
  (let [order (get-in data keypaths/order)]
    {:free-install-line-item    (cart-items/freeinstall-line-item-query data)
     :order                     order
     :shipping-item             (orders/shipping-item order)
     :adjustments-including-tax (orders/all-order-adjustments order)
     :promo-data                {:coupon-code   (get-in data keypaths/cart-coupon-code)
                                 :applying?     (utils/requesting? data request-keys/add-promotion-code)
                                 :focused       (get-in data keypaths/ui-focus)
                                 :error-message (get-in data keypaths/error-message)
                                 :field-errors  (get-in data keypaths/field-errors)}}))
