(ns checkout.confirmation
  (:require [catalog.images :as catalog-images]
            [checkout.templates.item-card :as item-card]
            [checkout.ui.cart-item :as cart-item]
            [checkout.ui.cart-summary :as cart-summary]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.adjustments :as adjustments]
            [storefront.accessors.mayvenn-install :as mayvenn-install]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.shipping :as shipping]
            [storefront.api :as api]
            [storefront.config :as config]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.trackings :as trackings]
            [storefront.hooks.quadpay :as quadpay]
            [storefront.hooks.stringer :as stringer]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [spice.date :as date]
            [ui.promo-banner :as promo-banner]
            [ui.molecules :as ui-molecules]))

(defn requires-additional-payment?
  [data]
  (let [no-stripe-payment?  (nil? (get-in data keypaths/order-cart-payments-stripe))
        store-credit-amount (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0)
        order-total         (get-in data keypaths/order-total)]
    (and
     ;; stripe can charge any amount
     no-stripe-payment?
     ;; is total covered by remaining store-credit?
     (> order-total store-credit-amount)
     (contains? (get-in data keypaths/checkout-selected-payment-methods) :store-credit))))

(defn checkout-button
  [selected-quadpay? {:keys [spinning? disabled?]}]
  (ui/submit-button (if selected-quadpay?
                      "Place Order with QuadPay"
                      "Place Order")
                    {:spinning? spinning?
                     :disabled? disabled?
                     :data-test "confirm-form-submit"}))

(defn checkout-button-query
  [data]
  (let [saving-card?   (checkout-credit-card/saving-card? data)
        has-order?     (get-in data keypaths/order)
        placing-order? (utils/requesting? data request-keys/place-order)
        placing-quadpay-order? (utils/requesting? data request-keys/update-cart-payments)]
    {:disabled? (or (utils/requesting? data request-keys/update-shipping-method)
                    (not has-order?))
     :spinning? (or saving-card? placing-order? placing-quadpay-order?)}))

(defmethod effects/perform-effects events/control-checkout-quadpay-confirmation-submit
  [_ _ _ app-state]
  (let [current-uri          (get-in app-state keypaths/navigation-uri)
        order-number         (get-in app-state keypaths/order-number)
        order-token          (get-in app-state keypaths/order-token)
        store-slug           (get-in app-state keypaths/store-slug)
        affiliate-stylist-id (some-> app-state
                                     (get-in keypaths/cookie)
                                     cookie-jar/retrieve-affiliate-stylist-id
                                     :affiliate-stylist-id
                                     spice/parse-int)]
    (api/update-cart-payments
     (get-in app-state keypaths/session-id)
     {:order {:number        order-number
              :token         order-token
              :cart-payments {:quadpay
                              {:status-url (str config/api-base-url "/hooks/quadpay_notifications"
                                                (when (and affiliate-stylist-id (= "shop" store-slug))
                                                  (str "?affiliate-stylist-id=" affiliate-stylist-id)))
                               :return-url
                               (str (assoc current-uri
                                           :path (str "/orders/" order-number "/quadpay")
                                           :query {:order-token order-token}))
                               :cancel-url (str (assoc current-uri :path "/cart?error=quadpay"))}}}}
     (fn [order]
       (messages/handle-message events/api-success-update-order-proceed-to-quadpay {:order order})))))

(defmethod trackings/perform-track events/api-success-update-order-proceed-to-quadpay
  [_ _ args app-state]
  (let [order-number (get-in app-state keypaths/order-number)
        store-slug   (get-in app-state keypaths/store-slug)
        order-total  (get-in app-state keypaths/order-total)
        redirect-url (-> (get-in app-state keypaths/order) :cart-payments :quadpay :redirect-url)]
    (stringer/track-event "customer-sent-to-quadpay"
                          {:order_number order-number
                           :store_slug   store-slug
                           :order_total  order-total}
                          events/external-redirect-quadpay-checkout
                          {:quadpay-redirect-url redirect-url})))

