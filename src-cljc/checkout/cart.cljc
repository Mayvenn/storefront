(ns checkout.cart
  (:require
   #?@(:cljs [[storefront.components.popup :as popup]
              [storefront.components.payment-request-button :as payment-request-button]
              [storefront.api :as api]
              [storefront.history :as history]
              [storefront.hooks.browser-pay :as browser-pay]
              [storefront.browser.cookie-jar :as cookie-jar]
              [storefront.accessors.stylist-urls :as stylist-urls]
              [storefront.hooks.quadpay :as quadpay]
              [goog.labs.userAgent.device :as device]])
   [catalog.images :as catalog-images]
   [cemerick.url :refer [url-encode]]
   [checkout.call-out :as call-out]
   [checkout.cart.items :as cart-items]
   [checkout.cart.summary :as cart-summary]
   [checkout.header :as header]
   [checkout.suggestions :as suggestions]
   [storefront.accessors.experiments :as experiments]
   [catalog.facets :as facets]
   [storefront.accessors.images :as images]
   [storefront.accessors.line-items :as line-items]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.products :as products]
   [storefront.accessors.promos :as promos]
   [storefront.accessors.stylists :as stylists]
   [storefront.component :as component]
   [storefront.components.flash :as flash]
   [storefront.components.footer :as storefront.footer]
   [storefront.components.money-formatters :as mf]
   [ui.promo-banner :as promo-banner]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.config :as config]
   [storefront.css-transitions :as css-transitions]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]))

(defn display-adjustable-line-items
  [recently-added-skus line-items skus update-line-item-requests delete-line-item-requests]
  (for [{sku-id :sku variant-id :id :as line-item} line-items

        :let [sku                  (get skus sku-id)
              legacy-variant-id    (or (:legacy/variant-id line-item) (:id line-item))
              price                (or (:sku/price line-item)         (:unit-price line-item))
              removing?            (get delete-line-item-requests variant-id)
              updating?            (get update-line-item-requests sku-id)
              just-added-to-order? (contains? recently-added-skus sku-id)
              length-circle-value  (-> sku :hair/length first)
              discount-price       (line-items/discounted-unit-price line-item)
              money-formatter      mf/as-money-without-cents]]
    [:div.pt1.pb2 {:key (str sku-id "-" (:quantity line-item))}
     [:div.left.pr1
      (when-not length-circle-value
        {:class "pr3"})
      (when length-circle-value
        (css-transitions/transition-background-color
         just-added-to-order?
         [:div.right.z1.circle.stacking-context.border.border-light-gray.flex.items-center.justify-center.medium.h5.bg-too-light-teal
          {:key       (str "length-circle-" sku-id)
           :data-test (str "line-item-length-" sku-id)
           :style     {:margin-left "-21px"
                       :margin-top  "-10px"
                       :width       "32px"
                       :height      "32px"}} (str length-circle-value "”")]))

      (css-transitions/transition-background-color
       just-added-to-order?
       [:div.flex.items-center.justify-center.ml1
        {:key       (str "thumbnail-" sku-id)
         :data-test (str "line-item-img-" (:catalog/sku-id sku))
         :style     {:width "79px" :height "74px"}}
        (ui/ucare-img
         {:width 75}
         (->> sku (catalog-images/image "cart") :ucare/id))])]

     [:div {:style {:margin-top "-14px"}}
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
        [:div.h5.right {:data-test (str "line-item-price-ea-" sku-id)} (money-formatter price) " each"]]]]]))

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

