(ns checkout.shop.filled-cart-v2020-09
  (:require
   #?@(:cljs [[storefront.components.payment-request-button :as payment-request-button]
              [storefront.components.popup :as popup]
              [storefront.hooks.quadpay :as quadpay]])
   spice.selector
   api.orders
   [checkout.header :as header]
   [checkout.suggestions :as suggestions]
   [checkout.ui.cart-item-v202004 :as cart-item-v202004]
   [checkout.ui.cart-summary-v202004 :as cart-summary-v202004]
   [clojure.string :as string]
   [spice.core :as spice]
   [storefront.accessors.adjustments :as adjustments]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.auth :as auth]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.line-items :as line-items]
   [storefront.accessors.stylists :as stylists]
   [storefront.accessors.shipping :as shipping]
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.flash :as flash]
   [storefront.components.footer :as storefront.footer]
   [storefront.components.money-formatters :as mf]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [ui.molecules :as ui-molecules]
   [ui.promo-banner :as promo-banner]))

;; selectors

(def ^:private select
  (comp seq (partial spice.selector/match-all {:selector/strict? true})))

(def ^:private ?physical
  {:catalog/department #{"hair" "stylist-exclusives"}})

(def ^:private ?wig
  {:catalog/department #{"hair"}
   :hair/family        #{"ready-wigs" "360-wigs" "lace-front-wigs"}})

(def ^:private ?service
  {:catalog/department #{"service"}})

(def ^:private ?discountable
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{true}})

(def ^:private ?a-la-carte
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{false}})

(def ^:private ?recent
  {:item/recent? #{true}})

;; page selectors - maybe they can just be cats

(def ^:private mayvenn-install-category
  {:page/slug           "mayvenn-install"
   :catalog/category-id "23"})

(def ^:private wig-category
  {:page/slug           "wigs"
   :catalog/category-id "13"})

;; this might just be an extension - e.g. :item/image

(defn ^:private hacky-cart-image
  [item]
  (some->> item
           :selector/images
           (filter #(= "cart" (:use-case %)))
           first
           :url
           ui/ucare-img-id))

;; session helpers, might be better as a model

(defn ^:private variants-requests [data request-key variant-ids]
  (->> variant-ids
       (map (juxt identity
                  #(utils/requesting? data (conj request-key %))))
       (into {})))

(defn ^:private update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(apply utils/requesting? data %)
          [request-keys/add-promotion-code
           [request-key-prefix request-keys/add-to-bag]
           [request-key-prefix request-keys/update-line-item]
           [request-key-prefix request-keys/delete-line-item]])))

;; formatters

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

;;; -----------------------------------

(defn physical-items-component
  [physical-items suggestions]
  [:div
   [:div.title-2.proxima.mb1 "Items"]
   (if (seq physical-items)
     (for [[index cart-item] (map-indexed vector physical-items)
           :let              [react-key (:react/key cart-item)]
           :when             react-key]
       [:div
        {:key (str index "-cart-item-" react-key)}
        (when-not (zero? index)
          [:div.flex.bg-white
           [:div.ml2 {:style {:width "75px"}}]
           [:div.flex-grow-1.border-bottom.border-cool-gray.ml-auto.mr2]])
        (component/build cart-item-v202004/organism {:cart-item   cart-item
                                                     :suggestions (when (zero? index)
                                                                    suggestions)}
                         (component/component-id (str index "-cart-item-" react-key)))])
     [:div.mt2
      (component/build cart-item-v202004/no-items {}
                       (component/component-id "no-items"))])])

(defn service-items-component
  [{:keys [service-line-items stylist]}]
  (when (seq service-line-items)
    [:div.mb3
     [:div.title-2.proxima.mb1 "Services"]
     (component/build cart-item-v202004/stylist-organism stylist nil)
     (component/build cart-item-v202004/no-stylist-organism stylist nil)

     ;; TODO: Separate mayvenn install base into its own service line item key
     (for [service-line-item service-line-items]
       [:div {:key (:react/key service-line-item)}
        [:div.mt2-on-mb
         (component/build cart-item-v202004/organism {:cart-item service-line-item}
                          (component/component-id (:react/key service-line-item)))]])
     [:div.border-bottom.border-gray.hide-on-mb]]))

(defcomponent full-component
  [{:keys [promo-banner
           service-items
           physical-items
           cart-summary
           suggestions
           cta
           shared-cart
           paypal
           quadpay
           browser-pay?
           checkout-caption]}
   _ _]
  [:div.container.px2
   (component/build promo-banner/sticky-organism promo-banner nil)
   [:div.clearfix.mxn3
    [:div
     [:div.bg-refresh-gray.p3.col-on-tb-dt.col-6-on-tb-dt.bg-white-on-tb-dt
      (service-items-component service-items)
      (physical-items-component physical-items suggestions)]]

    [:div.col-on-tb-dt.col-6-on-tb-dt.bg-refresh-gray.bg-white-on-mb.mbj1
     (component/build cart-summary-v202004/organism cart-summary nil)
     [:div.px4.center.bg-white-on-mb ; Checkout buttons
      #?@(:cljs
          [(component/build quadpay/component quadpay nil)])

      ;; Caption
      (when-let [{:keys [checkout-caption-copy servicing-stylist-portrait-url]} checkout-caption]
        [:div.flex.h6.items-center.mtj1
         {:data-test "checkout-caption"}
         [:div.pr2
          (ui/circle-picture
           {:width 50}
           ;; Note: We are not using ucare-id because stylist portraits may have
           ;; ucarecdn crop parameters saved into the url
           (ui/square-image {:resizable-url servicing-stylist-portrait-url} 50))]
         [:div.left-align
          checkout-caption-copy]])

      ;; CTAs for checking out
      [:div.py2
       ;; cta button
       (let [{:cta/keys [id content disabled? target]} cta]
         (when id
           (ui/button-large-primary {:data-test id
                                     :disabled? disabled?
                                     :on-click  (apply utils/send-event-callback target)}
                                    content)))

       ;; paypal button
       (let [{:keys [redirecting-to-paypal?]} paypal]
         [:div
          [:div.h5.black.py1.flex.items-center
           [:div.flex-grow-1.border-bottom.border-gray]
           [:div.mx2 "or"]
           [:div.flex-grow-1.border-bottom.border-gray]]

          [:div
           (ui/button-large-paypal
            {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
             :spinning? redirecting-to-paypal?
             :data-test "paypal-checkout"}
            (component/html
             [:div
              "Check out with "
              [:span.medium.italic "PayPal™"]]))]])

       ;; browser pay
       #?(:cljs (when browser-pay?
                  (payment-request-button/built-component nil
                                                          {})))]]

     (when-let [{:keys [requesting-shared-cart?]} shared-cart]
       [:div.py2.px4
        [:div.content-2.center.pt2
         "Is this bag for a customer?"]
        (ui/button-large-secondary
         {:on-click  (utils/send-event-callback events/control-cart-share-show)
          :spinning? requesting-shared-cart?
          :data-test "share-cart"}
         [:div.flex.items-center.justify-center.bold
          (svg/share-arrow
           {:class  "stroke-black mr2 fill-black"
            :width  "18px"
            :height "18px"})
          "Share your bag"])])]]])

(defcomponent template
  [{:keys [header footer popup flash cart nav-event]} _ _]
  [:div.flex.flex-column.stretch
   {:style {:margin-bottom "-1px"}}
   #?(:cljs (popup/built-component popup nil))

   (header/built-component header nil)
   [:div.relative.flex.flex-column.flex-auto
    (flash/built-component flash nil)

    [:main.bg-white.flex-auto
     {:data-test (keypaths/->component-str nav-event)}
     [:div
      [:div.hide-on-tb-dt
       [:div.border-bottom.border-gray.border-width-1.m-auto.col-7-on-dt
        [:div.px2.my2 (ui-molecules/return-link (:return-link cart))]]]
      [:div.hide-on-mb
       [:div.m-auto.container
        [:div.px2.my2 (ui-molecules/return-link (:return-link cart))]]]
      [:div.col-7-on-dt.mx-auto
       (component/build full-component cart)]]]
    [:footer
     (storefront.footer/built-component footer nil)]]])

;;; --------------

(defn free-services<-
  [items delete-line-item-requests]
  (for [{:as                         free-service
         :catalog/keys               [sku-id]
         :item/keys                  [id variant-name quantity unit-price]
         :item.service/keys          [addons]
         :promo.mayvenn-install/keys [hair-missing-quantity requirement-copy]
         :copy/keys                  [whats-included]
         :legacy/keys                [variant-id]}

        (select ?discountable items)]
    (cond-> {:react/key                               "freeinstall-line-item-freeinstall"
             :cart-item-title/id                      "line-item-title-upsell-free-service"
             :cart-item-title/primary                 variant-name
             :cart-item-copy/lines                    [{:id    (str "line-item-whats-included-" sku-id)
                                                        :value (if (pos? hair-missing-quantity)
                                                                 requirement-copy
                                                                 (str "You're all set! " whats-included))}
                                                       {:id    (str "line-item-quantity-" sku-id)
                                                        :value (str "qty. " quantity)}]
             :cart-item-floating-box/id               "line-item-freeinstall-price"
             :cart-item-floating-box/value            (some-> (* quantity unit-price) mf/as-money)
             :cart-item-remove-action/id              "line-item-remove-freeinstall"
             :cart-item-remove-action/spinning?       (boolean (get delete-line-item-requests id))
             :cart-item-remove-action/target          [events/control-cart-remove variant-id]
             :cart-item-service-thumbnail/id          "freeinstall"
             :cart-item-service-thumbnail/image-url   (hacky-cart-image free-service)
             :cart-item-modify-button/id              "browse-addons"
             :cart-item-modify-button/target          [events/control-show-addon-service-menu]
             :cart-item-modify-button/tracking-target [events/browse-addon-service-menu-button-enabled]
             :cart-item-modify-button/content         "+ Browse Add-Ons"}

      (seq addons)
      (merge {:cart-item-modify-button/content "Edit Add-Ons"
              :cart-item-sub-items/id          "addon-services"
              :cart-item-sub-items/title       "Add-On Services"
              :cart-item-sub-items/items       (map (fn [addon-sku]
                                                      {:cart-item-sub-item/title  (:sku/title addon-sku)
                                                       :cart-item-sub-item/price  (some-> addon-sku :sku/price mf/as-money)
                                                       :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                              addons)}))))

(defn ^:private a-la-carte-services<-
  [items delete-line-item-requests]
  (for [{:as           item
         :catalog/keys [sku-id]
         :hacky/keys   [cart-title]
         :sku/keys     [price]
         :copy/keys    [whats-included]
         :item/keys    [id quantity unit-price product-name]}

        (select ?a-la-carte items)]
    {:react/key                             sku-id
     :cart-item-title/primary               (or cart-title product-name)
     :cart-item-title/id                    (str "line-item-" sku-id)
     :cart-item-floating-box/id             (str "line-item-" sku-id "-price")
     :cart-item-floating-box/value          (some-> (or price unit-price) mf/as-money)
     :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                              :value whats-included}
                                             {:id    (str "line-item-quantity-" sku-id)
                                              :value (str "qty. " quantity)}]
     :cart-item-remove-action/id            (str "line-item-remove-" sku-id)
     :cart-item-remove-action/spinning?     (boolean (get delete-line-item-requests id))
     :cart-item-remove-action/target        [events/control-cart-remove id]
     :cart-item-service-thumbnail/id        sku-id
     :cart-item-service-thumbnail/image-url (hacky-cart-image item)}))

(defn service-items<-
  [items delete-line-item-requests]
  (when-let [services (select ?service items)]
    ;; COREY the order/session model should have currently selected stylist at an order level?
    (let [{:item.service/keys [stylist]} (first services)]
      {:stylist            (if stylist
                             {:servicing-stylist-portrait-url     (-> stylist :portrait :resizable-url)
                              :servicing-stylist-banner/id        "servicing-stylist-banner"
                              :servicing-stylist-banner/title     (stylists/->display-name stylist)
                              :servicing-stylist-banner/rating    {:rating/value (:rating stylist)
                                                                   :rating/id    "stylist-rating-id"}
                              :servicing-stylist-banner/image-url (some-> stylist :portrait :resizable-url)
                              :servicing-stylist-banner/target    [events/control-change-stylist {:stylist-id (:stylist-id stylist)}]
                              :servicing-stylist-banner/action-id "stylist-swap"}
                             {:stylist-organism/id            "stylist-organism"
                              :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})
       :service-line-items (concat
                            (free-services<- items delete-line-item-requests)
                            (a-la-carte-services<- items delete-line-item-requests))})))

(defn physical-items<-
  [items update-line-item-requests delete-line-item-requests]
  (for [{:as           item
         :catalog/keys [sku-id]
         :sku/keys     [price]
         :item/keys    [id quantity unit-price product-name]
         :hacky/keys   [cart-title]
         :join/keys    [facets]}

        (select ?physical items)
        :let
        [qty-adjustment-args {:variant {:id id :sku sku-id}}
         removing?            (get delete-line-item-requests id)
         updating?            (get update-line-item-requests sku-id)]]
    {:react/key                                      (str sku-id "-" quantity)
     :cart-item-title/id                             (str "line-item-title-" sku-id)
     :cart-item-title/primary                        (or cart-title product-name)
     :cart-item-title/secondary                      (some-> facets :hair/color :option/name)
     :cart-item-floating-box/id                      (str "line-item-price-ea-with-label-" sku-id)
     :cart-item-floating-box/value                   ^:ignore-interpret-warning [:div {:data-test (str "line-item-price-ea-" sku-id)}
                                                                                 (mf/as-money (or price unit-price))
                                                                                 ^:ignore-interpret-warning
                                                                                 [:div.proxima.content-4 " each"]]
     :cart-item-square-thumbnail/id                  sku-id
     :cart-item-square-thumbnail/sku-id              sku-id
     :cart-item-square-thumbnail/sticker-label       (when-let [length-circle-value (some-> item :hair/length first)]
                                                       (str length-circle-value "”"))
     :cart-item-square-thumbnail/ucare-id            (hacky-cart-image item)
     :cart-item-adjustable-quantity/id               (str "line-item-quantity-" sku-id)
     :cart-item-adjustable-quantity/spinning?        updating?
     :cart-item-adjustable-quantity/value            quantity
     :cart-item-adjustable-quantity/id-suffix        sku-id
     :cart-item-adjustable-quantity/decrement-target [events/control-cart-line-item-dec qty-adjustment-args]
     :cart-item-adjustable-quantity/increment-target [events/control-cart-line-item-inc qty-adjustment-args]
     :cart-item-remove-action/id                     (str "line-item-remove-" sku-id)
     :cart-item-remove-action/spinning?              removing?
     :cart-item-remove-action/target                 [events/control-cart-remove id]}))

(defn coupon-code->remove-promo-action [coupon-code]
  {:cart-summary-line/action-id     "cart-remove-promo"
   :cart-summary-line/action-icon   [:svg/close-x {:class "stroke-white fill-gray" }]
   :cart-summary-line/action-target [events/control-checkout-remove-promotion {:code coupon-code}]})

(defn shipping-method-summary-line-query
  [shipping-method non-shipping-line-items]
  (let [free-shipping? (= "WAITER-SHIPPING-1" (:sku shipping-method))
        only-services? (every? line-items/service? non-shipping-line-items)]
    (when (and shipping-method (not (and free-shipping? only-services?)))
      {:cart-summary-line/id       "shipping"
       :cart-summary-line/label    "Shipping"
       :cart-summary-line/sublabel (-> shipping-method :sku shipping/timeframe)
       :cart-summary-line/value    (->> shipping-method
                                        vector
                                        (apply (juxt :quantity :unit-price))
                                        (reduce * 1)
                                        mf/as-money-or-free)})))

(defn regular-cart-summary-query
  "This is for cart's that haven't entered an upsell (free install, wig customization, etc)"
  [{:as order :keys [adjustments tax-total total]}]
  (let [subtotal (orders/products-and-services-subtotal order)]
    {:cart-summary-total-line/id    "total"
     :cart-summary-total-line/label "Total"
     :cart-summary-total-line/value ^:ignore-interpret-warning [:div (some-> total mf/as-money)]

     :cart-summary/id    "cart-summary"
     :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                   :cart-summary-line/label "Subtotal"
                                   :cart-summary-line/value (mf/as-money subtotal)}]

                                 (when-let [shipping-method-summary-line
                                            (shipping-method-summary-line-query
                                             (orders/shipping-item order)
                                             (orders/product-and-service-items order))]
                                   [shipping-method-summary-line])

                                 (for [{:keys [name price coupon-code] :as adjustment}
                                       (filter adjustments/non-zero-adjustment? adjustments)]
                                   (cond-> {:cart-summary-line/id    (text->data-test-name name)
                                            :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                         :height "2em" :width "2em"} ]
                                            :cart-summary-line/label (adjustments/display-adjustment-name adjustment)
                                            :cart-summary-line/class "p-color"
                                            :cart-summary-line/value (mf/as-money-or-free price)}

                                     coupon-code
                                     (merge (coupon-code->remove-promo-action coupon-code))))

                                 (when (pos? tax-total)
                                   [{:cart-summary-line/id    "tax"
                                     :cart-summary-line/label "Tax"
                                     :cart-summary-line/value (mf/as-money tax-total)}]))}))

