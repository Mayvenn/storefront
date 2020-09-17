(ns checkout.confirmation
  (:require api.orders
            [catalog.images :as catalog-images]
            [checkout.shop.cart-v202004 :as cart]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [checkout.ui.cart-summary-v202004 :as cart-summary]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.adjustments :as adjustments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.images :as images]
            [storefront.accessors.stylists :as stylists]
            [storefront.api :as api]
            [storefront.config :as config]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.accessors.line-items :as line-items]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :as mf]
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
            [ui.promo-banner :as promo-banner]))

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

(defcomponent component
  [{:as   queried-data
    :keys [checkout-button-data
           checkout-steps
           cart-summary
           delivery
           cart-items
           order
           loaded-quadpay?
           payment
           promo-banner
           requires-additional-payment?
           selected-quadpay?
           servicing-stylist
           freeinstall-cart-item
           service-line-items]}
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
       [:div
        [:div.bg-refresh-gray.p3.col-on-tb-dt.col-6-on-tb-dt.bg-white-on-tb-dt
         (when (seq service-line-items)
           [:div
            [:div.title-2.proxima.mb1 "Services"]
            [:div
             [:div.mbn1
              (component/build cart-item-v202004/stylist-organism queried-data nil)
              (component/build cart-item-v202004/no-stylist-organism queried-data nil)]

             ;; TODO: Separate mayvenn install base into its own service line item key
             (for [service-line-item service-line-items]
               [:div
                [:div.mt2-on-mb
                 (component/build cart-item-v202004/organism {:cart-item service-line-item}
                                  (component/component-id (:react/key service-line-item)))]])]
            [:div.border-bottom.border-gray.hide-on-mb]])

         (when (seq cart-items)
           [:div.mt3
            [:div.title-2.proxima.mb1
             "Items"]

            [:div
             {:data-test "confirmation-line-items"}

             (for [[index cart-item] (map-indexed vector cart-items)
                   :let [react-key (:react/key cart-item)]
                   :when react-key]
               [:div
                {:key (str index "-cart-item-" react-key)}
                (when-not (zero? index)
                  [:div.flex.bg-white
                   [:div.ml2 {:style {:width "75px"}}]
                   [:div.flex-grow-1.border-bottom.border-cool-gray.ml-auto.mr2]])
                (component/build cart-item-v202004/organism {:cart-item  cart-item}
                                 (component/component-id (str index "-cart-item-" react-key)))])]])]]

       [:div.col-on-tb-dt.col-6-on-tb-dt
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

        [:div.mx3
         [:div.col-12.mx-auto.mt4
          (checkout-button selected-quadpay? checkout-button-data)]]

        (when (seq service-line-items)
          [:div.p3.content-3.flex.items-center
           (ui/ucare-img {:width "56px"
                          :class "mtp2"} "9664879b-07e0-432e-9c09-b2cf4c899b10")
           [:div.px1
            (if servicing-stylist
              (str "You will be connected to "
                   (stylists/->display-name servicing-stylist)
                   " over SMS to make an appointment within 2 business days after the check out")
              "You’ll be able to select your Certified Mayvenn Stylist after checkout.")]])]]]
     [:div.py6.h2
      [:div.py4 (ui/large-spinner {:style {:height "6em"}})]
      [:h2.center "Processing your order..."]])])

