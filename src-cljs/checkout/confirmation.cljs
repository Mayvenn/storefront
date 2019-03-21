(ns checkout.confirmation
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.money-formatters :as mf]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.order-summary :as summary]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.ui :as ui]
            [adventure.checkout.cart.items :as adventure-cart-items]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [spice.core :as spice]
            [spice.maps :as maps]
            [clojure.string :as string]
            [catalog.images :as catalog-images]
            [checkout.templates.item-card :as item-card]
            [checkout.cart.items :as cart-items]
            [checkout.confirmation.summary :as confirmation-summary]))

(defn requires-additional-payment? [data]
  (let [no-stripe-payment?  (nil? (get-in data keypaths/order-cart-payments-stripe))
        store-credit-amount (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0)
        order-total         (get-in data keypaths/order-total)]
    (and
     ;; stripe can charge any amount
     no-stripe-payment?
     ;; is total covered by remaining store-credit?
     (> order-total store-credit-amount))))

(defn checkout-button [{:keys [spinning? disabled?]}]
  (ui/submit-button "Place Order" {:spinning? spinning?
                                   :disabled? disabled?
                                   :data-test "confirm-form-submit"}))

(defn checkout-button-query [data]
  (let [order                   (get-in data keypaths/order)

        saving-card?   (checkout-credit-card/saving-card? data)
        placing-order? (utils/requesting? data request-keys/place-order)]
    {:disabled?              (utils/requesting? data request-keys/update-shipping-method)
     :spinning?              (or saving-card? placing-order?)}))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn ^:private display-freeinstall-line-item
  [{:keys [id price title thumbnail-image-fn detail]}]
  [:div.clearfix.border-top.border-bottom.border-gray.py3
   [:a.left.mr1
    [:div.block.border.border-gray.rounded.hide-on-mb
     (thumbnail-image-fn 117)]
    [:div.block.border.border-gray.rounded.hide-on-tb-dt
     (thumbnail-image-fn 132)]]
   [:div.overflow-hidden
    [:div.ml1
     [:a.medium.titleize.h5 {:data-test (str "line-item-title-" id)}
      title]
     [:div.h6.mt1.line-height-1
      (if (empty? detail)
        [:div.mb1.mt0 (str "w/ " "a Certified Mayvenn Stylist")
         [:ul.h6.list-img-purple-checkmark.pl4.mt1
          (mapv (fn [%] [:li %])
                ["Licensed Salon Stylist" "Near you" "Experienced"])]]
        detail)]]]])

(defn component
  [{:keys [available-store-credit
           checkout-steps
           payment delivery order
           items
           requires-additional-payment?
           promotion-banner
           install-or-free-install-applied?
           confirmation-summary
           checkout-button-data]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (component/build promotion-banner/sticky-component promotion-banner nil)
     (om/build checkout-steps/component checkout-steps)

     [:form
      {:on-submit
       (utils/send-event-callback events/control-checkout-confirmation-submit
                                  {:place-order? requires-additional-payment?})}

      [:.clearfix.mxn3
       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        [:.h3.left-align "Order Summary"]

        [:div.my2
         {:data-test "confirmation-line-items"}
         (component/build item-card/component items nil)]]

       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        (om/build checkout-delivery/component delivery)
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (if confirmation-summary
          (component/build confirmation-summary/component confirmation-summary {})
          (summary/display-order-summary order
                                         {:read-only?             true
                                          :use-store-credit?      (not install-or-free-install-applied?)
                                          :available-store-credit available-store-credit}))
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (checkout-button checkout-button-data)]]]]])))

(defn item-card-query
  [data]
  (let [order               (get-in data keypaths/order)
        skus                (get-in data keypaths/v2-skus)
        facets              (maps/index-by :facet/slug (get-in data keypaths/v2-facets))
        color-options->name (->> facets
                                 :hair/color
                                 :facet/options
                                 (maps/index-by :option/slug)
                                 (maps/map-values :option/name))]
    {:items (mapv (fn [{sku-id :sku :as line-item}]
                    (let [sku   (get skus sku-id)
                          price (:unit-price line-item)]
                      {:react/key                 (str (:id line-item)
                                                       "-"
                                                       (:catalog/sku-id sku)
                                                       "-"
                                                       (:quantity line-item))
                       :circle/id                 (str "line-item-length-" sku-id)
                       :circle/value              (-> sku :hair/length first (str "”"))
                       :image/id                  (str "line-item-img-" (:catalog/sku-id sku))
                       :image/value               (->> sku (catalog-images/image "cart") :ucare/id)
                       :title/id                  (str "line-item-title-" sku-id)
                       :title/value               (or (:product-title line-item)
                                                      (:product-name line-item))
                       :detail-top-left/id        (str "line-item-color-" sku-id)
                       :detail-top-left/value     (-> sku :hair/color first color-options->name str)
                       :detail-bottom-right/id    (str "line-item-price-ea-" sku-id)
                       :detail-bottom-right/value (str (mf/as-money-without-cents price) " ea")
                       :detail-bottom-left/id     (str "line-item-quantity-" sku-id)
                       :detail-bottom-left/value  (str "Qty " (:quantity line-item))}))
                  (orders/product-items order))}))

