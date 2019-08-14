(ns checkout.consolidated-cart
  (:require
   #?@(:cljs [[storefront.components.popup :as popup]
              [storefront.components.order-summary :as summary]
              [storefront.config :as config]
              [cemerick.url :refer [url-encode]]
              [storefront.platform.messages :as messages]
              [storefront.components.payment-request-button :as payment-request-button]
              [storefront.api :as api]
              [storefront.history :as history]
              [storefront.hooks.browser-pay :as browser-pay]
              [storefront.browser.cookie-jar :as cookie-jar]
              [storefront.accessors.stylist-urls :as stylist-urls]
              [storefront.hooks.quadpay :as quadpay]
              [goog.labs.userAgent.device :as device]])
   [clojure.string :as string]
   [spice.core :as spice]
   [catalog.images :as catalog-images]
   [ui.molecules :as ui-molecules]
   [checkout.call-out :as call-out]
   [checkout.consolidated-cart.items :as cart-items]
   [checkout.header :as header]
   [checkout.suggestions :as suggestions]
   [checkout.ui.cart-summary :as cart-summary]
   [storefront.accessors.experiments :as experiments]
   [catalog.facets :as facets]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.products :as products]
   [storefront.accessors.promos :as promos]
   [storefront.accessors.stylists :as stylists]
   [storefront.component :as component]
   [storefront.components.flash :as flash]
   [storefront.components.footer :as storefront.footer]
   [storefront.components.money-formatters :as mf]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.css-transitions :as css-transitions]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [ui.promo-banner :as promo-banner]))

(defmethod effects/perform-effects events/control-cart-add-freeinstall-coupon
  [_ _ _ _ app-state]
  #?(:cljs
     (api/add-promotion-code {:shop?              (= "shop" (get-in app-state keypaths/store-slug))
                              :session-id         (get-in app-state keypaths/session-id)
                              :number             (get-in app-state keypaths/order-number)
                              :token              (get-in app-state keypaths/order-token)
                              :promo-code         "freeinstall"
                              :allow-dormant?     false
                              :consolidated-cart? (experiments/consolidated-cart? app-state)})))

(defn display-adjustable-line-items
  [recently-added-skus line-items skus update-line-item-requests delete-line-item-requests suggestions]
  (for [{sku-id :sku variant-id :id :as line-item} line-items

        :let [sku                  (get skus sku-id)
              price                (or (:sku/price line-item)         (:unit-price line-item))
              removing?            (get delete-line-item-requests variant-id)
              updating?            (get update-line-item-requests sku-id)
              just-added-to-order? (contains? recently-added-skus sku-id)
              length-circle-value  (-> sku :hair/length first)]]
    [:div.pt1.pb2.flex
     {:key (str sku-id "-" (:quantity line-item))}
     (suggestions/image-with-sticker {:cart-icon/ucare-id      (->> sku (catalog-images/image "cart") :ucare/id)
                                      :cart-icon/sku-id        (:catalog/sku-id sku)
                                      :cart-icon/sticker-label (when-let [length-circle-value (-> sku :hair/length first)]
                                                                 (str length-circle-value "”"))
                                      :cart-icon/sticker-id    (str "line-item-length-" (:catalog/sku-id sku))
                                      :cart-icon/sticker-size  "28px"
                                      :cart-icon/image-width   48
                                      :cart-icon/top-margin    "-8px"
                                      :cart-icon/left-margin   "-15px"
                                      :cart-icon/highlighted?  just-added-to-order?})
     [:div.flex-auto
      [:a.medium.titleize.h5
       {:data-test (str "line-item-title-" sku-id)}
       (or (:product-title line-item)
           (:product-name line-item))]
      [:div.h6
       [:div.flex.justify-between.mt1
        [:div
         {:data-test (str "line-item-color-" sku-id)}
         (:color-name line-item)]
        [:div.flex.items-center.justify-between
         (if removing?
           [:div.h3 {:style {:width "1.2em"}} ui/spinner]
           [:a.gray.medium
            (merge {:data-test (str "line-item-remove-" sku-id)}
                   (utils/fake-href events/control-cart-remove (:id line-item)))
            ^:inline (svg/trash-can {:height "1.1em"
                                     :width  "1.1em"
                                     :class  "stroke-dark-gray"})])]]
       [:div.flex.justify-between.mt1
        [:div.h3
         {:data-test (str "line-item-quantity-" sku-id)}
         (ui/auto-complete-counter {:spinning? updating?
                                    :data-test sku-id}
                                   (:quantity line-item)
                                   (utils/send-event-callback events/control-cart-line-item-dec
                                                              {:variant line-item})
                                   (utils/send-event-callback events/control-cart-line-item-inc
                                                              {:variant line-item}))]
        [:div.h5 {:data-test (str "line-item-price-ea-" sku-id)} (mf/as-money-without-cents price) " ea"]]]
      (component/build suggestions/consolidated-component suggestions nil)]]))