(defn item-card-query
  [data]
  (let [order               (get-in data keypaths/order)
        skus                (get-in data keypaths/v2-skus)
        images              (get-in data keypaths/v2-images)
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
                       :image/value               (->> sku (catalog-images/image images "cart") :ucare/id)
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

(defn ^:private cart-summary-query
  [{:as order :keys [adjustments]}
   {:free-mayvenn-service/keys [service-item discounted]}
   addon-skus
   available-store-credit]
  (when (seq order)
    (let [total               (-> order :total)
          tax                 (:tax-total order)
          subtotal            (orders/products-and-services-subtotal order)
          adjustment          (->> order :adjustments (map :price) (reduce + 0))
          total-savings       (- adjustment)
          service-discounted? discounted
          service-name        (:variant-name service-item)
          wig-customization?  (orders/wig-customization? order)]
      (cond-> {:cart-summary/id               "cart-summary"
               :cart-summary-total-line/id    "total"
               :cart-summary-total-line/label "Total"
               :cart-summary-total-line/value ^:ignore-interpret-warning [:div (some-> total (- available-store-credit) (max 0) mf/as-money)]
               :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                             :cart-summary-line/label "Subtotal"
                                             :cart-summary-line/value (mf/as-money subtotal)}]

                                           (when-let [shipping-method-summary-line
                                                      (cart/shipping-method-summary-line-query
                                                       (orders/shipping-item order)
                                                       (orders/product-and-service-items order))]
                                             [shipping-method-summary-line])

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
                                               (merge {:cart-summary-line/id    "free-service-adjustment"
                                                       :cart-summary-line/value (mf/as-money-or-free (- (line-items/service-line-item-price service-item)))
                                                       :cart-summary-line/label (str "Free " service-name)})))
                                           (when (pos? tax)
                                             [{:cart-summary-line/id       "tax"
                                               :cart-summary-line/label    "Tax"
                                               :cart-summary-line/value    (mf/as-money tax)}])
                                           (when (pos? available-store-credit)
                                             [{:cart-summary-line/id    "store-credit"
                                               :cart-summary-line/label "Store Credit"
                                               :cart-summary-line/class "p-color"
                                               :cart-summary-line/value (mf/as-money (- (min available-store-credit total)))}]))}

        (seq addon-skus)
        (merge
         {:cart-item-sub-items/id    "addon-services"
          :cart-item-sub-items/title "Add-On Services"
          :cart-item-sub-items/items (map (fn [addon-sku]
                                            {:cart-item-sub-item/title  (:sku/title addon-sku)
                                             :cart-item-sub-item/price  (some-> addon-sku :sku/price mf/as-money)
                                             :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                          addon-skus)})

        service-discounted?
        (merge {:cart-summary-total-incentive/id      "mayvenn-install"
                :cart-summary-total-incentive/label   "Includes Mayvenn Service"
                :cart-summary-total-incentive/savings (when (pos? total-savings)
                                                        (mf/as-money total-savings))})

        (and service-discounted? wig-customization?)
        (merge {:cart-summary-total-incentive/id    "wig-customization"
                :cart-summary-total-incentive/label "Includes Wig Customization"})))))

(defn free-service-line-items-query
  [data
   {:free-mayvenn-service/keys [service-item]}
   addon-skus]
  (let [sku-catalog        (get-in data storefront.keypaths/v2-skus)
        sku-id             (:sku service-item)
        service-sku        (get sku-catalog sku-id)
        wig-customization? (= "SRV-WGC-000" sku-id)
        images-catalog     (get-in data storefront.keypaths/v2-images)]
    (when (:id service-item)
      [(merge {:react/key                             "freeinstall-line-item-freeinstall"
               :cart-item-title/id                    "linr-item-title-upsell-free-service"
               :cart-item-floating-box/id             "line-item-freeinstall-price"
               :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                                        :value (str "You're all set! " (:copy/whats-included service-sku))}
                                                       {:id    (str "line-item-quantity-" sku-id)
                                                        :value (str "qty. " (:quantity service-item))}]
               :cart-item-floating-box/value          (some-> service-item
                                                              line-items/service-line-item-price
                                                              mf/as-money)
               :cart-item-service-thumbnail/id        "freeinstall"
               :cart-item-service-thumbnail/image-url (->> service-sku
                                                           (images/skuer->image images-catalog "cart")
                                                           :url)}
              (if wig-customization?
                {:cart-item-title/id      "line-item-title-applied-wig-customization"
                 :cart-item-title/primary "Wig Customization"}
                {:cart-item-title/id      "line-item-title-applied-mayvenn-install"
                 :cart-item-title/primary (:variant-name service-item)})
              (when (seq addon-skus)
                {:cart-item-sub-items/id    "addon-services"
                 :cart-item-sub-items/title "Add-On Services"
                 ;; think about sharing this function
                 :cart-item-sub-items/items (map (fn [addon-sku]
                                                   {:cart-item-sub-item/title  (:sku/title addon-sku)
                                                    :cart-item-sub-item/price  (some-> addon-sku :sku/price mf/as-money)
                                                    :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                                 addon-skus)}))])))

