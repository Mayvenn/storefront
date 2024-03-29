(ns checkout.shop.filled-cart-v2020-09
  (:require
   #?@(:cljs [[storefront.components.payment-request-button :as payment-request-button]
              [storefront.components.popup :as popup]
              [storefront.hooks.quadpay :as quadpay]])
   [api.catalog :refer [select ?a-la-carte ?discountable ?physical ?recent ?service ?wig ?bundle-deal]]
   api.current
   api.orders
   catalog.keypaths
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
   [ui.promo-banner :as promo-banner]
   [mayvenn.visual.tools :refer [within]]
   [mayvenn.concept.account :as accounts]
   [mayvenn.concept.booking :as booking]
   [mayvenn.live-help.core :as live-help]))

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
           [request-key-prefix request-keys/delete-line-item]
           [request-key-prefix request-keys/remove-servicing-stylist]])))

;; formatters

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

;;; -----------------------------------

(defn shipping-delay-hack
  [{:shipping-delay/keys [show?]}]
  (when show?
    [:div.bg-warning-yellow.border.border-warning-yellow
     [:div.bg-lighten-4.p3
      [:span.bold "Shipping Delay: "]
      [:span "There may be a slight delay in shipping and deliveries as we observe Memorial Day. We apologize for any inconvenience."]]]))

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
  [{:keys [service-line-items stylist service-section-id] :as data}]
  (when service-section-id
    [:div.mb3
     {:data-test service-section-id}
     [:div.title-2.proxima.mb1 "Services"]
     (component/build cart-item-v202004/stylist-organism stylist nil)
     (component/build cart-item-v202004/no-stylist-organism stylist nil)

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
           checkout-wo-mayvenn-install
           quadpay
           browser-pay?
           checkout-caption]
    :as data}
   _ _]
  [:div.container.px2
   (component/build promo-banner/sticky-organism promo-banner nil)
   [:div.clearfix.mxn3
    [:div
     [:div.bg-refresh-gray.px3.pb3.col-on-tb-dt.col-6-on-tb-dt.bg-white-on-tb-dt.p3-on-mb
      (shipping-delay-hack data)
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
           {:width 50 :alt   ""}
           ;; Note: We are not using ucare-id because stylist portraits may have
           ;; ucarecdn crop parameters saved into the url
           (ui/square-image {:resizable-url servicing-stylist-portrait-url} 50))]
         [:div.left-align
          checkout-caption-copy]])

      ;; CTAs for checking out
      [:div.py2
       ;; cta button
       (let [{:cta/keys [id content disabled? disabled-reason target]} cta]
         (when id
           [:div
            (ui/button-large-primary {:data-test id
                                      :data-ref  id
                                      :disabled? disabled?
                                      :on-click  (apply utils/send-event-callback target)}
                                     content)
            (when disabled-reason
              [:div.content-2.mt2
               {:data-test "checkout-disabled-reason"}
               disabled-reason])]))

       [:div
        [:div.h5.black.py1.flex.items-center
         [:div.flex-grow-1.border-bottom.border-gray]
         [:div.mx2 "or"]
         [:div.flex-grow-1.border-bottom.border-gray]]

        ;; paypal button
        (when-let [{:keys [spinning? disabled? id]} paypal]
          [:div
           (ui/button-large-paypal
            {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
             :disabled? disabled?
             :spinning? spinning?
             :data-test id}
            (component/html
             [:div.bg-paypal
              "Check out with "
              [:span.medium.italic "PayPal™"]]))])

        ;; "checkout without mayvenn install" button
        (when-let [{:keys [id target disabled? spinning?]} checkout-wo-mayvenn-install]
          [:div
           (ui/button-large-ghost
            {:on-click  (apply utils/send-event-callback target)
             :disabled? disabled?
             :spinning? spinning?
             :data-test id}
            "Checkout without Mayvenn Install")])]

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


(defn clear-cart-link
  [{:clear-cart-link/keys [id target primary]}]
  (component/html
   (when id
     [:div.mx2-on-mb.my1
      (ui/button-small-secondary
       (merge
        {:class "py0"}
        (apply utils/fake-href target))
       [:span.flex.items-center.px2.proxima.title-3.shout.black primary])])))

(defcomponent template
  [{:keys [header footer popup flash live-help-bug cart nav-event]} _ _]
  [:div
   [:div.flex.flex-column.stretch
    {:style {:margin-bottom "-1px"}}
    #?(:cljs (popup/built-component popup nil))
    (when-let [promo-banner (:promo-banner cart)]
      (promo-banner/static-organism promo-banner nil nil))
    (header/built-component header nil)
    [:div.relative.flex.flex-column.flex-auto
     (flash/built-component flash nil)

     [:main.bg-white.flex-auto
      {:data-test (keypaths/->component-str nav-event)}
      [:div
       [:div.hide-on-tb-dt
        [:div.border-bottom.border-gray.border-width-1.m-auto.col-7-on-dt
         [:div.flex.justify-between
          [:div.px2.my2.flex.items-center (ui-molecules/return-link (:return-link cart))]
          (clear-cart-link (:clear-cart-link cart))]]]
       [:div.hide-on-mb.col-7-on-dt.mx-auto
        [:div.m-auto.container
         [:div.flex.justify-between
          [:div.px2.my2 (ui-molecules/return-link (:return-link cart))]
          (clear-cart-link (:clear-cart-link cart))]]]
       [:div.col-7-on-dt.mx-auto
        (component/build full-component cart)]]]
     [:footer
      (storefront.footer/built-component footer nil)]]]
   (live-help/bug-component live-help-bug)])

;;; --------------

(defn cart-items|addons|SRV<-
  "GROT(SRV)"
  [addons]
  (when (seq addons)
    {:cart-item-modify-button/content "Edit Add-Ons"
     :cart-item-sub-items/id          "addon-services"
     :cart-item-sub-items/title       "Add-On Services"
     :cart-item-sub-items/items       (map (fn [addon-sku]
                                             {:cart-item-sub-item/title  (:sku/title addon-sku)
                                              :cart-item-sub-item/price  (some-> addon-sku :sku/price mf/as-money)
                                              :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                           addons)}))

(defn cart-items|addons<-
  [item-facets]
  (when (seq item-facets)
    {:cart-item-modify-button/content "Edit Add-Ons"
     :cart-item.addons/id             "addon-services"
     :cart-item.addons/title          "Add-On Services"
     :cart-item.addons/elements
     (->> item-facets
          (mapv (fn [facet]
                  {:cart-item.addon/title (:facet/name facet)
                   :cart-item.addon/price (some-> facet :service/price mf/as-money)
                   :cart-item.addon/id    (:service/sku-part facet)})))}))

(defn free-services<-
  [items delete-line-item-requests]
  (for [{:as                         free-service
         :catalog/keys               [sku-id]
         :item/keys                  [id variant-name quantity unit-price]
         :item.service/keys          [addons]
         :join/keys                  [addon-facets]
         :promo.mayvenn-install/keys [hair-missing-quantity requirement-copy]
         :hacky/keys                 [promo-mayvenn-install-requirement-copy]
         :product/keys               [essential-title essential-price essential-inclusions]
         :copy/keys                  [whats-included]
         :legacy/keys                [variant-id]}

        (select ?discountable items)
        :let [requirement-copy (or requirement-copy
                                   promo-mayvenn-install-requirement-copy)
              required-hair-quantity-met? (not (pos? hair-missing-quantity))
              ;; GROT(SRV) remove unit price here, deprecated key
              price (some-> (or essential-price unit-price) (* quantity) mf/as-money)]]
    (merge
     {:react/key                               "freeinstall-line-item-freeinstall"
      :cart-item-title/id                      "line-item-title-upsell-free-service"
      :cart-item-title/primary                 (if essential-title
                                                 essential-title
                                                 variant-name) ;; GROT(SRV)
      :cart-item-copy/lines                    [{:id    (str "line-item-whats-included-" sku-id)
                                                 :value (if required-hair-quantity-met?
                                                          (str "You're all set! "
                                                               (or essential-inclusions
                                                                   whats-included)) ;; GROT(SRV) deprecated key
                                                          requirement-copy)}
                                                {:id    (str "line-item-quantity-" sku-id)
                                                 :value (str "qty. " quantity)}]
      :cart-item-floating-box/id               "line-item-freeinstall-price"
      :cart-item-floating-box/contents         (if required-hair-quantity-met?
                                                 [{:text price :attrs {:class "strike"}}
                                                  {:text "Free" :attrs {:class "p-color shout"}}]
                                                 [{:text price}])
      :cart-item-remove-action/id              "line-item-remove-freeinstall"
      :cart-item-remove-action/aria-label      "remove freeinstall"
      :cart-item-remove-action/spinning?       (boolean (get delete-line-item-requests id))
      :cart-item-remove-action/target          [events/control-cart-remove variant-id]
      :cart-item-service-thumbnail/id          "freeinstall"
      :cart-item-service-thumbnail/image-url   (hacky-cart-image free-service)
      :cart-item-modify-button/id              "browse-addons"
      :cart-item-modify-button/target          [events/control-show-addon-service-menu]
      :cart-item-modify-button/tracking-target [events/browse-addon-service-menu-button-enabled]
      :cart-item-modify-button/content         "+ Browse Add-Ons"}
     (cart-items|addons|SRV<- addons)
     (cart-items|addons<- addon-facets))))

(defn ^:private a-la-carte-services<-
  [items delete-line-item-requests]
  (for [{:as           item
         :catalog/keys [sku-id]
         :hacky/keys   [cart-title]
         :sku/keys     [price]
         :copy/keys    [whats-included]
         :product/keys [essential-inclusions]
         :item/keys    [id quantity unit-price product-name]}

        (select ?a-la-carte items)]
    {:react/key                             sku-id
     :cart-item-title/primary               (or cart-title product-name)
     :cart-item-title/id                    (str "line-item-" sku-id)
     :cart-item-floating-box/id             (str "line-item-" sku-id "-price")
     :cart-item-floating-box/contents       [{:text (some-> (or price unit-price) mf/as-money)}]
     :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                              :value (or essential-inclusions
                                                         whats-included)} ;; GROT(SRV) deprecated key
                                             {:id    (str "line-item-quantity-" sku-id)
                                              :value (str "qty. " quantity)}]
     :cart-item-remove-action/id            (str "line-item-remove-" sku-id)
     :cart-item-remove-action/aria-label    (str "remove " (or cart-title product-name))
     :cart-item-remove-action/spinning?     (boolean (get delete-line-item-requests id))
     :cart-item-remove-action/target        [events/control-cart-remove id]
     :cart-item-service-thumbnail/id        sku-id
     :cart-item-service-thumbnail/image-url (hacky-cart-image item)}))

(defn ^:private service-items<-
  [stylist items remove-in-progress? delete-line-item-requests order-appointment-time-slot]
  (let [services   (select ?service items)
        stylist-id (:stylist/id stylist)]
    (merge
     (when (or stylist-id
               (seq services))
       {:service-section-id "service-section"})

     {:stylist (if stylist-id
                 (merge
                  {:servicing-stylist-portrait-url                        (-> stylist :stylist/portrait :resizable-url)
                   :servicing-stylist-banner/id                           "servicing-stylist-banner"
                   :servicing-stylist-banner/title                        (:stylist/name stylist)
                   :servicing-stylist-banner/rating                       {:rating/value (:stylist.rating/score stylist)
                                                                           :rating/id    "stylist-rating-id"}
                   :servicing-stylist-banner/image-url                    (some-> stylist :stylist/portrait :resizable-url)
                   :servicing-stylist-banner/title-and-image-target       [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                                                      :store-slug (:stylist/slug stylist)}]
                   :servicing-stylist-banner.swap-icon/target             [events/control-change-stylist {:stylist-id stylist-id}]
                   :servicing-stylist-banner.swap-icon/id                 "stylist-swap"
                   :servicing-stylist-banner.edit-appointment-icon/target [events/control-show-edit-appointment-menu]
                   :servicing-stylist-banner.edit-appointment-icon/id      "edit-appointment"
                   :servicing-stylist-banner.remove-icon/spinning?        remove-in-progress?
                   :servicing-stylist-banner.remove-icon/target           [events/control-remove-stylist {:stylist-id stylist-id}]
                   :servicing-stylist-banner.remove-icon/id               "remove-stylist"}
                  (within :servicing-stylist-banner.appointment-time-slot order-appointment-time-slot))
                 {:stylist-organism/id            "stylist-organism"
                  :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})}

     (when (seq services)
       {:service-line-items (concat
                             (free-services<- items delete-line-item-requests)
                             (a-la-carte-services<- items delete-line-item-requests))}))))