(defn ^:private non-adjustable-line-item
  [freeinstall-just-added? {:keys [removing? id title detail price remove-event thumbnail-image-fn]}]
  [:div.pt1.pb2.clearfix
   [:div.left.ml1.pr3
    (css-transitions/transition-background-color
     freeinstall-just-added?
     [:div.flex.justify-center.items-center
      {:style {:height "79px"
               :width  "79px"}}
      (thumbnail-image-fn 75)])]
   [:div
    [:a.medium.titleize.h5
     {:data-test (str "line-item-title-" id)}
     title]
    [:div.h6
     [:div.flex.justify-between.mt1
      [:div {:data-test (str "line-item-detail-" id)}
       detail]
      [:div.flex.items-center.justify-between
       (if removing?
         [:div.h3 {:style {:width "1.2em"}} ui/spinner]
         [:a.gray.medium
          (merge {:data-test (str "line-item-remove-" id)}
                 (apply utils/fake-href remove-event))
          ^:inline (svg/trash-can {:height "1.1em"
                                   :width  "1.1em"
                                   :class  "stroke-dark-gray"})])]]
     [:div.h5.right {:data-test (str "line-item-price-ea-" id)} (some-> price mf/as-money)]]]])

(defn full-component
  [{:keys [order
           skus
           promo-banner
           call-out checkout-disabled?
           redirecting-to-paypal?
           share-carts?
           requesting-shared-cart?
           suggestions
           line-items
           update-line-item-requests
           show-browser-pay?
           recently-added-skus
           delete-line-item-requests
           freeinstall-line-item-data
           freeinstall-just-added?
           number-of-items-needed
           loaded-quadpay?
           freeinstall-needs-more-items?
           remove-freeinstall-event] :as queried-data} owner _]
  (component/create
   [:div.container.p2
    (component/build promo-banner/sticky-organism promo-banner nil)

    (component/build call-out/component call-out nil)

    [:div.clearfix.mxn3
     [:div.px4 (ui-molecules/return-link queried-data)]
     [:div.hide-on-dt.border-top.border-light-gray.mt2.mb3]
     [:div.col-on-tb-dt.col-6-on-tb-dt.px3
      {:data-test "cart-line-items"}
      (display-adjustable-line-items recently-added-skus
                                     line-items
                                     skus
                                     update-line-item-requests
                                     delete-line-item-requests
                                     suggestions)

      ;; TODO does this below stay post consolidation?
      (when freeinstall-line-item-data
        (non-adjustable-line-item freeinstall-just-added? freeinstall-line-item-data))]

     [:div.col-on-tb-dt.col-6-on-tb-dt
      (component/build cart-summary/organism queried-data nil)

      [:div.px4
       #?@(:cljs
           [(component/build quadpay/component
                             {:show?       loaded-quadpay?
                              :order-total (:total order)
                              :directive   [:div.flex.items-center.justify-center
                                            "Just select"
                                            [:div.mx1 {:style {:width "70px" :height "14px"}}
                                             ^:inline (svg/quadpay-logo)]
                                            "at check out."]}
                             nil)])
       (ui/teal-button {:spinning? false
                        :disabled? checkout-disabled?
                        :on-click  (utils/send-event-callback events/control-checkout-cart-submit)
                        :data-test "start-checkout-button"}
                       [:div "Check out"])

       (when freeinstall-needs-more-items?
         [:div.error.h7.center.medium.py1
          (str "Add " number-of-items-needed " more items")])

       [:div.h5.black.center.py1.flex.justify-around.items-center
        [:div.flex-grow-1.border-bottom.border-light-gray]
        [:div.mx2 "or"]
        [:div.flex-grow-1.border-bottom.border-light-gray]]

       (if freeinstall-needs-more-items?
         [:a.teal.medium.center.mt1.mb2
          (apply utils/fake-href remove-freeinstall-event)
          "Checkout without a free Mayvenn Install"]
         [:div.pb2
          (ui/aqua-button {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                           :spinning? redirecting-to-paypal?
                           :disabled? checkout-disabled?
                           :data-test "paypal-checkout"}
                          [:div
                           "Check out with "
                           [:span.medium.italic "PayPal™"]])])]

      #?@(:cljs [(when show-browser-pay? (payment-request-button/built-component nil {}))])

      (when share-carts?
        [:div.py2
         [:div.h6.center.pt2.black.bold "Is this bag for a customer?"]
         (ui/navy-ghost-button {:on-click  (utils/send-event-callback events/control-cart-share-show)
                                :class     "border-width-2 border-navy"
                                :spinning? requesting-shared-cart?
                                :data-test "share-cart"}
                               [:div.flex.items-center.justify-center.bold
                                (svg/share-arrow {:class  "stroke-navy mr1 fill-navy"
                                                  :width  "24px"
                                                  :height "24px"})
                                "Share your bag"])])]]]))