(defn upsold-cart-summary-query
  "The cart has an upsell 'entered' because the customer has requested a service discount"
  [{:as order :keys [adjustments]} free-mayvenn-service]
  (let [wig-customization?  (orders/wig-customization? order)
        service-item-price  (- (* (:item/quantity free-mayvenn-service)
                                  (:item/unit-price free-mayvenn-service)))
        total               (:total order)
        tax                 (:tax-total order)
        subtotal            (orders/products-and-services-subtotal order)
        order-adjustment    (->> order :adjustments (map :price) (reduce + 0))
        total-savings       (- order-adjustment)
        service-discounted? (= (->> free-mayvenn-service :item/applied-promotions (map :amount) (reduce + 0))
                               service-item-price)]
    (cond->
        {:cart-summary/id                 "cart-summary"
         :cart-summary-total-line/id      "total"
         :cart-summary-total-line/label   (if (and free-mayvenn-service
                                                   (not wig-customization?))
                                            "Hair + Install Total"
                                            "Total")
         :cart-summary-total-line/value   ^:ignore-interpret-warning [:div (some-> total mf/as-money)]
         :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                       :cart-summary-line/label "Subtotal"
                                       :cart-summary-line/value (mf/as-money subtotal)}]


                                     (when-let [shipping-method-summary-line
                                                (shipping-method-summary-line-query
                                                 (orders/shipping-item order)
                                                 (orders/product-and-service-items order))]
                                       [shipping-method-summary-line])

                                     (for [{:keys [name price coupon-code] :as adjustment}
                                           (filter adjustments/non-zero-adjustment? adjustments)
                                           :let [install-summary-line? (orders/service-line-item-promotion? adjustment)]]
                                       (cond-> {:cart-summary-line/id    (str (text->data-test-name name) "-adjustment")
                                                :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                             :height "2em" :width "2em"}]
                                                :cart-summary-line/label (adjustments/display-adjustment-name adjustment)
                                                :cart-summary-line/class "p-color"
                                                :cart-summary-line/value (mf/as-money-or-free price)}

                                         install-summary-line?
                                         (merge
                                          {:cart-summary-line/id    "free-service-adjustment"
                                           :cart-summary-line/value (mf/as-money-or-free service-item-price)
                                           :cart-summary-line/label (str "Free " (:item/variant-name free-mayvenn-service))

                                           :cart-summary-line/action-id     "cart-remove-promo"
                                           :cart-summary-line/action-icon   [:svg/close-x {:class "stroke-white fill-gray"}]
                                           :cart-summary-line/action-target [events/order-remove-freeinstall-line-item]})

                                         coupon-code
                                         (merge (coupon-code->remove-promo-action coupon-code))))

                                     (when (pos? tax)
                                       [{:cart-summary-line/id    "tax"
                                         :cart-summary-line/label "Tax"
                                         :cart-summary-line/value (mf/as-money tax)}]))}

      service-discounted?
      (merge {:cart-summary-total-incentive/id    "mayvenn-install"
              :cart-summary-total-incentive/label "Includes Mayvenn Service"

              :cart-summary-total-incentive/savings (when (pos? total-savings)
                                                      (mf/as-money total-savings))})

      (and service-discounted? wig-customization?)
      (merge {:cart-summary-total-incentive/id    "wig-customization"
              :cart-summary-total-incentive/label "Includes Wig Customization"}))))