(defn physical-items<-
  [items update-line-item-requests delete-line-item-requests products-catalog]
  (for [{:as           item
         :catalog/keys [sku-id]
         :sku/keys     [price]
         :item/keys    [id quantity unit-price product-name]
         :hacky/keys   [cart-title]
         :join/keys    [facets]}
        (select ?physical items)

        :let
        [qty-adjustment-args {:variant {:id id :sku sku-id}}
         removing?           (get delete-line-item-requests id)
         updating?           (get update-line-item-requests sku-id)
         relevant-product    (get products-catalog (-> item :selector/from-products first))]]
    {:react/key                                      (str sku-id "-" quantity)
     :cart-item-title/id                             (str "line-item-title-" sku-id)
     :cart-item-title/primary                        (or cart-title product-name)
     :cart-item-title/secondary                      (ui/sku-card-secondary-text item)
     :cart-item-title/target                         (when relevant-product
                                                       [events/navigate-product-details
                                                        {:catalog/product-id (:catalog/product-id relevant-product)
                                                         :page/slug          (:page/slug relevant-product)
                                                         :query-params       {:SKU sku-id}}])
     :cart-item-floating-box/id                      (str "line-item-price-ea-with-label-" sku-id)
     :cart-item-floating-box/contents                (let [price (mf/as-money (or price unit-price))]
                                                       [{:text price :attrs {:data-test (str "line-item-price-ea-" sku-id)}}
                                                        {:text " each" :attrs {:class "proxima content-4"}}])
     :cart-item-square-thumbnail/id                  sku-id
     :cart-item-square-thumbnail/sku-id              sku-id
     :cart-item-square-thumbnail/sticker-label       (when-let [length-circle-value (some-> item :hair/length first)]
                                                       (str length-circle-value "”"))
     :cart-item-square-thumbnail/ucare-id            (hacky-cart-image item)
     :cart-item-square-thumbnail/aria-label          (or cart-title product-name)
     :cart-item-square-thumbnail/target              (when relevant-product
                                                       [events/navigate-product-details
                                                        {:catalog/product-id (:catalog/product-id relevant-product)
                                                         :page/slug          (:page/slug relevant-product)
                                                         :query-params       {:SKU sku-id}}])
     :cart-item-adjustable-quantity/id               (str "line-item-quantity-" sku-id)
     :cart-item-adjustable-quantity/spinning?        updating?
     :cart-item-adjustable-quantity/value            quantity
     :cart-item-adjustable-quantity/id-suffix        sku-id
     :cart-item-adjustable-quantity/decrement-target [events/control-cart-line-item-dec qty-adjustment-args]
     :cart-item-adjustable-quantity/increment-target [events/control-cart-line-item-inc qty-adjustment-args]
     :cart-item-remove-action/id                     (str "line-item-remove-" sku-id)
     :cart-item-remove-action/aria-label             (str "remove " (or cart-title product-name))
     :cart-item-remove-action/spinning?              removing?
     :cart-item-remove-action/target                 [events/control-cart-remove id]}))