(defn empty-component [{:keys [promotions aladdin?]} owner _]
  (component/create
   (ui/narrow-container
    [:div.p2
     [:.center {:data-test "empty-cart"}
      [:div.m2 ^:inline (svg/bag {:style {:height "70px" :width "70px"}
                                  :class "fill-black"})]

      [:p.m2.h2.light "Your bag is empty."]

      [:div.m2
       (let [promo (promos/default-advertised-promotion promotions)]
         (cond aladdin? promos/freeinstall-description
               promo    (:description promo)
               :else    promos/bundle-discount-description))]]

     (ui/teal-button (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
                     "Shop Our Looks")])))

(defn ^:private variants-requests [data request-key variant-ids]
  (->> variant-ids
       (map (juxt identity
                  #(utils/requesting? data (conj request-key %))))
       (into {})))

(defn ^:private update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(apply utils/requesting? data %)
          [request-keys/add-promotion-code
           [request-key-prefix request-keys/update-line-item]
           [request-key-prefix request-keys/delete-line-item]])))

(defn add-product-title-and-color-to-line-item [products facets line-item]
  (merge line-item {:product-title (->> line-item
                                        :sku
                                        (products/find-product-by-sku-id products)
                                        :copy/title)
                    :color-name    (-> line-item
                                       :variant-attrs
                                       :color
                                       (facets/get-color facets)
                                       :option/name)}))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn add-more-items-section
  [number-of-items-needed]
  [:div.p2.flex.flex-wrap
   [:div.col-5.h5 "Hair + Install Total"]
   [:div.col-7.h6.dark-gray.right-align
    "Add " number-of-items-needed
    " more " (ui/pluralize number-of-items-needed "hair item")
    " to calculate total price"]])

