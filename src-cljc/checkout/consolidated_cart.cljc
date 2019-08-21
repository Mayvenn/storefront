(ns checkout.consolidated-cart
  (:require
   #?@(:cljs [[om.core :as om]
              [storefront.api :as api]
              [storefront.components.payment-request-button :as payment-request-button]
              [storefront.components.popup :as popup]
              [storefront.confetti :as confetti]
              [storefront.hooks.quadpay :as quadpay]])
   [adventure.keypaths :as adventure-keypaths]
   [catalog.facets :as facets]
   [catalog.images :as catalog-images]
   [checkout.accessors.vouchers :as vouchers]
   [checkout.call-out :as call-out]
   [checkout.header :as header]
   [checkout.suggestions :as suggestions]
   [checkout.ui.cart-item :as cart-item]
   [checkout.ui.cart-summary :as cart-summary]
   [clojure.string :as string]
   [spice.core :as spice]
   [storefront.accessors.experiments :as experiments]
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
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [ui.molecules :as ui-molecules]
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

(defn qualified-banner-component
  [_ owner _]
  (let [burstable? (atom true)]
    #?(:clj [:div]
       :cljs
       (reify
         om/IDidMount
         (did-mount [_]
           (confetti/burst (om/get-ref owner "qualified-banner-confetti")))
         om/IRender
         (render [_]
           (component/html
            [:div.flex.items-center.bold
             {:data-test "qualified-banner"
              :style     {:height              "246px"
                          :padding-top         "43px"
                          :background-size     "cover"
                          :background-position "center"
                          :background-image    "url('//ucarecdn.com/97d80a16-1f48-467a-b8e2-fb16b532b75e/-/format/auto/-/quality/normal/aladdinMatchingCelebratoryOverlayImagePurpleR203Lm3x.png')"}
              :on-click (fn [_]
                          (when @burstable?
                            (reset! burstable? false)
                            (.then (confetti/burst (om/get-ref owner "qualified-banner-confetti"))
                                   #(reset! burstable? true))))}
             [:div.col.col-12.center.white
              [:div.absolute
               {:ref   "qualified-banner-confetti"
                :style {:left  "50%"
                        :right "50%"
                        :top   "25%"}}]
              [:div.h5.light "This order qualifies for a"]
              [:div.h1.shout "free install"]
              [:div.h5.light "from a Mayvenn Stylist near you"]]]))))))

(defn full-component
  [{:keys [applied?
           call-out
           cart-items
           cart-summary
           checkout-disabled?
           loaded-quadpay?
           locked?
           order
           promo-banner
           quantity-remaining
           redirecting-to-paypal?
           remove-freeinstall-event
           requesting-shared-cart?
           share-carts?
           show-browser-pay?
           suggestions] :as queried-data} owner _]
  (component/create
   [:div.container.p2
    (component/build promo-banner/sticky-organism promo-banner nil)

    (component/build call-out/component call-out nil)

    [:div.clearfix.mxn3
     [:div.px4 (ui-molecules/return-link queried-data)]
     [:div.hide-on-dt.border-top.border-light-gray.mt2.mb3]
     (when applied?
       (list ;; HACK: here until we get a desktop style pass
        [:div.hide-on-dt.mtn3]
        [:div.mb3
         (component/build qualified-banner-component nil nil)]))
     [:div.col-on-tb-dt.col-6-on-tb-dt.px3
      {:data-test "cart-line-items"}
      ;; HACK: have suggestions be paired with appropriate cart item
      (map-indexed
       (fn [index cart-item]
         (component/build cart-item/organism {:cart-item cart-item
                                              :suggestions (when (zero? index)
                                                             suggestions)}))
       cart-items)]

     [:div.col-on-tb-dt.col-6-on-tb-dt
      (component/build cart-summary/organism cart-summary nil)

      [:div.px4.center
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

       (when locked?
         [:div.error.h7.center.medium.py1
          (str "Add " quantity-remaining (ui/pluralize quantity-remaining " more item"))])

       [:div.h5.black.py1.flex.items-center
        [:div.flex-grow-1.border-bottom.border-light-gray]
        [:div.mx2 "or"]
        [:div.flex-grow-1.border-bottom.border-light-gray]]

       (if locked?
         [:a.teal.medium.mt1.mb2
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

(def default-service-menu
  "Install prices to use when a stylist has not yet been selected."
  {:advertised-sew-in-360-frontal "225.0"
   :advertised-sew-in-closure     "175.0"
   :advertised-sew-in-frontal     "200.0"
   :advertised-sew-in-leave-out   "150.0"})

(defn ^:private mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views"
  [app-state]
  (let [order                       (get-in app-state keypaths/order)
        freeinstall-entered?        (boolean (orders/freeinstall-entered? order))
        install-items-required      3
        items-added-for-install     (->> order
                                         :shipments
                                         first
                                         :line-items
                                         (filter #(-> %
                                                      :variant-attrs
                                                      :bundle-discount-eligible?))
                                         (map :quantity)
                                         (apply +)
                                         (min install-items-required))
        items-remaining-for-install (- install-items-required items-added-for-install)
        servicing-stylist           (get-in app-state adventure-keypaths/adventure-servicing-stylist)
        service-type                (->> (get-in app-state keypaths/environment)
                                         vouchers/campaign-configuration
                                         (filter #(= (:service/type %)
                                                     (some-> (orders/product-items order)
                                                             vouchers/product-items->highest-value-service)))
                                         first
                                         :service/diva-advertised-type)
        service-menu                (or (get-in app-state adventure-keypaths/adventure-servicing-stylist-service-menu)
                                        default-service-menu)]
    {:mayvenn-install/entered?           freeinstall-entered?
     :mayvenn-install/locked?            (and freeinstall-entered?
                                              (pos? items-remaining-for-install))
     :mayvenn-install/applied?           (and freeinstall-entered?
                                              ;; TODO should we consider the following that checks line-items for promos
                                              ;; (boolean (orders/applied-install-promotion order))
                                              ;; (orders/freeinstall-applied? order)
                                              (zero? items-remaining-for-install))
     :mayvenn-install/quantity-required  install-items-required
     :mayvenn-install/quantity-remaining (- install-items-required items-added-for-install)
     :mayvenn-install/quantity-added     items-added-for-install
     :mayvenn-install/stylist            servicing-stylist
     :mayvenn-install/service-type       service-type
     :mayvenn-install/service-discount   (some-> service-menu
                                                 ;; If the menu does not provide the service matching the
                                                 ;; cart contents, use the leave out price
                                                 (get service-type (:advertised-sew-in-leave-out service-menu))
                                                 spice/parse-double
                                                 -)}))

;; TODO: suggestions should be paired with appropriate cart item here
(defn cart-items-query
  [app-state
   {:mayvenn-install/keys [entered? locked? applied? stylist service-discount quantity-remaining quantity-required quantity-added]}
   line-items
   skus]
  (let [update-line-item-requests (merge-with
                                   #(or %1 %2)
                                   (variants-requests app-state request-keys/add-to-bag (map :sku line-items))
                                   (variants-requests app-state request-keys/update-line-item (map :sku line-items)))
        delete-line-item-requests (variants-requests app-state request-keys/delete-line-item (map :id line-items))

        cart-items (for [{sku-id :sku variant-id :id :as line-item} line-items
                         :let
                         [sku                  (get skus sku-id)
                          price                (or (:sku/price line-item)
                                                   (:unit-price line-item))
                          qty-adjustment-args {:variant (select-keys line-item [:id :sku])}
                          removing?            (get delete-line-item-requests variant-id)
                          updating?            (get update-line-item-requests sku-id)
                          just-added-to-order? false #_ (contains? recently-added-skus sku-id)]]
                     {:react/key                                      (str sku-id "-" (:quantity line-item))
                      :cart-item-title/id                             (str "line-item-title-" sku-id)
                      :cart-item-title/primary                        (or (:product-title line-item)
                                                                          (:product-name line-item))
                      :cart-item-title/secondary                      (:color-name line-item)
                      :cart-item-floating-box/id                      (str "line-item-price-ea-" sku-id)
                      :cart-item-floating-box/value                   [:span.dark-gray
                                                                       [:span.medium.black (mf/as-money price)]
                                                                       " ea"]
                      :cart-item-square-thumbnail/id                  sku-id
                      :cart-item-square-thumbnail/sku-id              sku-id
                      :cart-item-square-thumbnail/highlighted?        (get-in app-state keypaths/cart-freeinstall-just-added?)
                      :cart-item-square-thumbnail/sticker-label       (when-let [length-circle-value (-> sku :hair/length first)]
                                                                        (str length-circle-value "”"))
                      :cart-item-square-thumbnail/ucare-id            (->> sku (catalog-images/image "cart") :ucare/id)
                      :cart-item-adjustable-quantity/id               (str "line-item-quantity-" sku-id)
                      :cart-item-adjustable-quantity/spinning?        updating?
                      :cart-item-adjustable-quantity/value            (:quantity line-item)
                      :cart-item-adjustable-quantity/id-suffix        sku-id
                      :cart-item-adjustable-quantity/decrement-target [events/control-cart-line-item-dec qty-adjustment-args]
                      :cart-item-adjustable-quantity/increment-target [events/control-cart-line-item-inc qty-adjustment-args]
                      :cart-item-remove-action/id                     "line-item-remove-freeinstall"
                      :cart-item-remove-action/spinning?              removing?
                      :cart-item-remove-action/target                 [events/control-cart-remove (:id line-item)]})]
    (cond-> cart-items
      entered?
      (concat
       [(cond-> {:react/key                         "freeinstall-line-item-freeinstall"
                 :cart-item-title/id                "line-item-title-freeinstall"
                 :cart-item-floating-box/id         "line-item-price-freeinstall"
                 :cart-item-floating-box/value      [:span.medium (mf/as-money (- service-discount))]
                 :cart-item-thumbnail/id            "freeinstall"
                 :cart-item-thumbnail/highlighted?  (get-in app-state keypaths/cart-freeinstall-just-added?)
                 :cart-item-thumbnail/value         nil
                 :cart-item-thumbnail/ucare-id      "bc776b8a-595d-46ef-820e-04915478ffe8"
                 :cart-item-remove-action/id        "line-item-remove-freeinstall"
                 :cart-item-remove-action/spinning? (utils/requesting? app-state request-keys/remove-promotion-code)
                 :cart-item-remove-action/target    [events/control-checkout-remove-promotion {:code "freeinstall"}]}

          ;; Locked basically means the freeinstall coupon code was entered, yet not all the requirements
          ;; of a free install order to generate a voucher have been satisfied.
          locked?
          (merge  {:cart-item-title/primary                   "Mayvenn Install (locked)"
                   :cart-item-copy/value                      (str "Add " quantity-remaining
                                                                   " or more items to receive your free Mayvenn Install")
                   :cart-item-thumbnail/locked?               true
                   :cart-item-steps-to-complete/action-target []
                   :cart-item-steps-to-complete/action-label  "add items"
                   :cart-item-steps-to-complete/steps         (->> quantity-required
                                                                   range
                                                                   (map inc))
                   :cart-item-steps-to-complete/current-step  quantity-added})

          applied?
          (merge {:cart-item-title/primary "Mayvenn Install"
                  :cart-item-copy/value    "Congratulations! You're all set for your Mayvenn Install. Select your stylist after checkout."})

          stylist
          (merge {:cart-item-title/secondary    (str "w/ " (:store-nickname stylist))
                  :cart-item-thumbnail/ucare-id (-> stylist
                                                    :portrait
                                                    :resizable-url
                                                    ui/ucare-img-id)})

          (and applied? stylist)
          (merge {:rating/value         (:rating stylist)
                  :cart-item-copy/value nil}))]))))

(defn cart-summary-query
  [{:as order :keys [adjustments]}
   {:mayvenn-install/keys [entered? locked? applied? service-discount quantity-remaining]}]
  (let [total         (-> order :total)
        subtotal      (orders/products-subtotal order)
        shipping      (some->> order
                               orders/shipping-item
                               vector
                               (apply (juxt :quantity :unit-price))
                               (reduce *))
        adjustment    (reduce + (map :price (orders/all-order-adjustments order)))
        total-savings (- adjustment service-discount)]
    {:cart-summary/id                 "cart-summary"
     :freeinstall-informational/value (not entered?)
     :cart-summary-total-line/id      "total"
     :cart-summary-total-line/label   (if applied? "Hair + Install Total" "Total")
     :cart-summary-total-line/value   (cond
                                        applied?
                                        [:div
                                         [:div.bold.h2
                                          (some-> total mf/as-money)]
                                         [:div.h6.bg-purple.white.px2.nowrap.mb1
                                          "Includes Mayvenn Install"]
                                         (when (neg? total-savings)
                                           [:div.h6.light.dark-gray.pxp1.nowrap.italic
                                            "You've saved "
                                            [:span.bold {:data-test "total-savings"}
                                             (mf/as-money total-savings)]])]

                                        locked?
                                        [:div.h7.dark-gray.light
                                         "Add " quantity-remaining
                                         " more " (ui/pluralize quantity-remaining "item")
                                         " to "
                                         [:br]
                                         " calculate total price"]

                                        :else
                                        [:div (some-> total mf/as-money)])
     :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                   :cart-summary-line/label "Subtotal"
                                   :cart-summary-line/value (mf/as-money (cond-> subtotal
                                                                           applied?
                                                                           (- service-discount)))}]

                                 (when shipping
                                   [{:cart-summary-line/id    "shipping"
                                     :cart-summary-line/label "Shipping"
                                     :cart-summary-line/value (mf/as-money-or-free shipping)}])

                                 (when locked?
                                   ;; When FREEINSTALL is merely locked (and so not yet an adjustment) we must special case it, so:
                                   [{:cart-summary-line/id    "freeinstall-locked"
                                     :cart-summary-line/icon  (svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                 :height "2em" :width "2em"})
                                     :cart-summary-line/label "FREEINSTALL"
                                     :cart-summary-line/value (mf/as-money-or-free service-discount)
                                     :cart-summary-line/class "bold purple"}])

                                 (for [{:keys [name price coupon-code]}
                                       ;; TODO extract
                                       (filter (fn non-zero-adjustment? [{:keys [price coupon-code]}]
                                                 (or (not (= price 0))
                                                     (#{"amazon" "freeinstall" "install"} coupon-code)))
                                               adjustments)
                                       :let
                                       [install-summary-line? (= "freeinstall" coupon-code)
                                        coupon-summary-line? (and coupon-code
                                                                  (not install-summary-line?))]]
                                   (cond-> {:cart-summary-line/id    (text->data-test-name name)
                                            :cart-summary-line/icon  (svg/discount-tag {:class  "mxnp6 fill-gray pr1"
                                                                                        :height "2em" :width "2em"})
                                            :cart-summary-line/label (orders/display-adjustment-name name)
                                            :cart-summary-line/value (mf/as-money-or-free price)}

                                     install-summary-line?
                                     (merge {:cart-summary-line/value (mf/as-money-or-free service-discount)
                                             :cart-summary-line/class "bold purple"})

                                     coupon-summary-line?
                                     (merge {:cart-summary-line/action-id     "cart-remove-promo"
                                             :cart-summary-line/action-icon   (svg/close-x {:class "stroke-white fill-gray"})
                                             :cart-summary-line/action-target [events/control-checkout-remove-promotion {:code coupon-code}]}))))}))

(defn full-cart-query [data]
  (let [order                                (get-in data keypaths/order)
        products                             (get-in data keypaths/v2-products)
        facets                               (get-in data keypaths/v2-facets)
        line-items                           (map (partial add-product-title-and-color-to-line-item products facets)
                                                  (orders/product-items order))
        freeinstall-entered-cart-incomplete? (and (orders/freeinstall-entered? order)
                                                  (not (orders/freeinstall-applied? order)))
        mayvenn-install                      (mayvenn-install data)
        entered?                             (:mayvenn-install/entered? mayvenn-install)]
    {:suggestions             (suggestions/consolidated-query data)
     :order                   order
     :line-items              line-items
     :skus                    (get-in data keypaths/v2-skus)
     :products                products
     :promo-banner            (when (zero? (orders/product-quantity order))
                                (promo-banner/query data))
     :call-out                (call-out/query data)
     :checkout-disabled?      (or freeinstall-entered-cart-incomplete?
                                  (update-pending? data))
     :redirecting-to-paypal?  (get-in data keypaths/cart-paypal-redirect)
     :share-carts?            (stylists/own-store? data)
     :requesting-shared-cart? (utils/requesting? data request-keys/create-shared-cart)
     :loaded-quadpay?         (get-in data keypaths/loaded-quadpay)
     :show-browser-pay?       (and (get-in data keypaths/loaded-stripe)
                                   (experiments/browser-pay? data)
                                   (seq (get-in data keypaths/shipping-methods))
                                   (seq (get-in data keypaths/states)))
     :recently-added-skus     (get-in data keypaths/cart-recently-added-skus)

     :return-link/copy          "Continue Shopping"
     :return-link/event-message [events/control-open-shop-escape-hatch]
     :quantity-remaining        (:mayvenn-install/quantity-remaining mayvenn-install)
     :locked?                   (:mayvenn-install/locked? mayvenn-install)
     :entered?                  entered?
     :applied?                  (:mayvenn-install/applied? mayvenn-install)
     :remove-freeinstall-event  [events/control-checkout-remove-promotion {:code "freeinstall"}]
     :cart-summary              (merge
                                 (cart-summary-query order mayvenn-install)
                                 (when (and (orders/no-applied-promo? order)
                                            (not entered?))
                                   {:promo-data {:coupon-code   (get-in data keypaths/cart-coupon-code)
                                                 :applying?     (utils/requesting? data request-keys/add-promotion-code)
                                                 :focused       (get-in data keypaths/ui-focus)
                                                 :error-message (get-in data keypaths/error-message)
                                                 :field-errors  (get-in data keypaths/field-errors)}}))
     :cart-items                (cart-items-query data mayvenn-install line-items (get-in data keypaths/v2-skus))}))

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
      (if (and (zero? item-count)
               (not (:entered? full-cart)))
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
