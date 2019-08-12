(ns checkout.consolidated-cart.summary
  (:require #?@(:cljs [[storefront.api :as api]])
            [checkout.consolidated-cart.items :as cart-items]
            [clojure.string :as string]
            [spice.core :as spice]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.platform.messages :as messages]))

(defmethod effects/perform-effects events/control-cart-add-freeinstall-coupon
  [_ _ _ _ app-state]
  #?(:cljs
     (api/add-promotion-code (= "shop" (get-in app-state keypaths/store-slug))
                             (get-in app-state keypaths/session-id)
                             (get-in app-state keypaths/order-number)
                             (get-in app-state keypaths/order-token)
                             "freeinstall"
                             false)))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn ^:private summary-row
  ([content amount] (summary-row {} content amount))
  ([row-attrs content amount]
   [:tr.h6.medium
    (merge
     (when-not (pos? amount)
       {:class "teal"})
     row-attrs)
    [:td.pyp1 content]
    [:td.pyp1.right-align.medium
     (mf/as-money-or-free amount)]]))

(defn promo-entry
  [{:keys [focused coupon-code field-errors updating? applying? error-message] :as promo-data}]
  [:form.mt2.bg-white
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

(defn non-zero-adjustment? [{:keys [price coupon-code]}]
  (or (not (= price 0))
      (#{"amazon" "freeinstall" "install"} coupon-code)))

(defn order-total-title [freeinstall-line-item-data]
  (if freeinstall-line-item-data
    "Hair + Install Total"
    "Total"))

(defn summary-total-section [{:keys [freeinstall-line-item-data order]}]
  [:div.h3.medium
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
           shipping-cost
           adjustments-including-tax
           promo-data
           subtotal] :as data} owner _]
  (component/create
   [:div {:data-test "cart-order-summary"}
    [:div.py1.bg-fate-white.px4
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
              price))))]]

     (when (orders/no-applied-promo? order)
       [:div.h5
        (promo-entry promo-data)])

     (when-not (orders/applied-install-promotion order)
       [:div.flex.py2
        "✋"
        [:div.flex.flex-column.px1
         [:div.purple.h5.medium
          "Don't miss out on "
          [:span.bold
           "free Mayvenn Install"]]
         [:div.h6
          "Save 10% & get a free install by a licensed stylist when you purchase 3 or more bundles.*"]
         [:div.flex.justify-left.py1
          [:div.col-4 (ui/teal-button {:height-class :small
                                       :class        "bold"
                                       :on-click     (utils/send-event-callback events/control-cart-add-freeinstall-coupon)} "add to cart")]
          [:div.col-4.teal.h7.flex.items-center
           (ui/button {:class    "inherit-color px4 py1 medium"
                       :on-click (utils/send-event-callback events/popup-show-adventure-free-install)} "learn more")]]
         [:div.h8.dark-gray
          "*Mayvenn Install cannot be combined with other promo codes."]]])]
    [:div.pt2.px4
     (summary-total-section data)]]))

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
