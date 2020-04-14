(ns checkout.classic-cart
  (:require
   #?@(:cljs [[storefront.components.popup :as popup]
              [storefront.components.payment-request-button :as payment-request-button]
              [storefront.hooks.browser-pay :as browser-pay]
              [storefront.hooks.quadpay :as quadpay]])
   [catalog.images :as catalog-images]
   [checkout.cart.summary :as cart-summary]
   [checkout.header :as header]
   [checkout.suggestions :as suggestions]
   [storefront.accessors.experiments :as experiments]
   [catalog.facets :as facets]
   [storefront.accessors.line-items :as line-items]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.products :as products]
   [storefront.accessors.promos :as promos]
   [storefront.accessors.stylists :as stylists]
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.flash :as flash]
   [storefront.components.footer :as storefront.footer]
   [storefront.components.money-formatters :as mf]
   [ui.promo-banner :as promo-banner]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.css-transitions :as css-transitions]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]))

(defn display-adjustable-line-items
  [recently-added-skus line-items skus update-line-item-requests delete-line-item-requests]
  (for [{sku-id :sku variant-id :id :as line-item} line-items
        :let [sku                  (get skus sku-id)
              price                (or (:sku/price line-item)
                                       (:unit-price line-item))
              removing?            (get delete-line-item-requests variant-id)
              updating?            (get update-line-item-requests sku-id)
              just-added-to-order? (contains? recently-added-skus sku-id)
              length-circle-value  (-> sku :hair/length first)]]
    [:div.pt1.pb2 {:key (str sku-id "-" (:quantity line-item))}
     [:div.left.pr1
      (when-not length-circle-value
        {:class "pr3"})
      (when length-circle-value
        [:div.right.z1.circle.stacking-context.border.border-gray.flex.items-center.justify-center.medium.h5.bg-white
         (css-transitions/background-fade
          just-added-to-order?
          {:key       (str "length-circle-" sku-id)
           :data-test (str "line-item-length-" sku-id)
           :style     {:margin-left "-21px"
                       :margin-top  "-10px"
                       :width       "32px"
                       :height      "32px"}})
         (str length-circle-value "”")])

      [:div.flex.items-center.justify-center.ml1
       (css-transitions/background-fade
        just-added-to-order?
        {:key       (str "thumbnail-" sku-id)
         :data-test (str "line-item-img-" (:catalog/sku-id sku))
         :style     {:width "79px" :height "74px"}})
       (ui/ucare-img
        {:width 75}
        (->> sku (catalog-images/image "cart") :ucare/id))]]

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
                                     :class  "stroke-black"})])]]
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
        [:div.h5.right {:data-test (str "line-item-price-ea-" sku-id)} (mf/as-money price) " each"]]]]]))

(defcomponent full-component [{:keys [order
                                      skus
                                      promo-banner
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
                                      freeinstall-just-added?
                                      loaded-quadpay?
                                      cart-summary]} owner _]
  [:div.container.p2
   (component/build promo-banner/sticky-organism promo-banner nil)

   [:div.clearfix.mxn3
    [:div.col-on-tb-dt.col-6-on-tb-dt.px3
     {:data-test "cart-line-items"}
     (display-adjustable-line-items recently-added-skus
                                    line-items
                                    skus
                                    update-line-item-requests
                                    delete-line-item-requests)
     (component/build suggestions/component suggestions nil)]

    [:div.col-on-tb-dt.col-6-on-tb-dt.px3

     (component/build cart-summary/component cart-summary nil)

     #?@(:cljs
         [(component/build quadpay/component
                           {:quadpay/order-total (:total order)
                            :quadpay/show?       loaded-quadpay?
                            :quadpay/directive   :just-select}
                           nil)])
     (ui/button-large-primary {:spinning? false
                               :disabled? updating?
                               :on-click  (utils/send-event-callback events/control-checkout-cart-submit)
                               :data-test "start-checkout-button"}
                              [:div "Check out"])

     [:div.h5.black.center.py1.flex.justify-around.items-center
      [:div.flex-grow-1.border-bottom.border-cool-gray]
      [:div.mx2 "or"]
      [:div.flex-grow-1.border-bottom.border-cool-gray]]

     [:div.pb2
      (ui/button-large-paypal {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
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
        (ui/button-large-secondary {:on-click  (utils/send-event-callback events/control-cart-share-show)
                                    :class     "border-width-2 border-black"
                                    :spinning? requesting-shared-cart?
                                    :data-test "share-cart"}
                                   [:div.flex.items-center.justify-center.bold
                                    (svg/share-arrow {:class  "stroke-black mr1 fill-black"
                                                      :width  "24px"
                                                      :height "24px"})
                                    "Share your bag"])])]]])

(defcomponent empty-component [{:keys [promotions]} owner _]
  (ui/narrow-container
   [:div.p2
    [:.center {:data-test "empty-cart"}
     [:div.m2 ^:inline (svg/bag {:style {:height "70px" :width "70px"}
                                 :class "fill-black"})]

     [:p.m2.h2.light "Your bag is empty."]

     [:div.m2
      (if-let [promo (promos/default-advertised-promotion promotions)]
        (:description promo))]]

    (ui/button-large-primary (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
                             "Shop Our Looks")]))

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
     :recently-added-skus        (get-in data keypaths/cart-recently-added-skus)}))

(defn empty-cart-query
  [data]
  {:promotions              (get-in data keypaths/promotions)})

(defcomponent component
  [{:keys [fetching-order?
           item-count
           empty-cart
           full-cart]} owner opts]
  (if fetching-order?
    [:div.py3.h2 ui/spinner]
    [:div.col-7-on-dt.mx-auto
     (if (zero? item-count)
       (component/build empty-component empty-cart opts)
       (component/build full-component full-cart opts))]))

(defn query [data]
  {:fetching-order? (utils/requesting? data request-keys/get-order)
   :item-count      (orders/product-quantity (get-in data keypaths/order))
   :empty-cart      (empty-cart-query data)
   :full-cart       (full-cart-query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn layout [app-state nav-event]
  [:div.flex.flex-column.stretch {:style {:margin-bottom "-1px"}}
   #?(:cljs (popup/built-component app-state nil))

   ^:inline (promo-banner/built-static-organism app-state nil)
   (header/built-component app-state nil)
   [:div.relative.flex.flex-column.flex-auto
    (flash/built-component app-state nil)

    [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
     (built-component app-state nil)]

    [:footer
     (storefront.footer/built-component app-state nil)]]])
