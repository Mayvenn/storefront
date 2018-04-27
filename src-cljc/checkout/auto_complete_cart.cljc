(ns checkout.auto-complete-cart
  (:require
   #?@(:cljs [[goog.dom]
              [goog.events]
              [goog.events.EventType :as EventType]
              [goog.style]
              [storefront.component :as component]
              [storefront.components.popup :as popup]
              [storefront.components.order-summary :as summary]
              [om.core :as om]]
       :clj [[storefront.component-shim :as component]])
   [clojure.string :as string]
   [spice.core :as spice]
   [storefront.accessors.experiments :as experiments]
   [storefront.components.money-formatters :as mf]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.promos :as promos]
   [storefront.accessors.products :as products]
   [storefront.accessors.stylists :as stylists]
   [checkout.header :as header]
   [storefront.components.footer :as storefront.footer]
   [storefront.components.affirm :as affirm]
   [storefront.components.promotion-banner :as promotion-banner]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.components.flash :as flash]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.css-transitions :as css-transitions]
   [storefront.request-keys :as request-keys]
   [storefront.accessors.facets :as facets]
   [storefront.accessors.images :as images]
   [storefront.components.ui :as ui]
   [storefront.components.stylist-banner :as stylist-banner]
   [storefront.events :as events]
   [storefront.platform.component-utils :as utils]
   [storefront.components.svg :as svg]
   [checkout.cart :as cart]))

(defn transition-background-color [& content]
  (css-transitions/transition-element
   {:transitionName          "line-item-fade"
    :transitionAppearTimeout 1100
    :transitionAppear        true
    :transitionEnter         true
    :transitionEnterTimeout  1100}
   content))

(defn display-adjustable-line-items
  [line-items skus update-line-item-requests delete-line-item-requests]
  (for [{sku-id :sku variant-id :id :as line-item} line-items
        :let [sku               (get skus sku-id)
              legacy-variant-id (or (:legacy/variant-id line-item) (:id line-item))
              price             (or (:sku/price line-item)         (:unit-price line-item))
              thumbnail         (merge
                                 (images/cart-image sku)
                                 {:data-test (str "line-item-img-" (:catalog/sku-id sku))})
              removing?         (get delete-line-item-requests variant-id)
              updating?         (get update-line-item-requests sku-id)]]
    [:div.pt1.pb2 {:key legacy-variant-id}
     [:div.left.pr1
      (transition-background-color
       (when-let [length (-> sku :hair/length first)]
         [:div.right.z1.circle.stacking-context.border.border-light-gray.flex.items-center.justify-center.medium.h5.bg-too-light-teal
          {:style {:margin-left "-21px"
                   :margin-top  "-10px"
                   :width       "32px"
                   :height      "32px"}} (str length "\"")]))
      (transition-background-color
       [:div.flex.items-center.justify-center.ml1 {:style {:width "79px" :height "79px"}}
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
          (let [{:keys [focused coupon-code field-errors updating? applying?]} promo-data]
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
                 :errors        (get field-errors ["promo-code"])
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
          (when (or (not (= price 0)) (#{"amazon" "freeinstall"} coupon-code))
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

(defn full-component [{:keys [focused
                              order
                              line-items
                              skus
                              products
                              coupon-code
                              promotion-banner
                              applying-coupon?
                              updating?
                              redirecting-to-paypal?
                              share-carts?
                              requesting-shared-cart?
                              show-apple-pay?
                              disable-apple-pay-button?
                              update-line-item-requests
                              delete-line-item-requests
                              field-errors
                              the-ville?
                              show-green-banner?]} owner]
  (component/create
   [:div.container.p2
    (component/build cart/deploy-promotion-banner-component promotion-banner nil)

    (when the-ville?
      [:div.mb3
       (if show-green-banner?
         cart/free-install-cart-promo
         cart/ineligible-free-install-cart-promo)])

    [:div.clearfix.mxn3
     [:div.col-on-tb-dt.col-6-on-tb-dt.px3
      {:data-test "cart-line-items"}
      (display-adjustable-line-items line-items
                                     skus
                                     update-line-item-requests
                                     delete-line-item-requests)]

     [:div.col-on-tb-dt.col-6-on-tb-dt.px3
      (display-order-summary order
                             {:read-only?        false
                              :use-store-credit? false
                              :promo-data        {:coupon-code  coupon-code
                                                  :applying?    applying-coupon?
                                                  :focused      focused
                                                  :field-errors field-errors}})

      (affirm/auto-complete-as-low-as-box {:amount (:total order)})

      (ui/teal-button {:spinning? false
                       :disabled? updating?
                       :on-click  (utils/send-event-callback events/control-checkout-cart-submit)
                       :class     "py2"
                       :data-test "start-checkout-button"}
                      [:div.p1 "Check out"])

      [:div.h5.black.center.py1.flex.justify-around.items-center
       [:div.flex-grow-1.border-bottom.border-light-gray]
       [:div.mx2 "or"]
       [:div.flex-grow-1.border-bottom.border-light-gray]]

      [:div.pb2
       (ui/aqua-button {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                        :spinning? redirecting-to-paypal?
                        :disabled? updating?
                        :data-test "paypal-checkout"}
                       [:div.p1
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
                          [:div.flex.items-center.justify-center.bold.p1
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

     (ui/teal-button (utils/route-to events/navigate-shop-by-look {:album-slug "look"})
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

(defn full-cart-query [data]
  (let [order       (get-in data keypaths/order)
        products    (get-in data keypaths/v2-products)
        facets      (get-in data keypaths/v2-facets)
        line-items  (map (partial add-product-title-and-color-to-line-item products facets) (orders/product-items order))
        variant-ids (map :id line-items)]
    {:order                     order
     :line-items                line-items
     :skus                      (get-in data keypaths/v2-skus)
     :products                  products
     :show-green-banner?        (and (orders/bundle-discount? order)
                                     (-> order :promotion-codes set (contains? "freeinstall")))
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
     :focused                   (get-in data keypaths/ui-focus)
     :the-ville?                (experiments/the-ville? data)}))

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
  {:fetching-order?  (utils/requesting? data request-keys/get-order)
   :item-count       (orders/product-quantity (get-in data keypaths/order))
   :empty-cart       (empty-cart-query data)
   :full-cart        (full-cart-query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn layout [data nav-event]
  [:div.flex.flex-column {:style {:min-height    "100vh"
                                  :margin-bottom "-1px"}}

   (stylist-banner/built-component data nil)
   (promotion-banner/built-component data nil)
   #?(:cljs (popup/built-component data nil))
   [:header (header/built-component data nil)]

   (flash/built-component data nil)

   [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
    (built-component data nil)]

   [:footer
    (storefront.footer/built-component data nil)]])