(defn promo-input<-
  [data order pending-requests?]
  (when (orders/no-applied-promo? order)
    (let [keypath                 keypaths/cart-coupon-code
          value                   (get-in data keypath)
          promo-link?             (experiments/promo-link? data)
          show-promo-code-field?  (or (not promo-link?)
                                      (get-in data keypaths/promo-code-entry-open?))
          disabled?               (or pending-requests? (empty? value))
          promo-code-field-errors (get (get-in data keypaths/field-errors) ["promo-code"])
          input-group-attrs       {:text-input-attrs
                                   {:errors        promo-code-field-errors
                                    :value         (or value "")
                                    :keypath       keypath
                                    :label         "enter promocode"
                                    :data-test     "promo-code"
                                    :id            "promo-code"
                                    :wrapper-class "col-12 bg-white"
                                    :type          "text"}
                                   :button-attrs
                                   {:args    {:data-test "cart-apply-promo"
                                              :disabled? disabled?
                                              :on-click  (utils/send-event-callback events/control-cart-update-coupon)
                                              :style     {:width   "55px"
                                                          :padding "0"}}
                                    :content (svg/forward-arrow {:class (if disabled?
                                                                          "fill-gray"
                                                                          "fill-white")
                                                                 :style {:width  "14px"
                                                                         :height "14px"}})}}]
      {:promo-field-data
       (cond-> {}

         show-promo-code-field?
         (merge input-group-attrs)

         promo-link?
         (merge
          {:field-reveal/id     "reveal-promo-entry"
           :field-reveal/target [events/control-toggle-promo-code-entry]
           :field-reveal/label  (if show-promo-code-field?
                                  "Hide promo code"
                                  "Add promo code")}))})))