(defn ^:private standalone-service-line-items-query
  [app-state]
  (let [skus                          (get-in app-state keypaths/v2-skus)
        images                        (get-in app-state keypaths/v2-images)
        service-line-items            (orders/service-line-items (get-in app-state keypaths/order))
        standalone-service-line-items (filter line-items/standalone-service? service-line-items)]
    (for [{sku-id :sku :as service-line-item} standalone-service-line-items
          :let
          [sku   (get skus sku-id)
           price (or (:sku/price service-line-item)
                     (:unit-price service-line-item))]]
      {:react/key                             sku-id
       :cart-item-title/primary               (or (:product-title service-line-item)
                                                  (:product-name service-line-item))
       :cart-item-title/id                    (str "line-item-" sku-id)
       :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                                :value (:copy/whats-included sku)}
                                               {:id    (str "line-item-quantity-" sku-id)
                                                :value (str "qty. " (:quantity service-line-item))}]
       :cart-item-floating-box/id             (str "line-item-" sku-id "-price")
       :cart-item-floating-box/value          (some-> price mf/as-money)
       :cart-item-service-thumbnail/id        sku-id
       :cart-item-service-thumbnail/image-url (->> sku (catalog-images/image images "cart") :ucare/id)})))

(defn cart-items-query
  [app-state line-items skus]
  (let [images (get-in app-state keypaths/v2-images)
        cart-items
        (for [{sku-id :sku :as line-item} line-items
              :let
              [sku                  (get skus sku-id)
               price                (or (:sku/price line-item)
                                        (:unit-price line-item))]]
          {:react/key                                (str sku-id "-" (:quantity line-item))
           :cart-item-title/id                       (str "line-item-title-" sku-id)
           :cart-item-title/primary                  (or (:product-title line-item)
                                                         (:product-name line-item))
           :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                       :value (str "qty. " (:quantity line-item))}]
           :cart-item-title/secondary                (:color-name line-item)
           :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" sku-id)
           :cart-item-floating-box/value             ^:ignore-interpret-warning [:div {:data-test (str "line-item-price-ea-" sku-id)}
                                                                                 (mf/as-money price)
                                                                                 [:div.proxima.content-4 " each"]]
           :cart-item-square-thumbnail/id            sku-id
           :cart-item-square-thumbnail/sku-id        sku-id
           :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (-> sku :hair/length first)]
                                                       (str length-circle-value "”"))
           :cart-item-square-thumbnail/ucare-id      (->> sku (catalog-images/image images "cart") :ucare/id)})]
    cart-items))