(defn adventure-component
  [{:keys [available-store-credit
           items
           checkout-steps
           payment delivery order
           requires-additional-payment?
           promotion-banner
           install-or-free-install-applied?
           confirmation-summary
           checkout-button-data
           servicing-stylist]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (component/build promotion-banner/sticky-component promotion-banner nil)
     (om/build checkout-steps/component checkout-steps)

     [:form
      {:on-submit
       (utils/send-event-callback events/control-checkout-confirmation-submit
                                  {:place-order? requires-additional-payment?})}

      [:.clearfix.mxn3
       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        [:.h3.left-align "Order Summary"]

        [:div.my2
         {:data-test "confirmation-line-items"}
         (component/build item-card/component items nil)]]

       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        (om/build checkout-delivery/component delivery)
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (if confirmation-summary
          (component/build confirmation-summary/component confirmation-summary {})
          (summary/display-order-summary order
                                         {:read-only?             true
                                          :use-store-credit?      (not install-or-free-install-applied?)
                                          :available-store-credit available-store-credit}))
        [:div.h5.my4.center.col-10.mx-auto.line-height-3
         (if-let [servicing-stylist-firstname (-> servicing-stylist :address :firstname)]
           (str "You’ll be connected with " servicing-stylist-firstname " after checkout.")
           "You’ll be able to select your Certified Mayvenn Stylist after checkout.")]
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (checkout-button checkout-button-data)]]]]])))

(defn- absolute-url [& path]
  (apply str (.-protocol js/location) "//" (.-host js/location) path))

(defn query [data]
  (let [order                      (get-in data keypaths/order)
        freeinstall-line-item-data (cart-items/freeinstall-line-item-query data)
        freeinstall-applied? (orders/freeinstall-applied? order)]
    {:requires-additional-payment?     (requires-additional-payment? data)
     :promotion-banner                 (promotion-banner/query data)
     :checkout-steps                   (checkout-steps/query data)
     :products                         (get-in data keypaths/v2-products)
     :items                            (cond-> (item-card-query data)
                                         freeinstall-applied?
                                         (update
                                          :items conj
                                          {:react/key             (:id freeinstall-line-item-data)
                                           :title/value           (:title freeinstall-line-item-data)
                                           :title/id              (:id freeinstall-line-item-data)
                                           :detail-top-left/id    "freeinstall-details"
                                           :detail-top-left/value (:detail freeinstall-line-item-data)
                                           :image/id              "freeinstall-needle-thread"
                                           :image/value           (:thumbnail-image freeinstall-line-item-data)}))
     :order                            order
     :payment                          (checkout-credit-card/query data)
     :delivery                         (checkout-delivery/query data)
     :install-or-free-install-applied? freeinstall-applied?
     :available-store-credit           (get-in data keypaths/user-total-available-store-credit)
     :checkout-button-data             (checkout-button-query data)
     :confirmation-summary             (confirmation-summary/query data)
     :store-slug                       (get-in data keypaths/store-slug)
     :freeinstall?                     (= "freeinstall" (get-in data keypaths/store-slug))}))

(defn adventure-query [data]
  (let [order                      (get-in data keypaths/order)
        freeinstall-line-item-data (adventure-cart-items/freeinstall-line-item-query data)]
    {:requires-additional-payment?     (requires-additional-payment? data)
     :promotion-banner                 (promotion-banner/query data)
     :checkout-steps                   (checkout-steps/query data)
     :products                         (get-in data keypaths/v2-products)
     :items                            (update (item-card-query data)
                                               :items conj
                                               {:react/key             (:id freeinstall-line-item-data)
                                                :title/value           (:title freeinstall-line-item-data)
                                                :title/id              (:id freeinstall-line-item-data)
                                                :detail-top-left/id    "freeinstall-details"
                                                :detail-top-left/value (:detail freeinstall-line-item-data)
                                                :image/id              "freeinstall-needle-thread"
                                                :image/value           (:thumbnail-image freeinstall-line-item-data)})
     :order                            order
     :payment                          (checkout-credit-card/query data)
     :delivery                         (checkout-delivery/query data)
     :install-or-free-install-applied? (orders/freeinstall-applied? order)
     :available-store-credit           (get-in data keypaths/user-total-available-store-credit)
     :checkout-button-data             (checkout-button-query data)
     :confirmation-summary             (confirmation-summary/query data)
     :store-slug                       (get-in data keypaths/store-slug)
     :servicing-stylist                (get-in data adventure.keypaths/adventure-servicing-stylist)
     :freeinstall?                     (= "freeinstall" (get-in data keypaths/store-slug))}))

(defn built-component [data opts]
  (let [query-data           (query data)
        query-adventure-data (adventure-query data)
        freeinstall?         (:freeinstall? query-data)]
    (if freeinstall?
      (om/build adventure-component query-adventure-data opts)
      (om/build component query-data opts))))