(defn summary-query [data]
  (let [freeinstall-line-item-data      (cart-items/freeinstall-line-item-query data)
        order                           (get-in data keypaths/order)
        shipping-item                   (orders/shipping-item order)
        shipping-cost                   (* (:quantity shipping-item)
                                           (:unit-price shipping-item))
        promo-data                      {:coupon-code   (get-in data keypaths/cart-coupon-code)
                                         :applying?     (utils/requesting? data request-keys/add-promotion-code)
                                         :focused       (get-in data keypaths/ui-focus)
                                         :error-message (get-in data keypaths/error-message)
                                         :field-errors  (get-in data keypaths/field-errors)}
        subtotal                        (cond-> (orders/products-subtotal order)
                                          freeinstall-line-item-data
                                          (+ (spice/parse-double (:price freeinstall-line-item-data))))
        adjustments                     (:adjustments order)
        freeinstall-promotion-on-order? (boolean (orders/applied-install-promotion order))
        freeinstall-entered?            (boolean (orders/freeinstall-entered? order))
        number-of-items-needed          (- 3 (orders/product-quantity order))
        order-adjustment-prices         (map :price (orders/all-order-adjustments order))
        install-discount                (- 0 (or (:price freeinstall-line-item-data) 0)) ;; TODO shop needs the servicing stylist menu
        total-savings                   (reduce + (conj order-adjustment-prices install-discount))]
    (merge
     {:freeinstall-informational/value (not freeinstall-promotion-on-order?)
      :cart-summary-total-line/id      "total"
      :cart-summary-total-line/label   (if freeinstall-promotion-on-order?
                                         "Hair + Install Total"
                                         "Total")
      :cart-summary-total-line/value   (cond
                                         (orders/freeinstall-applied? order)
                                         [:div
                                          [:div.bold.h2
                                           (some-> order :total mf/as-money)]
                                          [:div.h6.bg-purple.white.px2.nowrap.mb1
                                           "Includes Mayvenn Install"]
                                          (when (neg? total-savings)
                                            [:div.h6.light.dark-gray.pxp1.nowrap.italic
                                             "You've saved "
                                             [:span.bold {:data-test "total-savings"}
                                              (mf/as-money total-savings)]])]

                                         freeinstall-entered?
                                         [:div.h7.dark-gray.light
                                          "Add " number-of-items-needed
                                          " more " (ui/pluralize number-of-items-needed "item")
                                          " to "
                                          [:br]
                                          " calculate total price"]

                                         :else [:div (some-> order :total mf/as-money)])

      :cart-summary/id    "cart-summary"
      :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                    :cart-summary-line/label "Subtotal"
                                    :cart-summary-line/value (mf/as-money subtotal)}]

                                  (when shipping-cost
                                    [{:cart-summary-line/id    "shipping"
                                      :cart-summary-line/label "Shipping"
                                      :cart-summary-line/value (mf/as-money-or-free shipping-cost)}])

                                  (for [{:keys [name price coupon-code]} (filter (fn non-zero-adjustment? [{:keys [price coupon-code]}]
                                                                                   (or (not (= price 0))
                                                                                       (#{"amazon" "freeinstall" "install"} coupon-code)))
                                                                                 adjustments)
                                        :let                             [freeinstall-promo? (= "freeinstall" coupon-code)]]
                                    (cond-> {:cart-summary-line/id    (text->data-test-name name)
                                             :cart-summary-line/icon  (svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                         :height "2em" :width "2em"})
                                             :cart-summary-line/label (orders/display-adjustment-name name)
                                             :cart-summary-line/value (mf/as-money-or-free price)}

                                      ;; TODO shop needs the servicing stylist menu
                                      (and freeinstall-line-item-data freeinstall-promo?)
                                      (merge {:cart-summary-line/value (mf/as-money-or-free install-discount)
                                              :cart-summary-line/class "bold purple"})

                                      (and coupon-code (not freeinstall-promo?))
                                      (merge {:cart-summary-line/action-id     "cart-remove-promo"
                                              :cart-summary-line/action-icon   (svg/close-x {:class "stroke-white fill-gray"})
                                              :cart-summary-line/action-target [events/control-checkout-remove-promotion {:code coupon-code}]}))))}

     (when (and (orders/no-applied-promo? order) (not freeinstall-entered?))
       {:promo-data promo-data}))))