(defn ^:private servicing-stylist-banner-component
  [{:servicing-stylist-banner/keys [id heading title subtitle image-url rating]}]
  (when id
    [:div.px3
     [:div.h3.mb2 heading]
     [:div.flex.items-center.ml1.mb2 {:data-test id}
      [:div.mr4
       (ui/circle-picture {:width 50} (ui/square-image {:resizable-url image-url} 50))]
      [:div.flex-grow-1.line-height-1
       [:div.h5.medium.mbn1 title]
       [:div.h6 subtitle]
       [:div (ui.molecules/stars-rating-molecule rating)]]]]))

(defcomponent component
  [{:as   queried-data
    :keys [checkout-button-data
           checkout-steps
           cart-summary
           delivery
           free-install-applied?
           items
           site
           order
           loaded-quadpay?
           payment
           promo-banner
           requires-additional-payment?
           selected-quadpay?
           servicing-stylist
           freeinstall-cart-item]}
   owner _]
  [:div.container.p2
   (component/build promo-banner/sticky-organism promo-banner nil)
   (component/build checkout-steps/component checkout-steps nil)
   (if order
     [:form
      {:on-submit
       (when-not (or (:disabled? checkout-button-data) (:spinning? checkout-button-data))
         (if selected-quadpay?
           (utils/send-event-callback events/control-checkout-quadpay-confirmation-submit)
           (utils/send-event-callback events/control-checkout-confirmation-submit
                                      {:place-order? requires-additional-payment?})))}
      [:.clearfix.mxn3
       (servicing-stylist-banner-component queried-data)
       [:.col-on-tb-dt.col-6-on-tb-dt.px3
        [:.h3.left-align "Order Summary"]

        [:div.my2
         {:data-test "confirmation-line-items"}
         (component/build item-card/component items nil)
         (when freeinstall-cart-item
           (component/build cart-item/organism freeinstall-cart-item nil))]]

       [:div.col-on-tb-dt.col-6-on-tb-dt.px3
        (component/build checkout-delivery/component delivery nil)
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "p-color"
             :data-test "additional-payment-required-note"}
            [:div.p2.black
             "Please enter an additional payment method below for the remaining total on your order."])
           (component/build checkout-credit-card/component payment nil)])
        (component/build cart-summary/organism cart-summary nil)
        (component/build quadpay/component
                         {:quadpay/show?       (and selected-quadpay? loaded-quadpay?)
                          :quadpay/order-total (:total order)
                          :quadpay/directive   :continue-with}
                         nil)
        (when free-install-applied?
          [:div.h5.my4.center.col-10.mx-auto.line-height-3
           (let [servicing-stylist-name (stylists/->display-name servicing-stylist)]
             (cond
               (= :aladdin site)
               (str "After you place your order, please contact "
                    servicing-stylist-name
                    " to make your appointment.")
               servicing-stylist
               (str "After your order ships, you’ll be connected with "
                    servicing-stylist-name
                    " over SMS to make an appointment.")
               :else
               "You’ll be able to select your Certified Mayvenn Stylist after checkout."))])
        [:div.col-12.mx-auto.mt4
         (checkout-button selected-quadpay? checkout-button-data)]]]]
     [:div.py6.h2
      [:div.py4 (ui/large-spinner {:style {:height "6em"}})]
      [:h2.center "Processing your order..."]])])

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
                       :detail-bottom-right/value (str (mf/as-money price) " ea")
                       :detail-bottom-left/id     (str "line-item-quantity-" sku-id)
                       :detail-bottom-left/value  (str "Qty " (:quantity line-item))}))
                  (orders/product-items order))}))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn cart-summary-query
  [show-priority-shipping-method?
   {:as order :keys [adjustments]}
   {:mayvenn-install/keys [locked? applied? service-discount addon-services service-type]}
   available-store-credit]
  (when (seq order)
    (let [total              (-> order :total)
          tax                (:tax-total order)
          subtotal           (orders/products-and-services-subtotal order)
          shipping           (orders/shipping-item order)
          shipping-cost      (some->> shipping
                                      vector
                                      (apply (juxt :quantity :unit-price))
                                      (reduce *))
          timeframe-copy-fn  (if show-priority-shipping-method?
                               shipping/priority-shipping-experimental-timeframe
                               shipping/timeframe)
          shipping-timeframe (some-> shipping :sku timeframe-copy-fn)
          adjustment         (->> order :adjustments (map :price) (reduce + 0))
          total-savings      (- adjustment)
          wig-customization? (= :wig-customization service-type)]
      (cond-> {:cart-summary/id               "cart-summary"
               :cart-summary-total-line/id    "total"
               :cart-summary-total-line/label "Total"
               :cart-summary-total-line/value [:div (some-> total (- available-store-credit) (max 0) mf/as-money)]
               :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                             :cart-summary-line/label "Subtotal"
                                             :cart-summary-line/value (mf/as-money subtotal)}
                                            {:cart-summary-line/id       "shipping"
                                             :cart-summary-line/label    "Shipping"
                                             :cart-summary-line/sublabel shipping-timeframe
                                             :cart-summary-line/value    (mf/as-money-or-free shipping-cost)}]
                                           (for [{:keys [name price] :as adjustment}
                                                 (filter adjustments/non-zero-adjustment? adjustments)
                                                 :let [install-summary-line? (orders/service-line-item-promotion? adjustment)]]
                                             (cond-> {:cart-summary-line/id    (str (text->data-test-name name) "-adjustment")
                                                      :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                                   :height "2em" :width "2em"}]
                                                      :cart-summary-line/label (adjustments/display-adjustment-name adjustment)
                                                      :cart-summary-line/class "p-color"
                                                      :cart-summary-line/value (mf/as-money-or-free price)}

                                               install-summary-line?
                                               (merge {:cart-summary-line/value (mf/as-money-or-free service-discount)
                                                       :cart-summary-line/label (adjustments/display-service-line-item-adjustment-name adjustment)
                                                       :cart-summary-line/class "p-color"})))
                                           (when (pos? tax)
                                             [{:cart-summary-line/id       "tax"
                                               :cart-summary-line/label    "Tax"
                                               :cart-summary-line/value    (mf/as-money tax)}])
                                           (when (pos? available-store-credit)
                                             [{:cart-summary-line/id    "store-credit"
                                               :cart-summary-line/label "Store Credit"
                                               :cart-summary-line/class "p-color"
                                               :cart-summary-line/value (mf/as-money (- (min available-store-credit total)))}]))}

        (seq addon-services)
        (merge
         {:cart-item-sub-items/id    "addon-services"
          :cart-item-sub-items/title "Add-On Services"
          :cart-item-sub-items/items (map (fn [{:addon-service/keys [title price]}]
                                            {:cart-item-sub-item/title title
                                             :cart-item-sub-item/price price})
                                          addon-services)})

        applied?
        (merge {:cart-summary-total-incentive/id      "mayvenn-install"
                :cart-summary-total-incentive/label   "Includes Mayvenn Install"
                :cart-summary-total-incentive/savings (when (pos? total-savings)
                                                        (mf/as-money total-savings))})

        (and applied? wig-customization?)
        (merge {:cart-summary-total-incentive/id    "wig-customization"
                :cart-summary-total-incentive/label "Includes Wig Customization"})))))

