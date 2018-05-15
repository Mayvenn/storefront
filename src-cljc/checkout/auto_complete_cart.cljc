(ns checkout.auto-complete-cart
  (:require
   #?@(:cljs [[storefront.component :as component]
              [storefront.components.popup :as popup]
              [storefront.components.order-summary :as summary]
              [storefront.api :as api]]
       :clj [[storefront.component-shim :as component]])
   [checkout.cart :as cart]
   [checkout.header :as header]
   [spice.selector :as selector]
   [clojure.string :as string]
   [spice.core :as spice]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.facets :as facets]
   [storefront.accessors.images :as images]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.products :as products]
   [storefront.accessors.promos :as promos]
   [storefront.accessors.stylists :as stylists]
   [storefront.components.affirm :as affirm]
   [storefront.components.flash :as flash]
   [storefront.components.footer :as storefront.footer]
   [storefront.components.scrim :as scrim]
   [storefront.components.money-formatters :as mf]
   [storefront.components.header-new-flyout :as header-new-flyout]
   [storefront.components.promotion-banner :as promotion-banner]
   [storefront.components.stylist-banner :as stylist-banner]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.platform.messages :as messages]
   [storefront.css-transitions :as css-transitions]
   [storefront.events :as events]
   [storefront.transitions :as transitions]
   [storefront.effects :as effects]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]))

(defn transition-background-color [run-transition? & content]
  (if run-transition?
    (css-transitions/transition-element
     {:transitionName          "line-item-fade"
      :transitionAppearTimeout 1300
      :transitionAppear        true
      :transitionEnter         true
      :transitionEnterTimeout  1300}
     content)
    content))

(defn display-adjustable-line-items
  [recently-added-skus line-items skus update-line-item-requests delete-line-item-requests]
  (for [{sku-id :sku variant-id :id :as line-item} line-items
        :let [sku                  (get skus sku-id)
              legacy-variant-id    (or (:legacy/variant-id line-item) (:id line-item))
              price                (or (:sku/price line-item)         (:unit-price line-item))
              thumbnail            (merge
                                    (images/cart-image sku)
                                    {:data-test (str "line-item-img-" (:catalog/sku-id sku))})
              removing?            (get delete-line-item-requests variant-id)
              updating?            (get update-line-item-requests sku-id)
              just-added-to-order? (contains? recently-added-skus sku-id)]]
    [:div.pt1.pb2 {:key (str (:catalog/sku-id sku) (:quantity line-item))}

     [:div.left.pr1
      (when-let [length (-> sku :hair/length first)]
        (transition-background-color just-added-to-order?
                                     [:div.right.z1.circle.stacking-context.border.border-light-gray.flex.items-center.justify-center.medium.h5.bg-too-light-teal
                                      {:key   (str "length-circle-" sku-id)
                                       :data-test (str "line-item-length-" sku-id)
                                       :style {:margin-left "-21px"
                                               :margin-top  "-10px"
                                               :width       "32px"
                                               :height      "32px"}} (str length "”")]))

      (transition-background-color just-added-to-order?
                                   [:div.flex.items-center.justify-center.ml1 {:key   (str "thumbnail-" sku-id)
                                                                               :style {:width "79px" :height "79px"}}
                                    [:img.block.border.border-light-gray
                                     (assoc thumbnail :style {:width "75px" :height "75px"})]])]

     [:div {:style {:margin-top "-14px"}}
      [:a.medium.titleize.h5
       {:data-test (str "line-item-title-" sku-id)}
       (:product-title line-item)]
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
            (svg/trash-can {:height "1.1em"
                            :width  "1.1em"
                            :class  "stroke-dark-gray"})])]]
       [:div.flex.justify-between.mt1
        [:div.h3
         {:data-test (str "line-item-quantity-" sku-id)}
         (ui/auto-complete-counter {:spinning? updating?
                                    :data-test sku-id}
                                   (:quantity line-item)
                                   (utils/send-event-callback events/control-cart-line-item-dec {:variant line-item})
                                   (utils/send-event-callback events/control-cart-line-item-inc {:variant line-item}))]
        [:div.h5 {:data-test (str "line-item-price-ea-" sku-id)} (mf/as-money-without-cents price) " ea"]]]]]))