(defn full-component [{:keys [order
                              skus
                              promo-banner
                              call-out
                              updating?
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
                              loaded-quadpay?
                              cart-summary]} owner _]
  (component/create
   [:div.container.p2
    (component/build promo-banner/sticky-organism promo-banner nil)

    (component/build call-out/component call-out nil)

    [:div.clearfix.mxn3
     [:div.col-on-tb-dt.col-6-on-tb-dt.px3
      {:data-test "cart-line-items"}
      (display-adjustable-line-items recently-added-skus
                                     line-items
                                     skus
                                     update-line-item-requests
                                     delete-line-item-requests)
      (when freeinstall-line-item-data
        (non-adjustable-line-item freeinstall-just-added? freeinstall-line-item-data))

      (component/build suggestions/component suggestions nil)]

     [:div.col-on-tb-dt.col-6-on-tb-dt.px3

      (component/build cart-summary/component cart-summary nil)

      #?@(:cljs
          [(component/build quadpay/component
                            {:quadpay/order-total (:total order)
                             :quadpay/show?       loaded-quadpay?
                             :quadpay/directive   :just-select}
                            nil)])
      (ui/teal-button {:spinning? false
                       :disabled? updating?
                       :on-click  (utils/send-event-callback events/control-checkout-cart-submit)
                       :data-test "start-checkout-button"}
                      [:div "Check out"])

      [:div.h5.black.center.py1.flex.justify-around.items-center
       [:div.flex-grow-1.border-bottom.border-light-gray]
       [:div.mx2 "or"]
       [:div.flex-grow-1.border-bottom.border-light-gray]]

      [:div.pb2
       (ui/aqua-button {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                        :spinning? redirecting-to-paypal?
                        :disabled? updating?
                        :data-test "paypal-checkout"}
                       [:div
                        "Check out with "
                        [:span.medium.italic "PayPal™"]])]

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



#?(:cljs
   (defn- order-has-inapplicable-freeinstall-promo?
     "A small hack to prevent classic orders from being placed with the freeinstall
        promo.  A full solution would be implemented in waiter."
     [app-state]
     (let [promotion-codes  (get-in app-state keypaths/order-promotion-codes)
           store-experience (get-in app-state keypaths/store-experience)]
       (and (some #(= "freeinstall" %) promotion-codes)
            (= "mayvenn-classic" store-experience)))))

#?(:cljs
   (defn- reject-inapplicable-freeinstall-promo [session-id order]
     (api/remove-promotion-code session-id
                                order
                                "freeinstall"
                                #(history/enqueue-navigate events/navigate-cart {:query-params {:error "ineligible-for-free-install"}}))))

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

(defmethod effects/perform-effects events/control-cart-update-coupon
  [_ _ _ _ app-state]
  #?(:cljs
     (let [coupon-code (get-in app-state keypaths/cart-coupon-code)]
       (when-not (empty? coupon-code)
         (api/add-promotion-code {:shop?              (= "shop" (get-in app-state keypaths/store-slug))
                                  :session-id         (get-in app-state keypaths/session-id)
                                  :number             (get-in app-state keypaths/order-number)
                                  :token              (get-in app-state keypaths/order-token)
                                  :promo-code         coupon-code
                                  :allow-dormant?     false})))))

(defmethod effects/perform-effects events/control-cart-share-show
  [_ _ _ _ app-state]
  #?(:cljs
     (api/create-shared-cart (get-in app-state keypaths/session-id)
                             (get-in app-state keypaths/order-number)
                             (get-in app-state keypaths/order-token))))

(defmethod effects/perform-effects events/control-cart-remove
  [_ event variant-id _ app-state]
  #?(:cljs
     (api/delete-line-item (get-in app-state keypaths/session-id) (get-in app-state keypaths/order) variant-id)))

(defmethod effects/perform-effects events/control-cart-line-item-inc
  [_ event {:keys [variant]} _ app-state]
  #?(:cljs
     (let [sku      (get (get-in app-state keypaths/v2-skus) (:sku variant))
           order    (get-in app-state keypaths/order)
           quantity 1]
       (api/add-sku-to-bag (get-in app-state keypaths/session-id)
                           {:sku      sku
                            :token    (:token order)
                            :number   (:number order)
                            :quantity quantity}
                           #(messages/handle-message events/api-success-add-sku-to-bag
                                            {:order    %
                                             :quantity quantity
                                             :sku      sku})))))

(defmethod effects/perform-effects events/control-cart-line-item-dec
  [_ event {:keys [variant]} _ app-state]
  #?(:cljs
     (let [order (get-in app-state keypaths/order)]
       (api/remove-line-item (get-in app-state keypaths/session-id)
                             {:number     (:number order)
                              :token      (:token order)
                              :variant-id (:id variant)
                              :sku-code   (:sku variant)}
                             #(messages/handle-message events/api-success-add-to-bag {:order %})))))

(defmethod effects/perform-effects events/control-checkout-cart-submit
  [dispatch event args _ app-state]
  #?(:cljs
     (if (order-has-inapplicable-freeinstall-promo? app-state)
       (reject-inapplicable-freeinstall-promo (get-in app-state keypaths/session-id)
                                              (get-in app-state keypaths/order))
       ;; If logged in, this will send user to checkout-address. If not, this sets
       ;; things up so that if the user chooses sign-in from the returning-or-guest
       ;; page, then signs-in, they end up on the address page. Convoluted.
       (history/enqueue-navigate events/navigate-checkout-address))))

(defmethod effects/perform-effects events/control-checkout-cart-paypal-setup
  [dispatch event args _ app-state]
  #?(:cljs
     (let [order (get-in app-state keypaths/order)]
       (if (order-has-inapplicable-freeinstall-promo? app-state)
         (reject-inapplicable-freeinstall-promo (get-in app-state keypaths/session-id)
                                                (get-in app-state keypaths/order))
         ;; If logged in, this will send user to checkout-address. If not, this sets
         ;; things up so that if the user chooses sign-in from the returning-or-guest
         ;; page, then signs-in, they end up on the address page. Convoluted.
         (api/update-cart-payments
          (get-in app-state keypaths/session-id)
          {:order (-> app-state
                      (get-in keypaths/order)
                      (select-keys [:token :number])
                 ;;; Get ready for some nonsense!
                      ;;
                      ;; Paypal requires that urls are *double* url-encoded, such as
                      ;; the token part of the return url, but that *query
                      ;; parameters* are only singley encoded.
                      ;;
                      ;; Thanks for the /totally sane/ API, PayPal.
                      (assoc-in [:cart-payments]
                                {:paypal {:amount (get-in app-state keypaths/order-total)
                                          :mobile-checkout? (not (device/isDesktop))
                                          :return-url (str stylist-urls/store-url "/orders/" (:number order) "/paypal/"
                                                           (url-encode (url-encode (:token order)))
                                                           "?sid="
                                                           (url-encode (get-in app-state keypaths/session-id)))
                                          :callback-url (str config/api-base-url "/v2/paypal-callback?number=" (:number order)
                                                             "&order-token=" (url-encode (:token order)))
                                          :cancel-url (str stylist-urls/store-url "/cart?error=paypal-cancel")}}))
           :event events/external-redirect-paypal-setup})))))

(defn full-cart-query [data]
  (let [order       (get-in data keypaths/order)
        products    (get-in data keypaths/v2-products)
        facets      (get-in data keypaths/v2-facets)
        line-items  (map (partial add-product-title-and-color-to-line-item products facets)
                         (orders/product-items order))
        variant-ids (map :id line-items)]
    {:suggestions                (suggestions/query data)
     :order                      order
     :line-items                 line-items
     :skus                       (get-in data keypaths/v2-skus)
     :products                   products
     :promo-banner               (when (zero? (orders/product-quantity order))
                                   (promo-banner/query data))
     :call-out                   (call-out/query data)
     :updating?                  (update-pending? data)
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
     :cart-summary               (cart-summary/query data)
     :delete-line-item-requests  (variants-requests data request-keys/delete-line-item variant-ids)
     :recently-added-skus        (get-in data keypaths/cart-recently-added-skus)
     :freeinstall-just-added?    (get-in data keypaths/cart-freeinstall-just-added?)
     :stylist-service-menu       (get-in data keypaths/stylist-service-menu)
     :freeinstall-line-item-data (cart-items/freeinstall-line-item-query data)}))

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

(defn layout [app-state nav-event]
  [:div.flex.flex-column {:style {:min-height    "100vh"
                                  :margin-bottom "-1px"}}
   #?(:cljs (popup/built-component app-state nil))

   (header/built-component app-state nil)
   (promo-banner/built-static-organism app-state nil)
   [:div.relative.flex.flex-column.flex-auto
    (flash/built-component app-state nil)

    [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
     (built-component app-state nil)]

    [:footer
     (storefront.footer/built-component app-state nil)]]])