(defn determine-site
  [app-state]
  (cond
    (= "mayvenn-classic" (get-in app-state keypaths/store-experience)) :classic
    (= "aladdin" (get-in app-state keypaths/store-experience))         :aladdin
    (= "shop" (get-in app-state keypaths/store-slug))                  :shop))

(defn query
  [data]
  (let [order                                   (get-in data keypaths/order)
        selected-quadpay?                       (-> (get-in data keypaths/order) :cart-payments :quadpay)
        {:mayvenn-install/keys [service-discount
                                applied?
                                stylist
                                service-type
                                service-title
                                addon-services
                                service-image-url]
         :as                   mayvenn-install} (mayvenn-install/mayvenn-install data)
        user                                    (get-in data keypaths/user)
        wig-customization?                      (= :wig-customization service-type)]
    (cond->
     {:order                        order
      :store-slug                   (get-in data keypaths/store-slug)
      :site                         (determine-site data)
      :requires-additional-payment? (requires-additional-payment? data)
      :promo-banner                 (promo-banner/query data)
      :checkout-steps               (checkout-steps/query data)
      :products                     (get-in data keypaths/v2-products)
      :payment                      (checkout-credit-card/query data)
      :delivery                     (checkout-delivery/query data)
      :free-install-applied?        applied?
      :checkout-button-data         (checkout-button-query data)
      :selected-quadpay?            selected-quadpay?
      :loaded-quadpay?              (get-in data keypaths/loaded-quadpay)
      :servicing-stylist            stylist
      :items                        (item-card-query data)
      :cart-summary                 (cart-summary-query (experiments/show-priority-shipping-method? data)
                                                        order
                                                        mayvenn-install
                                                        (orders/available-store-credit order user))}


      (seq addon-services)
      (maps/deep-merge
       {:freeinstall-cart-item
        {:cart-item
         {:cart-item-sub-items/id    "addon-services"
          :cart-item-sub-items/title "Add-On Services"
          :cart-item-sub-items/items (map (fn [{:addon-service/keys [title price]}]
                                            {:cart-item-sub-item/title title
                                             :cart-item-sub-item/price price})
                                          addon-services)}}})

      applied?
      (maps/deep-merge
       {:freeinstall-cart-item
        {:cart-item
         {:react/key                             "freeinstall-line-item-freeinstall"
          :cart-item-service-thumbnail/id        "freeinstall"
          :cart-item-service-thumbnail/image-url (or service-image-url ; GROT: when cellar deploy is done with service image
                                                     "//ucarecdn.com/3a25c870-fac1-4809-b575-2b130625d22a/")
          :cart-item-floating-box/id             "line-item-freeinstall-price"
          :cart-item-floating-box/value          [:div.flex.flex-column.justify-end
                                                  {:style {:height "100%"}}
                                                  (some-> service-discount - mf/as-money)]
          :cart-item-title/id                    "line-item-title-applied-mayvenn-install"
          :cart-item-title/primary               service-title
          :cart-item-title/secondary             [:div.line-height-3
                                                  "You’re all set! Shampoo, braiding and basic styling included."]}}})

      (and applied? wig-customization?)
      (maps/deep-merge
       {:freeinstall-cart-item
        {:cart-item
         {:cart-item-title/id        "line-item-title-applied-wig-customization"
          :cart-item-title/primary   "Wig Customization"
          :cart-item-title/secondary [:div.content-3
                                      "You're all set! Bleaching knots, tinting & cutting lace and hairline customization included."]}}})

      (and applied? stylist)
      (merge
       {:servicing-stylist-banner/id        "servicing-stylist-banner"
        :servicing-stylist-banner/heading   "Your Mayvenn Certified Stylist"
        :servicing-stylist-banner/title     (stylists/->display-name stylist)
        :servicing-stylist-banner/subtitle  (-> stylist :salon :name)
        :servicing-stylist-banner/rating    {:rating/value (:rating stylist)}
        :servicing-stylist-banner/image-url (some-> stylist :portrait :resizable-url)}))))

(defn ^:private built-non-auth-component [data opts]
  (component/build component (query data) opts))

(defn ^:export built-component
  [data opts]
  (checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout
   built-non-auth-component
   data
   opts))