(defn ^:private summary-row
  ([content amount] (summary-row {} content amount))
  ([row-attrs content amount]
   [:tr.h5
    (merge (when (neg? amount)
             {:class "teal"})
           row-attrs)
    [:td.pyp1 content]
    [:td.pyp1.right-align.medium
     (mf/as-money-or-free amount)]]))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn display-order-summary [order {:keys [read-only? available-store-credit use-store-credit? promo-data]}]
  (let [adjustments-including-tax (orders/all-order-adjustments order)
        shipping-item             (orders/shipping-item order)
        store-credit              (min (:total order) (or available-store-credit
                                                          (-> order :cart-payments :store-credit :amount)
                                                          0.0))]
    [:div
     [:div.hide-on-dt.border-top.border-light-gray]
     [:div.py1.border-bottom.border-light-gray
      [:table.col-12
       [:tbody
        (summary-row "Subtotal" (orders/products-subtotal order))
        (when shipping-item
          (summary-row "Shipping" (* (:quantity shipping-item) (:unit-price shipping-item))))

        (when-not (orders/applied-promo-code order)
          (let [{:keys [focused coupon-code field-errors updating? applying? error-message]} promo-data]
            [:tr.h5
             [:td
              {:col-span "2"}
              [:form.mt2
               {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
               (ui/input-group
                {:keypath       keypaths/cart-coupon-code
                 :wrapper-class "flex-grow-5"
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

        (for [{:keys [name price coupon-code]} adjustments-including-tax]
          (when (or (not (= price 0)) (#{"amazon" "freeinstall" "install"} coupon-code))
            (summary-row
             {:key name}
             [:div.flex.items-center.align-middle {:data-test (text->data-test-name name)}
              (when (= "Bundle Discount" name)
                (svg/discount-tag {:class  "mxnp6"
                                   :height "2em" :width "2em"}))
              (orders/display-adjustment-name name)
              (when (and (not read-only?) coupon-code)
                [:a.ml1.h6.gray.flex.items-center
                 (merge {:data-test "cart-remove-promo"}
                        (utils/fake-href events/control-checkout-remove-promotion
                                         {:code coupon-code}))
                 (svg/close-x {:class "stroke-white fill-gray"})])]
             price)))

        (when (pos? store-credit)
          (summary-row "Store Credit" (- store-credit)))]]]
     [:div.py2.h2
      [:div.flex
       [:div.flex-auto.light "Total"]
       [:div.right-align.medium
        (cond-> (:total order)
          use-store-credit? (- store-credit)
          true              mf/as-money)]]]]))

(defn suggested-bundles
  [{:keys [image position skus initial-sku this-is-adding-to-bag? any-adding-to-bag?]}]
  (let [[short-sku long-sku] skus
        sized-image          (update image :style merge {:height "36px" :width "40px"})]
    [:div.mx2.my4.col-11
     {:data-test (str "suggestion-" (name position))
      :key       (str "suggestion-" (map :catalog/sku-id skus) "-" (name position))}
     [:div.absolute (svg/discount-tag {:style {:height      "3em"
                                               :width       "3em"
                                               :margin-left "-23px"
                                               :margin-top  "-20px"}})]
     [:div.border.border-light-gray.bg-light-gray
      {:style {:height "68px"}}
      [:div.bg-white.h5.medium.center
       (first (:hair/length short-sku))
       "” & "
       (first (:hair/length long-sku))
       "”"]
      [:div.flex.justify-center
       [:img.m1 sized-image]
       [:img.m1 sized-image]]
      [:div.col-10.mx-auto
       (ui/navy-button {:class     "p1"
                        ;; we don't want to draw attention to the disabling of the other 'Add' button,
                        ;; but we do want to prevent people from clicking both.
                        ;; :disabled? (and (not this-is-adding-to-bag?) any-adding-to-bag?)
                        :on-click  (if (and (not this-is-adding-to-bag?) any-adding-to-bag?)
                                     utils/noop-callback
                                     (utils/send-event-callback events/control-suggested-add-to-bag {:skus        skus
                                                                                                     :initial-sku initial-sku}))
                        :spinning? this-is-adding-to-bag?
                        :data-test (str "add-" (name position))
                        :style     {:margin-top "-10px"
                                    :height     "40px"}} "Add")]]]))

(defn auto-complete-component [{:keys [suggestions]}]
  (component/create
   (when (seq suggestions)
     [:div.mb4.px1.col-12.mx-auto.bg-light-orange
      {:style     {:height "135px"}
       :data-test "auto-complete"}
      [:div.flex.justify-center (map suggested-bundles suggestions)]])))

(defn full-component [{:keys [products
                              order
                              disable-apple-pay-button?
                              skus
                              promotion-banner
                              updating?
                              redirecting-to-paypal?
                              share-carts?
                              requesting-shared-cart?
                              auto-complete
                              focused
                              field-errors
                              line-items
                              error-message
                              coupon-code
                              update-line-item-requests
                              show-apple-pay?
                              the-ville?
                              applying-coupon?
                              recently-added-skus
                              delete-line-item-requests
                              seventy-five-off-install?
                              show-green-banner?]} owner]
  (component/create
   [:div.container.p2
    (component/build cart/deploy-promotion-banner-component promotion-banner nil)

    (cond
      seventy-five-off-install?
      [:div.mb3
       (cart/seventy-five-off-install-cart-promo show-green-banner?)]

      the-ville?
      [:div.mb3
       (cart/free-install-cart-promo show-green-banner?)]

      :else nil)

    [:div.clearfix.mxn3
     [:div.col-on-tb-dt.col-6-on-tb-dt.px3
      {:data-test "cart-line-items"}
      (display-adjustable-line-items recently-added-skus
                                     line-items
                                     skus
                                     update-line-item-requests
                                     delete-line-item-requests)

      (component/build auto-complete-component auto-complete nil)]

     [:div.col-on-tb-dt.col-6-on-tb-dt.px3
      (display-order-summary order
                             {:read-only?        false
                              :use-store-credit? false
                              :promo-data        {:coupon-code   coupon-code
                                                  :applying?     applying-coupon?
                                                  :focused       focused
                                                  :error-message error-message
                                                  :field-errors  field-errors}})

      (affirm/auto-complete-as-low-as-box {:amount (:total order)})

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

      (when show-apple-pay?
        [:div.pb2
         (ui/apple-pay-button
          {:on-click  (utils/send-event-callback events/control-checkout-cart-apple-pay)
           :data-test "apple-pay-checkout"
           :disabled? disable-apple-pay-button?}
          [:div.flex.items-center.justify-center
           "Check out with "
           [:span.img-apple-pay.bg-fill.bg-no-repeat.inline-block.mtp4.ml1 {:style {:width  "4rem"
                                                                                    :height "2rem"}}]])])

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

(defn empty-component [{:keys [promotions]} owner]
  (component/create
   (ui/narrow-container
    [:div.p2
     [:.center {:data-test "empty-bag"}
      [:div.m2 (svg/bag {:style {:height "70px" :width "70px"}
                         :class "fill-black"})]

      [:p.m2.h2.light "Your bag is empty."]

      [:div.m2
       (if-let [promo (promos/default-advertised-promotion promotions)]
         (:description promo)
         promos/bundle-discount-description)]]

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

(defn ^:private add-product-title-and-color-to-line-item [products facets line-item]
  (merge line-item {:product-title (->> line-item
                                        :sku
                                        (products/find-product-by-sku-id products)
                                        :copy/title)
                    :color-name    (-> line-item
                                       :variant-attrs
                                       :color
                                       (facets/get-color facets)
                                       :option/name)}))

(defn suggest-bundles
  [data products skus items]
  (when (= 1 (orders/line-item-quantity items))
    (let [{:keys [variant-attrs] sku-id :sku} (first items)]
      (when (= "bundles" (variant-attrs :hair/family))
        (let [sku               (get skus sku-id)
              image             (images/cart-image sku)
              adjacent-skus     (->> sku
                                     :selector/from-products
                                     first
                                     (get products)
                                     :selector/sku-ids
                                     (map (partial get skus))
                                     (selector/match-all {} {:hair/color (:hair/color sku)})
                                     (sort-by (comp first :hair/length))
                                     (partition-by #(= (:catalog/sku-id sku) (:catalog/sku-id %))))
              shorter-skus      (first adjacent-skus)
              longer-skus       (last adjacent-skus)
              short-suggestions (if (< (count shorter-skus) 2)
                                  (repeat 2 sku)
                                  (take-last 2 shorter-skus))
              long-suggestions  (if (< (count longer-skus) 2)
                                  (repeat 2 sku)
                                  (take 2 longer-skus))]
          (->> {:shorter-lengths short-suggestions
                :longer-lengths  long-suggestions}
               (filterv (fn in-stock? [[_ skus]]
                          (every? :inventory/in-stock? skus)))
               (mapv (fn transform [[position skus]]
                       {:position               position
                        :image                  image
                        :skus                   skus
                        :initial-sku             sku
                        :any-adding-to-bag?     (utils/requesting? data (fn [req]
                                                                          (subvec (:request-key req []) 0 1))
                                                                   request-keys/add-to-bag)
                        :this-is-adding-to-bag? (utils/requesting? data (conj request-keys/add-to-bag (set (map :catalog/sku-id skus))))}))))))))

#?(:cljs
   (defmethod effects/perform-effects events/control-suggested-add-to-bag [_ _ {:keys [skus initial-sku]} _ app-state]
     (api/add-skus-to-bag (get-in app-state keypaths/session-id) {:number           (get-in app-state keypaths/order-number)
                                                                  :token            (get-in app-state keypaths/order-token)
                                                                  :sku-id->quantity (into {} (map (fn [[sku-id skus]] [sku-id (count skus)])
                                                                                                  (group-by :catalog/sku-id skus)))}
                          #(messages/handle-message events/api-success-suggested-add-to-bag (assoc % :initial-sku initial-sku)))))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-suggested-add-to-bag [_ _ {:keys [order]} previous-app-state app-state]
     (messages/handle-message events/save-order {:order order})))

(defn auto-complete-query
  [data]
  ;; TODO(jeff): refactor this as we are passing data in, as well as things that come off of data
  (let [skus       (get-in data keypaths/v2-skus)
        products   (get-in data keypaths/v2-products)
        line-items (orders/product-items (get-in data keypaths/order))]
    {:suggestions (suggest-bundles data products skus line-items)}))

(defn full-cart-query [data]
  (let [order         (get-in data keypaths/order)
        products      (get-in data keypaths/v2-products)
        facets        (get-in data keypaths/v2-facets)
        line-items    (map (partial add-product-title-and-color-to-line-item products facets) (orders/product-items order))
        variant-ids   (map :id line-items)
        auto-complete (auto-complete-query data)]
    {:auto-complete             auto-complete
     :order                     order
     :line-items                line-items
     :skus                      (get-in data keypaths/v2-skus)
     :products                  products
     :show-green-banner?        (cart/install-qualified? order)
     :coupon-code               (get-in data keypaths/cart-coupon-code)
     :promotion-banner          (promotion-banner/query data)
     :updating?                 (update-pending? data)
     :applying-coupon?          (utils/requesting? data request-keys/add-promotion-code)
     :redirecting-to-paypal?    (get-in data keypaths/cart-paypal-redirect)
     :share-carts?              (stylists/own-store? data)
     :requesting-shared-cart?   (utils/requesting? data request-keys/create-shared-cart)
     :show-apple-pay?           (and (get-in data keypaths/show-apple-pay?)
                                     (seq (get-in data keypaths/shipping-methods))
                                     (seq (get-in data keypaths/states)))
     :disable-apple-pay-button? (get-in data keypaths/disable-apple-pay-button?)
     :update-line-item-requests (merge-with
                                 #(or %1 %2)
                                 (variants-requests data request-keys/add-to-bag (map :sku line-items))
                                 (variants-requests data request-keys/update-line-item (map :sku line-items)))
     :delete-line-item-requests (variants-requests data request-keys/delete-line-item variant-ids)
     :field-errors              (get-in data keypaths/field-errors)
     :error-message             (get-in data keypaths/error-message)
     :focused                   (get-in data keypaths/ui-focus)
     :the-ville?                (experiments/the-ville? data)
     :seventy-five-off-install? (experiments/seventy-five-off-install? data)
     :recently-added-skus       (get-in data keypaths/cart-recently-added-skus)}))

(defn empty-cart-query [data]
  {:promotions (get-in data keypaths/promotions)})

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
   (stylist-banner/built-component data nil)
   (promotion-banner/built-component data nil)
   #?(:cljs (popup/built-component data nil))

   (if (experiments/new-flyout? data)
     (header-new-flyout/built-component data nil)
     (header/built-component data nil))
   [:div.relative.flex.flex-column.flex-auto
    (scrim/built-component data nil)
    (flash/built-component data nil)

    [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
     (built-component data nil)]

    [:footer
     (storefront.footer/built-component data nil)]]])