(defn freeinstall-informational<-
  [order]
  (when-not (orders/discountable-services-on-order? order)
    (let [any-wig? (orders/any-wig? order)]
      (cond->
          {:freeinstall-informational/button-id             "add-free-mayvenn-service"
           :freeinstall-informational/primary               "Don't miss out on free Mayvenn Services"
           :freeinstall-informational/secondary             "Get a free service by a licensed stylist when you purchase qualifying items"
           :freeinstall-informational/cta-label             "Add Mayvenn Service"
           :freeinstall-informational/cta-target            [events/navigate-category
                                                             {:catalog/category-id "31"
                                                              :page/slug           "free-mayvenn-services"}]
           :freeinstall-informational/id                    "freeinstall-informational"
           :freeinstall-informational/secondary-link-id     "cart-learn-more"
           :freeinstall-informational/secondary-link-target [events/popup-show-consolidated-cart-free-install]
           :freeinstall-informational/fine-print            "*Mayvenn Services cannot be combined with other promo codes."
           :freeinstall-informational/secondary-link-label  "learn more"}

        any-wig?
        (merge
         {:freeinstall-informational/button-id             "add-wig-customization"
          :freeinstall-informational/primary               "Don't miss out on free Wig Customization"
          :freeinstall-informational/secondary             "Get a free customization by a licensed stylist when you add a Wig Customization to your cart below."
          :freeinstall-informational/cta-label             "Add Wig Customization"
          :freeinstall-informational/cta-target            [events/navigate-product-details {:catalog/product-id "223" :page/slug "wig-customization" :query-params {:SKU "SRV-WGC-000"}}]
          :freeinstall-informational/secondary-link-id     "Learn More"
          :freeinstall-informational/secondary-link-target [events/popup-show-wigs-customization]
          :freeinstall-informational/fine-print            "*Wig Customization cannot be combined with other promo codes, and excludes Ready to Wear Wigs"})))))