(defn full-cart-query [data]
  (let [order                                (get-in data keypaths/order)
        products                             (get-in data keypaths/v2-products)
        facets                               (get-in data keypaths/v2-facets)
        line-items                           (map (partial add-product-title-and-color-to-line-item products facets)
                                                  (orders/product-items order))
        variant-ids                          (map :id line-items)
        freeinstall-line-item-data           (cart-items/freeinstall-line-item-query data)
        freeinstall-entered-cart-incomplete? (and (orders/freeinstall-entered? order)
                                                  (not (orders/freeinstall-applied? order)))
        number-of-items-needed               (- 3 (orders/product-quantity order))]
    (merge {:suggestions                (suggestions/consolidated-query data)
            :order                      order
            :line-items                 line-items
            :skus                       (get-in data keypaths/v2-skus)
            :products                   products
            :promo-banner               (when (zero? (orders/product-quantity order))
                                          (promo-banner/query data))
            :call-out                   (call-out/query data)
            :checkout-disabled?         (or freeinstall-entered-cart-incomplete?
                                            (update-pending? data))
            :redirecting-to-paypal?     (get-in data keypaths/cart-paypal-redirect)
            :share-carts?               (stylists/own-store? data)
            :requesting-shared-cart?    (utils/requesting? data request-keys/create-shared-cart)
            :loaded-quadpay?            (get-in data keypaths/loaded-quadpay)
            :show-browser-pay?          (and (get-in data keypaths/loaded-stripe)
                                             (experiments/browser-pay? data)
                                             (seq (get-in data keypaths/shipping-methods))
                                             (seq (get-in data keypaths/states)))
            :update-line-item-requests  (merge-with
                                         #(or %1 %2)
                                         (variants-requests data request-keys/add-to-bag (map :sku line-items))
                                         (variants-requests data request-keys/update-line-item (map :sku line-items)))
            :delete-line-item-requests  (variants-requests data request-keys/delete-line-item variant-ids)
            :recently-added-skus        (get-in data keypaths/cart-recently-added-skus)
            :freeinstall-just-added?    (get-in data keypaths/cart-freeinstall-just-added?)
            :stylist-service-menu       (get-in data keypaths/stylist-service-menu)
            :freeinstall-line-item-data freeinstall-line-item-data

            :return-link/copy              "Continue Shopping"
            :return-link/event-message     [events/control-open-shop-escape-hatch]
            :number-of-items-needed        number-of-items-needed
            :freeinstall-needs-more-items? (and (boolean (orders/freeinstall-entered? order))
                                                (pos? number-of-items-needed))
            :remove-freeinstall-event      [events/control-checkout-remove-promotion {:code "freeinstall"}]}

           (summary-query data))))

(defn empty-cart-query
  [data]
  {:promotions (get-in data keypaths/promotions)
   :aladdin?   (experiments/aladdin-experience? data)})

(defn component
  [{:keys [fetching-order?
           item-count
           empty-cart
           full-cart]} owner opts]
  (component/create
   (if fetching-order?
     [:div.py3.h2 ui/spinner]
     [:div.col-7-on-dt.mx-auto
      (if (zero? item-count)
        (component/build empty-component empty-cart opts)
        (component/build full-component full-cart opts))])))

(defn query [data]
  {:fetching-order? (utils/requesting? data request-keys/get-order)
   :item-count      (orders/product-quantity (get-in data keypaths/order))
   :empty-cart      (empty-cart-query data)
   :full-cart       (full-cart-query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn layout [data nav-event]
  [:div.flex.flex-column {:style {:min-height    "100vh"
                                  :margin-bottom "-1px"}}
   #?(:cljs (popup/built-component data nil))

   (header/built-component data nil)
   (when (zero? (orders/product-quantity (get-in data keypaths/order)))
     (promo-banner/built-static-organism data nil))
   [:div.relative.flex.flex-column.flex-auto
    (flash/built-component data nil)

    [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
     (built-component data nil)]

    [:footer
     (storefront.footer/built-component data nil)]]])