(defn coupon-code->remove-promo-action [coupon-code]
  {:cart-summary-line/action-id     "cart-remove-promo"
   :cart-summary-line/action-icon   [:svg/close-x {:class "stroke-white fill-gray" }]
   :cart-summary-line/action-target [events/control-checkout-remove-promotion {:code coupon-code}]})

(defn shipping-method-summary-line-query
  [shipping-method line-items-excluding-shipping]
  (let [free-shipping? (= "WAITER-SHIPPING-1" (:sku shipping-method))
        only-services? (every? line-items/service? line-items-excluding-shipping)
        drop-shipping? (->> (map :variant-attrs line-items-excluding-shipping)
                            (select {:warehouse/slug #{"factory-cn"}})
                            boolean)]
    (when (and shipping-method (not (and free-shipping? only-services?)))
      {:cart-summary-line/id        "shipping"
       :cart-summary-line/primary   "Shipping"
       :cart-summary-line/secondary (-> shipping-method :sku (shipping/timeframe drop-shipping?))
       :cart-summary-line/value     (->> shipping-method
                                        vector
                                        (apply (juxt :quantity :unit-price))
                                        (reduce * 1)
                                        mf/as-money-or-free)})))

(defn regular-cart-summary-query
  [{:as   order
    :keys [adjustments tax-total total]} items]
  (let [subtotal            (orders/products-and-services-subtotal order)
        bundle-deal-items   (select ?bundle-deal items)
        bundle-deal-missing (->> bundle-deal-items (map :item/quantity) (reduce + 0) (- 3) (max 0))]
    {:cart-summary-total-line/id    "total"
     :cart-summary-total-line/label "Total"
     :cart-summary-total-line/value (some-> total mf/as-money)

     :cart-summary/id               "cart-summary"
     :cart-summary/lines            (concat [{:cart-summary-line/id      "subtotal"
                                              :cart-summary-line/primary "Subtotal"
                                              :cart-summary-line/value   (mf/as-money subtotal)}]

                                            (when-let [shipping-method-summary-line
                                                       (shipping-method-summary-line-query
                                                        (orders/shipping-item order)
                                                        (orders/product-and-service-items order))]
                                              [shipping-method-summary-line])

                                            (for [{:keys [name price coupon-code]
                                                   :as   adjustment}
                                                  (filter adjustments/non-zero-adjustment? adjustments)]
                                              (cond-> {:cart-summary-line/id      (text->data-test-name name)
                                                       :cart-summary-line/icon    [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                                      :height "2em"
                                                                                                      :width  "2em"}]
                                                       :cart-summary-line/primary (adjustments/display-adjustment-name adjustment)
                                                       :cart-summary-line/value   (mf/as-money-or-free price)}

                                                coupon-code
                                                (merge (coupon-code->remove-promo-action coupon-code))))

                                            (when (and bundle-deal-items (pos? bundle-deal-missing))
                                              [{:cart-summary-line/id      "bundle-deal-promo"
                                                :cart-summary-line/icon    [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                               :height "2em"
                                                                                               :width  "2em"}]
                                                :cart-summary-line/primary "Bundle Discount"
                                                :cart-summary-line/value   (str "Add " bundle-deal-missing
                                                                                (if (= bundle-deal-missing 1) " bundle" " bundles"))}])

                                            (when (pos? tax-total)
                                              [{:cart-summary-line/id      "tax"
                                                :cart-summary-line/primary "Tax"
                                                :cart-summary-line/value   (mf/as-money tax-total)}]))}))

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
                                   {:args    {:data-test  "cart-apply-promo"
                                              :disabled?  disabled?
                                              :aria-label "apply promo code"
                                              :on-click   (utils/send-event-callback events/control-cart-update-coupon)
                                              :style      {:width   "55px"
                                                           :padding "0"}}
                                    :content (svg/forward-arrow {:class (if disabled?
                                                                          "fill-gray"
                                                                          "fill-white")
                                                                 :style {:width  "14px"
                                                                         :height "14px"}})}}]
      {:promo-field-data
       (cond-> {:submit-target [events/control-cart-update-coupon]}

         show-promo-code-field?
         (merge input-group-attrs)

         promo-link?
         (merge
          {:field-reveal/id     "reveal-promo-entry"
           :field-reveal/target [events/control-toggle-promo-code-entry]
           :field-reveal/label  (if show-promo-code-field?
                                  "Hide promo code"
                                  "Add promo code")}))})))