(defn cart-summary<-
  [order items]
  (if-let [free-service (first (select ?discountable items))]
    (upsold-cart-summary-query order free-service)
    (regular-cart-summary-query order)))

(defn cta<-
  [pending-requests?]
  {:cta/id        "start-checkout-button"
   :cta/disabled? pending-requests?
   :cta/target    [events/control-checkout-cart-submit]
   :cta/content   "Check out"})

(defn return-link<-
  [items]
  (let [recent-texture (some->> (select (merge ?recent ?physical) items)
                                (mapv :hair/texture)
                                first
                                (assoc-in {} [:query-params :subsection]))]
    {:return-link/id            "continue-shopping"
     :return-link/copy          "Continue Shopping"
     :return-link/event-message [events/navigate-category
                                 (if (select ?wig items)
                                   wig-category
                                   (merge mayvenn-install-category
                                          recent-texture))]}))

(defn quadpay<-
  [data order]
  {:quadpay/order-total (:total order)
   :quadpay/show?       (get-in data keypaths/loaded-quadpay)
   :quadpay/directive   :just-select})

(defn paypal<-
  [data]
  {:redirecting-to-paypal? (get-in data keypaths/cart-paypal-redirect)})

(defn checkout-caption<-
  ;; TODO(corey) This name seems confusing to me
  [items]
  (when-let [services (select ?service items)]
    (if-let [stylist (:item.service/stylist (first services))]
      {:checkout-caption-copy          (str "You will be connected to "
                                            (stylists/->display-name stylist)
                                            " over SMS to make an appointment within 2 business days after the check out")
       :servicing-stylist-portrait-url (-> stylist :portrait :resizable-url)}
      {:checkout-caption-copy          "You'll be able to select your Mayvenn Certified Stylist after checkout."
       :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})))