(defn query
  [data]
  (let [order                                           (get-in data keypaths/order)
        selected-quadpay?                               (-> order :cart-payments :quadpay)
        {service-items     :services/items
         servicing-stylist :services/stylist}           (api.orders/services data order)
        {:keys                    [service-item]
         free-service-discounted? :free-mayvenn-service/discounted?
         :as                      free-mayvenn-service} (api.orders/free-mayvenn-service servicing-stylist order)
        services-on-order?                              (seq service-items)
        wig-customization?                              (orders/wig-customization? (get-in data keypaths/order))
        user                                            (get-in data keypaths/user)
        skus                                            (get-in data keypaths/v2-skus)
        images-catalog                                  (get-in data storefront.keypaths/v2-images)
        products                                        (get-in data keypaths/v2-products)
        facets                                          (get-in data keypaths/v2-facets)
        physical-line-items                             (map (partial cart/add-product-title-and-color-to-line-item products facets)
                                                             (orders/product-items order))
        addon-service-line-items                        (->> order
                                                             orders/service-line-items
                                                             (filter (comp boolean #{"addon"} :service/type :variant-attrs)))
        addon-service-skus                              (map (fn [addon-service] (get skus (:sku addon-service))) addon-service-line-items)]
    (cond->
        {:order                        order
         :store-slug                   (get-in data keypaths/store-slug)
         :requires-additional-payment? (requires-additional-payment? data)
         :promo-banner                 (promo-banner/query data)
         :checkout-steps               (checkout-steps/query data)
         :products                     (get-in data keypaths/v2-products)
         :payment                      (checkout-credit-card/query data)
         :delivery                     (checkout-delivery/query data)
         :checkout-button-data         (checkout-button-query data)
         :selected-quadpay?            selected-quadpay?
         :loaded-quadpay?              (get-in data keypaths/loaded-quadpay)
         :servicing-stylist            servicing-stylist
         :cart-items                   (cart-items-query data physical-line-items skus)
         :service-line-items           (concat
                                        (free-service-line-items-query data free-mayvenn-service addon-service-skus)
                                        (standalone-service-line-items-query data))
         :cart-summary                 (cart-summary-query order
                                                           free-mayvenn-service
                                                           addon-service-skus
                                                           (orders/available-store-credit order user))}
      (seq addon-service-skus)
      (maps/deep-merge
       {:freeinstall-cart-item
        {:cart-item
         {:cart-item-sub-items/id    "addon-services"
          :cart-item-sub-items/title "Add-On Services"
          :cart-item-sub-items/items (map (fn [addon-sku]
                                            {:cart-item-sub-item/title  (:sku/title addon-sku)
                                             :cart-item-sub-item/price  (some-> addon-sku :sku/price mf/as-money)
                                             :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                          addon-service-skus)}}})

      free-service-discounted?
      (maps/deep-merge
       {:freeinstall-cart-item
        {:cart-item
         {:react/key                             "freeinstall-line-item-freeinstall"
          :cart-item-service-thumbnail/id        "freeinstall"
          :cart-item-service-thumbnail/image-url (or (->> (get skus (:sku service-item))
                                                          (images/skuer->image images-catalog "cart")
                                                          :url) ; GROT: when cellar deploy is done with service image
                                                     "//ucarecdn.com/3a25c870-fac1-4809-b575-2b130625d22a/")
          :cart-item-floating-box/id             "line-item-freeinstall-price"
          :cart-item-floating-box/value          ^:ignore-interpret-warning [:div.flex.flex-column.justify-end
                                                                             {:style {:height "100%"}}
                                                                             (some-> (line-items/service-line-item-price service-item)
                                                                                     mf/as-money)]
          :cart-item-title/id                    "line-item-title-applied-mayvenn-install"
          :cart-item-title/primary               (:variant-name service-item)
          :cart-item-title/secondary             [:div.line-height-3 "You’re all set! " (:copy/whats-included service-item)]}}})

      (and free-service-discounted? wig-customization?)
      (maps/deep-merge
       {:freeinstall-cart-item
        {:cart-item
         {:cart-item-title/id        "line-item-title-applied-wig-customization"
          :cart-item-title/primary   "Wig Customization"
          :cart-item-title/secondary [:div.content-3 "You're all set! " (:copy/whats-included service-item)]}}})

      (and services-on-order? servicing-stylist)
      (merge
       {:servicing-stylist-banner/id        "servicing-stylist-banner"
        :servicing-stylist-banner/heading   "Your Mayvenn Certified Stylist"
        :servicing-stylist-banner/title     (stylists/->display-name servicing-stylist)
        :servicing-stylist-banner/subtitle  (-> servicing-stylist :salon :name)
        :servicing-stylist-banner/rating    {:rating/value (:rating servicing-stylist)
                                             :rating/id    "stylist-rating-id"}
        :servicing-stylist-banner/image-url (some-> servicing-stylist :portrait :resizable-url)}))))

(defn ^:private built-non-auth-component [data opts]
  (component/build component (query data) opts))

(defn ^:export built-component
  [data opts]
  (checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout
   built-non-auth-component
   data
   opts))