(defn cart-summary<-
  [order items]
  (regular-cart-summary-query order items))

(defn cta<-
  [no-items? hair-missing-quantity pending-requests?]
  {:cta/id              "start-checkout-button"
   :cta/disabled?       (or no-items?
                            pending-requests?
                            (pos? hair-missing-quantity))
   :cta/target          [events/control-checkout-cart-submit]
   :cta/content         "Check out"
   :cta/disabled-reason (cond
                          no-items?                    "Please add a product or service to check out"
                          (pos? hair-missing-quantity) (str "Add " hair-missing-quantity " more " (ui/pluralize hair-missing-quantity "item")))})

(defn return-link<-
  [items]
  (let [recent-texture (some->> items
                                (select (merge ?recent ?physical))
                                (mapv :hair/texture)
                                ffirst
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
  [state items pending-requests?]
  (let [{:as service
         :promo.mayvenn-install/keys [hair-missing-quantity]}
        (first (select ?service items))]
    (when (or (empty? service)
              (and service (zero? hair-missing-quantity)))
      {:id        "paypal-checkout"
       :spinning? (get-in state keypaths/cart-paypal-redirect)
       :disabled? (or (zero? (count items))
                      pending-requests?)})))

(defn checkout-wo-mayvenn-install<-
  [hair-missing-quantity pending-requests? delete-line-item-requests {:keys [id]}]
  (when (pos? hair-missing-quantity)
    {:id        "checkout-wo-mayvenn-install"
     :disabled? pending-requests?
     :spinning? (get delete-line-item-requests id)
     :target    [events/control-cart-remove id]}))

(defn checkout-caption<-
  ;; TODO(corey) This name seems confusing to me
  [items easy-booking? {:mayvenn.concept.booking/keys [selected-date selected-time-slot]}]
  (when-let [services (select ?service items)]
    (if-let [stylist (:item.service/stylist (first services))]
      {:checkout-caption-copy          (let [booking-selected (and selected-date selected-time-slot)]
                                         (str "You've selected "
                                             (stylists/->display-name stylist)
                                             " as your stylist. A Concierge Specialist will reach out to you within 3 business days to "
                                             (if (and easy-booking? booking-selected)
                                               "confirm"
                                               "coordinate")
                                             " your appointment."))
       :servicing-stylist-portrait-url (-> stylist :portrait :resizable-url)}
      {:checkout-caption-copy          "You'll be able to select your Mayvenn Certified Stylist after checkout."
       :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})))

;; TODO(corey) This only uses session
(defn shared-cart<-
  [data]
  (let [{signed-in-as ::auth/as} (auth/signed-in data)]
    (when (or (= :stylist signed-in-as)
              (= "retail-location" (get-in data keypaths/store-experience)))
      {:requesting-shared-cart? (utils/requesting? data request-keys/create-shared-cart)})))

(defn clear-cart-link<- [app-state]
  (when (or (auth/stylist? (auth/signed-in app-state))
            (= "retail-location" (get-in app-state keypaths/store-experience)))
    {:clear-cart-link/target  [events/cart-cleared]
     :clear-cart-link/id      "clear-cart"
     :clear-cart-link/primary "Clear Cart"}))

(defn ^:export page
  [app-state nav-event]
  (let [waiter-order                                (get-in app-state keypaths/order)
        {:keys [:services/stylist]}                 (api.orders/services app-state waiter-order)
        {:free-mayvenn-service/keys [hair-missing-quantity
                                     service-item]} (api.orders/free-mayvenn-service stylist waiter-order)
        {:order/keys [items]}                       (api.orders/current app-state)

        ;; TODO(corey) these are session
        pending-requests?         (update-pending? app-state)
        remove-in-progress?       (utils/requesting? app-state request-keys/remove-servicing-stylist)
        easy-booking?             (experiments/easy-booking? app-state)
        booking                   (booking/<- app-state)
        update-line-item-requests (merge-with #(or %1 %2)
                                              (->> (map :catalog/sku-id items)
                                                   (variants-requests app-state request-keys/add-to-bag))
                                              (->> (map :catalog/sku-id items)
                                                   (variants-requests app-state request-keys/update-line-item)))
        delete-line-item-requests (->> (map :item/id items)
                                       (variants-requests app-state request-keys/delete-line-item))

        ;; TODO(corey) part item model / part order model
        suggestions (suggestions/consolidated-query app-state)
        no-items?   (empty? items)]
    (component/build template
                     {:cart {:return-link          (return-link<- items)
                             :clear-cart-link      (clear-cart-link<- app-state)
                             :promo-banner         (when (zero? (orders/product-quantity waiter-order))
                                                     (promo-banner/query app-state))
                             :cta                  (cta<- no-items? hair-missing-quantity pending-requests?)
                             :shipping-delay/show? (and (:show-shipping-delay (get-in app-state keypaths/features))
                                                        (->> (orders/product-and-service-items waiter-order)
                                                             (map :variant-attrs)
                                                             ;; Saddlecreek skus don't have a warehouse
                                                             (remove :warehouse/slug)
                                                             seq))
                             :physical-items       (physical-items<- items
                                                                     update-line-item-requests
                                                                     delete-line-item-requests
                                                                     (get-in app-state keypaths/v2-products))
                             :service-items        (service-items<-
                                                    (api.current/stylist app-state)
                                                    items
                                                    remove-in-progress?
                                                    delete-line-item-requests
                                                    (:appointment-time-slot waiter-order))

                             :checkout-caption            (checkout-caption<- items easy-booking? booking)
                             :cart-summary                (merge (cart-summary<- waiter-order items)
                                                                 (promo-input<- app-state
                                                                                waiter-order
                                                                                pending-requests?))
                             :shared-cart                 (shared-cart<- app-state)
                             :quadpay                     (quadpay<- app-state waiter-order)
                             :paypal                      (paypal<- app-state
                                                                    items
                                                                    pending-requests?)
                             :checkout-wo-mayvenn-install (checkout-wo-mayvenn-install<-
                                                           hair-missing-quantity
                                                           pending-requests?
                                                           delete-line-item-requests
                                                           service-item)
                             :browser-pay?                (and (get-in app-state keypaths/loaded-stripe)
                                                               (experiments/browser-pay? app-state)
                                                               (seq (get-in app-state keypaths/shipping-methods))
                                                               (seq (get-in app-state keypaths/states)))
                             :suggestions                 suggestions}
                      :header        app-state
                      :footer        app-state
                      :popup         app-state
                      :flash         app-state
                      :live-help-bug app-state
                      :nav-event     nav-event})))