;; TODO(corey) This only uses session
(defn shared-cart<-
  [data]
  (let [{signed-in-as ::auth/as} (auth/signed-in data)]
    (when (= :stylist signed-in-as)
      {:requesting-shared-cart? (utils/requesting? data request-keys/create-shared-cart)})))

(defn ^:export page
  [app-state nav-event]
  (let [order                 (get-in app-state keypaths/order)
        {:order/keys [items]} (api.orders/current app-state)

        ;; TODO(corey) these are session
        pending-requests?         (update-pending? app-state)
        update-line-item-requests (merge-with #(or %1 %2)
                                              (->> (map :catalog/sku-id items)
                                                   (variants-requests app-state request-keys/add-to-bag))
                                              (->> (map :catalog/sku-id items)
                                                   (variants-requests app-state request-keys/update-line-item)))
        delete-line-item-requests (->> (map :item/id items)
                                       (variants-requests app-state request-keys/delete-line-item))

        ;; TODO(corey) part item model / part order model
        suggestions (suggestions/consolidated-query app-state)]
    (component/build template
                     {:cart      {:return-link      (return-link<- items)
                                  :promo-banner     (when (zero? (orders/product-quantity order))
                                                      (promo-banner/query app-state))
                                  :cta              (cta<- pending-requests?)
                                  :physical-items   (physical-items<- items
                                                                      update-line-item-requests
                                                                      delete-line-item-requests)
                                  :service-items    (service-items<- items
                                                                     delete-line-item-requests)
                                  :checkout-caption (checkout-caption<- items)
                                  :cart-summary     (merge (cart-summary<- order items)
                                                           (freeinstall-informational<- order)
                                                           (promo-input<- app-state
                                                                          order
                                                                          pending-requests?))
                                  :shared-cart      (shared-cart<- app-state)
                                  :quadpay          (quadpay<- app-state order)
                                  :paypal           (paypal<- app-state)
                                  :browser-pay?     (and (get-in app-state keypaths/loaded-stripe)
                                                         (experiments/browser-pay? app-state)
                                                         (seq (get-in app-state keypaths/shipping-methods))
                                                         (seq (get-in app-state keypaths/states)))
                                  :suggestions      suggestions}
                      :header    app-state
                      :footer    app-state
                      :popup     app-state
                      :flash     app-state
                      :nav-event nav-event})))
