(ns storefront.components.shared-cart
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]
                       [storefront.hooks.quadpay :as quadpay]
                       [storefront.components.payment-request-button :as payment-request-button]])
            [api.catalog :refer [select ?discountable ?physical ?service]]
            api.orders
            api.stylist
            [catalog.images :as catalog-images]
            [catalog.products :as products]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [checkout.ui.cart-summary-v202004 :as cart-summary]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.shared-cart :as shared-cart]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.flash :as flash]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            [storefront.transitions :as transitions]))

(defcomponent component
  [{:keys [spinning? shared-cart-id shared-cart-promotion store fetching-products? creating-cart? advertised-promo]}
   owner
   opts]
  (let [{:keys [portrait store-nickname]} store]
    (if spinning?
      [:div.container.p4
       ui/spinner]
      [:div.container.p4
       [:div.pb3
        (when (:resizable-url portrait)
          [:div.mb2.h2
           (ui/circle-picture {:class "mx-auto"} (ui/square-image portrait 96))])
        [:p.center.h3.medium {:data-test "cart-creator-nickname"}
         store-nickname " has created a bag for you!"]]
       [:div.flex.items-center.px1.py3.border-top.border-bottom
        (ui/ucare-img {:width 90} "8787e30c-2879-4a43-8d01-9d6790575084")
        [:div.ml2.flex-auto
         [:p.medium.shout.mb2 "Free shipping & 30 day guarantee"]
         [:p "Shop with confidence: Wear it, dye it, even color it. "
          "If you do not love your Mayvenn hair we will exchange it within 30 days of purchase!"]]]
       [:div.p3.h4.center
        (or (:description shared-cart-promotion)
            (:description advertised-promo))]
       [:form
        {:on-submit (utils/send-event-callback events/control-create-order-from-shared-cart
                                               {:shared-cart-id shared-cart-id})}
        (ui/submit-button "View your bag"
                          {:data-test "create-order-from-shared-cart"
                           :spinning? (or fetching-products?
                                          creating-cart?)
                           :disabled? (or fetching-products?
                                          creating-cart?)})]])))

(defn shared-cart-promotion
  [data]
  (let [shared-cart    (get-in data keypaths/shared-cart-current)
        promotion-code (some-> shared-cart :promotion-codes first)
        all-promotions (get-in data keypaths/promotions)]
    (first (filter #(= promotion-code (:code %)) all-promotions))))

(defn query
  [data]
  {:shared-cart-id        (get-in data keypaths/shared-cart-id)
   :shared-cart-promotion (shared-cart-promotion data)
   :store                 (or ;; If stylist fails to be fetched, then it falls back to current store
                           (get-in data keypaths/shared-cart-creator)
                           (get-in data keypaths/store))
   :advertised-promo      (promos/default-advertised-promotion (get-in data keypaths/promotions))
   :fetching-products?    (utils/requesting? data (conj request-keys/get-products {}))
   :creating-cart?        (utils/requesting? data request-keys/create-order-from-shared-cart)
   :spinning?             (utils/requesting? data request-keys/fetch-shared-cart)})

(defn service-line-item-query ;; will need to pass in freeinstall-ness
  [{:keys [catalog/sku-id item/applied-promotions sku/price] :as service-sku} service-product]
  {:react/key                             (str "service-line-item-" sku-id)
   :cart-item-title/id                    "line-item-title-upsell-service"
   :cart-item-title/primary               (or (:copy/title service-product) (:legacy/product-name service-sku))
   :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                            :value (:copy/whats-included service-sku)}
                                           {:id    (str "line-item-quantity-" sku-id)
                                            :value (str "qty. " (:item/quantity service-sku))}]
   :cart-item-floating-box/id             "line-item-service-price"
   :cart-item-floating-box/contents       (if (not-empty applied-promotions) ;; TODO increase the resilience
                                            [{:text (mf/as-money price) :attrs {:class "strike"}}
                                             {:text "FREE" :attrs {:class "s-color"}}]
                                            [{:text (mf/as-money price)}])
   :cart-item-service-thumbnail/id        "service"
   :cart-item-service-thumbnail/image-url (->> service-sku
                                               (catalog-images/image (maps/index-by :catalog/image-id (:selector/images service-product)) "cart")
                                               :ucare/id)})

(defn ^:private hacky-cart-image
  [item]
  (some->> item
           :selector/images
           (filter #(= "cart" (:use-case %)))
           first
           :url
           ui/ucare-img-id))

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
  [items]
  (for [{:as                         free-service
         :catalog/keys               [sku-id]
         :item/keys                  [variant-name quantity unit-price]
         :item.service/keys          [addons]
         :join/keys                  [addon-facets]
         :promo.mayvenn-install/keys [hair-missing-quantity requirement-copy]
         :hacky/keys                 [promo-mayvenn-install-requirement-copy]
         :product/keys               [essential-title essential-price essential-inclusions]
         :copy/keys                  [whats-included]}

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
                                                  {:text "FREE" :attrs {:class "s-color"}}]
                                                 [{:text price}])
      :cart-item-service-thumbnail/id          "freeinstall"
      :cart-item-service-thumbnail/image-url   (hacky-cart-image free-service)}
     (cart-items|addons|SRV<- addons)
     (cart-items|addons<- addon-facets))))

(defn service-items<-
  [stylist items shared-cart-id pick-your-stylist-button-disabled?]
  (let [services (select ?service items)]
    (merge
     {:stylist (if-let [stylist-id (:stylist/id stylist)]
                 {:servicing-stylist-portrait-url                  (-> stylist :stylist/portrait :resizable-url)
                  :servicing-stylist-banner/id                     "servicing-stylist-banner"
                  :servicing-stylist-banner/title                  (:stylist/name stylist)
                  :servicing-stylist-banner/rating                 {:rating/value (:stylist.rating/score stylist)
                                                                    :rating/id    "stylist-rating-id"}
                  :servicing-stylist-banner/image-url              (some-> stylist :stylist/portrait :resizable-url)
                  :servicing-stylist-banner/title-and-image-target [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                                               :store-slug (:stylist/slug stylist)}]}
                 {:no-stylist-organism/id         "stylist-organism"
                  :no-stylist-organism/target     [events/control-shared-cart-pick-your-stylist-clicked
                                                   {:id shared-cart-id}]
                  :no-stylist-organism/disabled? pick-your-stylist-button-disabled?
                  :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})}
     (when (seq services)
       {:service-line-items (free-services<- items)}))))

(defn physical-items<-
  [items]
  (when items
    {:physical-items/id    "physical-items"
     :physical-items/title "Items"
     :physical-items/items (map-indexed
                            (fn [i {:keys [catalog/sku-id item/quantity legacy/product-name sku/title
                                           join/facets sku/price hair/length]
                                    :as item}]
                              {:cart-item/id                             (str i "-cart-item-" sku-id "-" quantity)
                               :cart-item/index                          i
                               :cart-item-title/id                       (str "line-item-title-" sku-id)
                               :cart-item-title/primary                  (or product-name title)
                               :cart-item-title/secondary                (some-> facets :hair/color :option/name)
                               :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                                           :value (str "qty. " quantity)}]
                               :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" sku-id)
                               :cart-item-floating-box/contents          (let [price (mf/as-money price)]
                                                                           [{:text price :attrs {:data-test (str "line-item-price-ea-" sku-id)}}
                                                                            {:text " each" :attrs {:class "proxima content-4"}}])
                               :cart-item-square-thumbnail/id            sku-id
                               :cart-item-square-thumbnail/sku-id        sku-id
                               :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (some-> length first)]
                                                                                 (str length-circle-value "”"))
                               :cart-item-square-thumbnail/ucare-id      (hacky-cart-image item)})
                            items)}))

(def shipping-method-summary-line-query
  {:cart-summary-line/id       "shipping"
   :cart-summary-line/label    "Shipping"
   :cart-summary-line/sublabel "4-6 days" ;; NOTE: if only services, no shipping time?
   :cart-summary-line/value    "FREE"})

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn line-item-promo-discount
  [promotion-code line-item-price]
  (case promotion-code
    "flash15"  (* 0.15 line-item-price)
    "welcome5" (* 0.05 line-item-price)
    0))

(defn order-promo-discount
  [promotion-code]
  (case promotion-code
    "heat"     15
    "cash5"    5
    0))

(defn cart-summary<-
  "The cart has an upsell 'entered' because the customer has requested a service discount"
  [order]
  (let [waiter-order              (:waiter/order order)
        order-adjustments         (:adjustments waiter-order)
        discountable-item         (->> order
                                       :order/items
                                       (filter #(and (contains? (:service/type %) "base")
                                                     (first (:promo.mayvenn-install/discountable %))))
                                       first)
        wig-customization?        (and (contains? (:hair/family discountable-item) "ready-wigs" )
                                       (contains? (:hair/family discountable-item) "lace-front-wigs")
                                       (contains? (:hair/family discountable-item) "360-wigs")
                                       (contains? (:service/category discountable-item) "customization"))
        discounted-service-amount (->> discountable-item
                                       :item/applied-promotions
                                       (filter #(= :freeinstall (:name (:promotion %))))
                                       first
                                       :amount)
        service-is-discounted?    (neg? discounted-service-amount)
        explicit-promotion        (->> order-adjustments (remove #(= :freeinstall (:coupon-code %))) first)
        total-savings             (->> order-adjustments (reduce (fn [acc adj] (+ acc (:price adj))) 0))]
    (cond->
        {:cart-summary/id               "shared-cart-summary"
         :cart-summary-total-line/id    "total"
         :cart-summary-total-line/label (if (and (not-empty discountable-item) (not wig-customization?))
                                          "Hair + Install Total"
                                          "Total")
         :cart-summary-total-line/value (-> waiter-order :total mf/as-money)
         :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                       :cart-summary-line/label "Subtotal"
                                       :cart-summary-line/value (mf/as-money-or-free (:line-items-total waiter-order))}]

                                     (when-let [shipping-method-summary-line
                                                shipping-method-summary-line-query]
                                       [shipping-method-summary-line])

                                     [(when service-is-discounted?
                                        {:cart-summary-line/id    "freeinstall-adjustment"
                                         :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                      :height "2em" :width "2em"}]
                                         :cart-summary-line/label (str "Free " (:sku/title discountable-item))
                                         :cart-summary-line/value (mf/as-money-or-free discounted-service-amount)})
                                      (when explicit-promotion
                                        {:cart-summary-line/id    (str (text->data-test-name (:coupon-code explicit-promotion)) "-adjustment")
                                         :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                      :height "2em" :width "2em"}]
                                         :cart-summary-line/label (string/upper-case (:coupon-code explicit-promotion))
                                         :cart-summary-line/value (mf/as-money-or-free (:price explicit-promotion))})])}

      service-is-discounted?
      (merge {:cart-summary-total-incentive/savings (when (neg? total-savings)
                                                      (mf/as-money (- total-savings)))})

      (and service-is-discounted?
           (not wig-customization?))
      (merge {:cart-summary-total-incentive/id    "mayvenn-install"
              :cart-summary-total-incentive/label "Includes Mayvenn Service"})

      (and service-is-discounted?
           wig-customization?)
      (merge {:cart-summary-total-incentive/id    "wig-customization"
              :cart-summary-total-incentive/label "Includes Wig Customization"}))))

(defn quadpay<-
  [data {:keys [waiter/order]}]
  {:quadpay/order-total (:total order)
   :quadpay/show?       (get-in data keypaths/loaded-quadpay)
   :quadpay/directive   :just-select})

(defn servicing-stylist-sms-info<-
  [items servicing-stylist]
  (when (select ?service items)
    (if servicing-stylist
      {:copy          (str "After your order ships, you'll be connected with "
                           (stylists/->display-name servicing-stylist)
                           " over SMS to make an appointment.")
       :servicing-stylist-portrait-url (-> servicing-stylist :portrait :resizable-url)}
      {:copy          "You'll be able to select your Mayvenn Certified Stylist after checkout."
       :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})))

(defn hero-component [{:hero/keys [title subtitle]}]
  [:div.center.my6
   [:div.canela.title-1.mb3 title]
   [:div.proxima.content-2.mx-auto
    {:style {:width "270px"}}
    subtitle]])

(component/defcomponent no-stylist-organism
  [{:no-stylist-organism/keys [id target disabled?]} _ _]
  (when id
    [:div.bg-white
     [:div.pt3.pb4.px3.canela.title-2.dark-gray.items-center.flex.flex-column
      [:div.mb1 "No Stylist Selected"]
      [:div (ui/button-small-primary
             (merge {:data-test "pick-a-stylist"
                     :disabled? disabled?}
                    (apply utils/fake-href target))
             "Pick Your Stylist")]]
     [:div.mb1.border-bottom.border-cool-gray.hide-on-mb]]))

(defn service-items-component
  [{:keys [service-line-items stylist services-section/title] :as data}]
  [:div.mb3
   [:div.title-2.proxima.mb1.shout title]
   (component/build cart-item-v202004/stylist-organism stylist nil)
   (component/build no-stylist-organism stylist nil)

   (if (seq service-line-items)
     (for [service-line-item service-line-items]
       [:div {:key (:react/key service-line-item)}
        [:div.mt2-on-mb
         (component/build cart-item-v202004/organism {:cart-item service-line-item}
                          (component/component-id (:react/key service-line-item)))]])

     (component/build cart-item-v202004/no-services-organism data nil))

   [:div.border-bottom.border-gray.hide-on-mb]])

(defn physical-items-component
  [{:physical-items/keys [items id]}]
  (when id
    [:div
     {:key id}
     [:div.title-2.proxima.mb1 "Items"]
     (for [{:cart-item/keys [id index] :as cart-item} items
           :when                                key]
       [:div
        {:key id}
        (when (not (zero? index))
          [:div.flex.bg-white
           [:div.ml2 {:style {:width "75px"}}]
           [:div.flex-grow-1.border-bottom.border-cool-gray.ml-auto.mr2]])
        (component/build cart-item-v202004/organism {:cart-item cart-item}
                         (component/component-id id))])]))

(defcomponent template
  [{:keys [cta
           paypal
           quadpay
           browser-pay?
           edit
           servicing-stylist-sms-info
           errors] :as data} _ _]
  [:main.bg-white.flex-auto
   [:div.col-7-on-dt.mx-auto
    [:div.container
     (component/build flash/component data)
     (hero-component data)
     [:div.bg-refresh-gray.p3.col-on-tb-dt.col-6-on-tb-dt.bg-white-on-tb-dt
      (service-items-component data)
      (physical-items-component data)]
     [:div.col-on-tb-dt.col-6-on-tb-dt.bg-refresh-gray.bg-white-on-mb.mbj1
      (component/build cart-summary/organism data nil)
      [:div.px4.center.bg-white-on-mb ; Checkout buttons
       #?@(:cljs
           [(component/build quadpay/component quadpay nil)])

       ;; Service Caption
       (when-let [{:keys [copy servicing-stylist-portrait-url]} servicing-stylist-sms-info]
         [:div.flex.h6.items-center.mtj1
          {:data-test "servicing-stylist-sms-info"}
          [:div.pr2
           (ui/circle-picture
            {:width 50}
            ;; Note: We are not using ucare-id because stylist portraits may have
            ;; ucarecdn crop parameters saved into the url
            (ui/square-image {:resizable-url servicing-stylist-portrait-url} 50))]
          [:div.left-align
           copy]])

       ;; CTAs for checking out
       [:div.py2
        ;; cta button
        (let [{:cta/keys [id content disabled? spinning? disabled-reason target]} cta]
          (when id
            [:div
             (ui/button-large-primary {:data-test id
                                       :disabled? disabled?
                                       :spinning? spinning?
                                       :on-click  (apply utils/send-event-callback target)}
                                      content)
             (when disabled-reason
               [:div.red.content-3.mt2
                {:data-test "checkout-disabled-reason"}
                disabled-reason])]))

        ;; paypal button
        (let [{:cta/keys [id spinning? disabled? target]} paypal]
          [:div
           [:div.h5.black.py1.flex.items-center
            [:div.flex-grow-1.border-bottom.border-gray]
            [:div.mx2 "or"]
            [:div.flex-grow-1.border-bottom.border-gray]]

           [:div
            (ui/button-large-paypal
             {:on-click  (apply utils/send-event-callback target)
              :disabled? disabled?
              :spinning? spinning?
              :data-test id}
             (component/html
              [:div
               "Check out with "
               [:span.medium.italic "PayPal™"]]))]])

        ;; browser pay
        #?(:cljs (when browser-pay?
                   (payment-request-button/built-component nil
                                                           {})))

        (let [{:cta/keys [target disabled? spinning? id]} edit]
          [:div
           [:div.h5.black.py1.flex.items-center
            [:div.flex-grow-1.border-bottom.border-gray]
            [:div.mx2 "or"]
            [:div.flex-grow-1.border-bottom.border-gray]]

           [:div
            (ui/button-small-underline-primary
             {:on-click  (apply utils/send-event-callback target)
              :disabled? disabled?
              :spinning? spinning?
              :data-test id}
             (component/html
              "edit cart"))]])
        ]]]]]])

(defn ^:private ->waiter-order
  [{:keys [line-items discounts promotion-codes servicing-stylist-id total-discounted-amount]}]
  (let [subtotal (reduce (fn [rolling-total {:keys [sku item/quantity]}]
                           (-> sku
                               :sku/price
                               (* quantity)
                               (+ rolling-total)))
                         0 line-items)]
    {:shipments [{:storefront/all-line-items (for [{:keys [sku item/quantity discounts]} line-items]
                                               {:applied-promotions (map (fn [{:keys [promotion discount-amount]}]
                                                                           {:amount (- discount-amount)
                                                                            :promotion {:name promotion}}) discounts)
                                                :quantity quantity
                                                :sku      (:catalog/sku-id sku)
                                                :source  (if (contains? (:catalog/department sku) "service")
                                                           "service"
                                                           "spree")
                                                :variant-attrs {:service/type (first (:service/type sku))}
                                                :variant-name (:sku/name sku)
                                                :unit-price  (:sku/price sku)
                                                :line-item-group 1}) ;; TODO ???
                  :servicing-stylist-id      servicing-stylist-id
                  :promotion-codes           promotion-codes}]
     :adjustments (map (fn [{:keys [promotion discount-amount]}]
                         {:coupon-code promotion
                          :name        (if (= "freeinstall" promotion) "Free Install" promotion)
                          :price       (- discount-amount)})
                       discounts)
     :line-items-total subtotal
     :total (- subtotal total-discounted-amount)}))

(defn ^:private enrich-line-items-with-sku-data
  [sku-db shared-cart]
  (let [indexed-sku-db (maps/index-by :legacy/variant-id (vals sku-db))]
    (update shared-cart :line-items
            #(for [item %]
               (assoc item :sku (or (->> item :catalog/sku-id (get sku-db))
                                         (->> item :legacy/variant-id (get indexed-sku-db))))))))

(defn ^:private meets-discount-criterion?
  [items [_word essentials rule-quantity]]
  (->> items
       (map #(merge (:sku %) %))
       (select essentials)
       (map :item/quantity)
       (apply +)
       (<= rule-quantity)))

(defn ^:private add-discounts-to-line-items
  [promotion-code items]
  (for [{:keys [sku item/quantity catalog/sku-id] :as item} items
        :let
        [freeinstall-rules-for-item (get api.orders/rules sku-id)
         line-item-base-price       (-> sku :sku/price (* quantity))]]
    (cond-> item
      (and freeinstall-rules-for-item ;; is a freeinstall discountable service line item
           (every? (partial meets-discount-criterion? items) freeinstall-rules-for-item))
      (update :discounts (fn [discounts] (conj discounts {:promotion       :freeinstall
                                                          :discount-amount line-item-base-price})))

      (and promotion-code
           (not freeinstall-rules-for-item)) ;; is not a freeinstall discountable service line item
      (update :discounts (fn [discounts] (conj discounts {:promotion       promotion-code
                                                          :discount-amount (line-item-promo-discount promotion-code line-item-base-price)}))))))

(defn ^:private add-discounts-roll-up
  [{:keys [line-items]
    :as shared-cart}]
  (->> line-items
       (mapcat :discounts)
       (concat (for [code (:promotion-codes shared-cart)]
                 {:promotion       code
                  :discount-amount (order-promo-discount code)}))
       (reduce (fn [acc {:keys [promotion discount-amount]}]
                 (update acc promotion (partial + discount-amount))) {})
       (map (fn [[k v]]
              {:promotion k
               :discount-amount v}))
       (assoc shared-cart :discounts)))

(defn ^:private add-total-discounted-amount
  [{:keys [discounts]
    :as shared-cart}]
  (->> discounts
       (reduce (fn [acc {:keys [discount-amount]}]
                 (+ acc discount-amount)) 0)
       (assoc shared-cart :total-discounted-amount)))

(defn ^:private apply-promos
  [shared-cart]
  (let [promotion-code (first (:promotion-codes shared-cart))]
    (-> shared-cart
        (update :line-items (partial add-discounts-to-line-items promotion-code))
        add-discounts-roll-up
        add-total-discounted-amount)))

(defn page [state _]
  (let [{:keys [servicing-stylist-id number]
         :as   shared-cart} (get-in state keypaths/shared-cart-current)
        sku-db              (get-in state keypaths/v2-skus)
        order               (->> shared-cart
                                 (enrich-line-items-with-sku-data sku-db)
                                 apply-promos
                                 ->waiter-order
                                 (api.orders/->order state))
        cart-creator        (or ;; If stylist fails to be fetched, then it falls back to current store
                             (get-in state keypaths/shared-cart-creator)
                             (get-in state keypaths/store))
        cart-creator-copy   (if (= "salesteam" (:store-slug cart-creator))
                              "Your Mayvenn Concierge"
                              (stylists/->display-name cart-creator))
        servicing-stylist   (api.stylist/by-id state servicing-stylist-id)
        order-items         (:order/items order)
        physical-items      (select ?physical order-items)
        pending-request?    (utils/requesting? state request-keys/create-order-from-shared-cart)
        advertised-price    (-> order :waiter/order :total)
        spinning-button     (get-in state keypaths/shared-cart-redirect)]
    (component/build template (merge {:hero/title    "Your Bag"
                                      :hero/subtitle (str cart-creator-copy " has created a bag for you!")}
                                     (service-items<- servicing-stylist order-items number pending-request?)
                                     (physical-items<- physical-items)
                                     (quadpay<- state order)
                                     (cart-summary<- order)
                                     {:servicing-stylist-sms-info (servicing-stylist-sms-info<- order-items (:diva/stylist servicing-stylist))
                                      ;; TODO there are multiple CTAs!
                                      ;; TODO consider spinning/disabled for alla these buttons
                                      :cta                        {:cta/id        "start-checkout-button"
                                                                   :cta/disabled? pending-request?
                                                                   :cta/target    [events/control-shared-cart-checkout-clicked
                                                                                   {:id               number
                                                                                    :advertised-price advertised-price
                                                                                    :validate-price?  true}]
                                                                   :cta/spinning? (= :checkout spinning-button)
                                                                   :cta/content   "Check out"}
                                      :paypal                     {:cta/id        "paypal-checkout"
                                                                   :cta/target    [events/control-shared-cart-paypal-checkout-clicked
                                                                                   {:id               number
                                                                                    :advertised-price advertised-price
                                                                                    :validate-price?  true}]
                                                                   :cta/spinning? (= :paypal spinning-button)
                                                                   :cta/disabled? pending-request?}
                                      :edit                       {:cta/id        "edit"
                                                                   :cta/target    [events/control-shared-cart-edit-cart-clicked
                                                                                   {:id number}]
                                                                   :cta/spinning? (= :cart spinning-button)
                                                                   :cta/disabled? pending-request?}}))))

(defn ^:export built-component
  [data opts]
  (if (experiments/new-shared-cart? data)
    (page data opts)
    (component/build component (query data) opts)))

(defmethod transitions/transition-state events/api-success-shared-cart-fetch
  [_ event {:as args :keys [shared-cart skus products shared-cart-creator]} app-state]
  (-> app-state
      (assoc-in keypaths/shared-cart-current shared-cart)
      (assoc-in keypaths/shared-cart-creator shared-cart-creator)
      (update-in keypaths/v2-skus merge (products/index-skus skus))
      (update-in keypaths/v2-products merge (products/index-products products))))

;; TODO: make this work server side
;; TODO: destructuring in the look detail page is throwing an exception (around gathering images?)
(defmethod transitions/transition-state events/api-success-shared-carts-fetch
  [_ event {:keys [carts skus images]} app-state]
  (-> app-state
      (update-in keypaths/v2-skus merge (catalog.products/index-skus (vals skus)))
      (update-in keypaths/v2-images merge (maps/map-keys (fnil name "") images))
      (update-in keypaths/v1-looks-shared-carts merge (maps/index-by :number carts))))

(defmethod effects/perform-effects events/api-success-shared-cart-fetch
  [_ _ {{:keys [promotion-codes servicing-stylist-id]} :shared-cart} _ app-state]
  #?(:cljs
     (let [shared-cart  (get-in app-state keypaths/shared-cart-current)
           catalog-skus (get-in app-state keypaths/v2-skus)
           api-cache    (get-in app-state keypaths/api-cache)]
       (if (->> shared-cart
                (enrich-line-items-with-sku-data catalog-skus)
                :line-items
                (not-every? (comp :inventory/in-stock? :sku)))
         (do (messages/handle-message events/flash-later-show-failure
                                      {:message "The bag that has been shared with you has items that are no longer available."})
             (history/enqueue-navigate events/navigate-home))
         (when (and servicing-stylist-id
                    (experiments/new-shared-cart? app-state))
           (api/fetch-matched-stylist
            api-cache servicing-stylist-id
            {:error-handler   #(publish events/shared-cart-error-matched-stylist-not-eligible %)
             :success-handler #(publish events/api-success-fetch-shared-cart-matched-stylist %)}))))))

(defmethod effects/perform-effects events/api-success-fetch-shared-cart-matched-stylist
  [_ _ {:keys [stylist]} _ _]
  (publish events/flow|shared-cart-stylist|resulted
           {:method  :by-ids
            :results [stylist]}))

(defmethod transitions/transition-state events/shared-cart-error-matched-stylist-not-eligible
  [_ _ _ state]
  (assoc-in state keypaths/errors
            {:error-message "The original servicing stylist is no longer available. Please pick a new stylist below"}))

(defmethod transitions/transition-state events/flow|shared-cart-stylist|resulted
  [_ _ {:keys [results]} state]
  (let [shared-cart-stylist-model (->> results
                                       (mapv #(api.stylist/stylist<- state %))
                                       (maps/index-by :stylist/id))]
    (update-in state storefront.keypaths/models-stylists merge shared-cart-stylist-model)))

(defmethod transitions/transition-state events/navigate-shared-cart
  [_ event {:keys [shared-cart-id]} app-state]
  (-> app-state
      (assoc-in keypaths/shared-cart-id shared-cart-id)
      (assoc-in keypaths/shared-cart-redirect nil)))

(defmethod effects/perform-effects events/navigate-shared-cart
  [_ _ {:keys [shared-cart-id]} _ app-state]
  #?(:cljs (api/fetch-shared-cart shared-cart-id)))


#?(:cljs
   (defmethod transitions/transition-state events/control-shared-cart-checkout-clicked
     [_ _ _ state]
     (assoc-in state keypaths/shared-cart-redirect :checkout)))

#?(:cljs
   (defmethod transitions/transition-state events/control-shared-cart-paypal-checkout-clicked
     [_ _ _ state]
     (assoc-in state keypaths/shared-cart-redirect :paypal)))

#?(:cljs
   (defmethod transitions/transition-state events/control-shared-cart-edit-cart-clicked
     [_ _ _ state]
     (assoc-in state keypaths/shared-cart-redirect :edit-cart)))

#?(:cljs
   (defmethod transitions/transition-state events/control-shared-cart-pick-your-stylist-clicked
     [_ _ _ state]
     (assoc-in state keypaths/shared-cart-redirect :pick-your-stylist)))


;; #?(:cljs
;;    (defmethod transitions/transition-state events/control-shared-cart-edit-clicked
;;      [_ _ _ state]
;;      (assoc-in state keypaths/shared-cart-redirect :edit)))

#?(:cljs
   (defn control-fx
     [state {:keys [id advertised-price]} on-success]
     (let [success-handler #(do
                              (messages/handle-message events/save-order
                                                       {:order (orders/TEMP-pretend-service-items-do-not-exist %)})
                              (messages/handle-message events/biz|shared-cart|hydrated
                                                       {:order            %
                                                        :advertised-price advertised-price
                                                        :on/success       on-success}))
           error-handler   #(messages/handle-message events/api-failure-shared-cart)]
       (api/create-order-from-cart {:session-id           (get-in state keypaths/session-id)
                                    :shared-cart-id       id
                                    :user-id              (get-in state keypaths/user-id)
                                    :user-token           (get-in state keypaths/user-token)
                                    :stylist-id           (get-in state keypaths/store-stylist-id)
                                    :servicing-stylist-id (get-in state keypaths/order-servicing-stylist-id)}
                                   success-handler
                                   error-handler))))

#?(:cljs
   (defmethod effects/perform-effects events/control-shared-cart-checkout-clicked
     [_ _ args _ state]
     (control-fx state args (partial history/enqueue-navigate
                                     events/navigate-checkout-returning-or-guest))))

#?(:cljs
   (defmethod effects/perform-effects events/control-shared-cart-paypal-checkout-clicked
     [_ _ args _ state]
     (control-fx state args (partial messages/handle-message
                                     events/checkout-initiated-paypal-checkout))))

#?(:cljs
   (defmethod effects/perform-effects events/control-shared-cart-edit-cart-clicked
     [_ _ args _ state]
     (control-fx state args (partial history/enqueue-navigate
                                     events/navigate-cart))))

#?(:cljs
   (defmethod effects/perform-effects events/control-shared-cart-pick-your-stylist-clicked
     [_ _ args _ state]
     (control-fx state args (partial history/enqueue-navigate
                                     events/navigate-adventure-find-your-stylist))))

#?(:cljs
   (defmethod transitions/transition-state events/clear-shared-cart-redirect
     [_ _ _ state]
     (update-in state keypaths/shared-cart dissoc :redirect)))

#?(:cljs
   (defmethod effects/perform-effects events/biz|shared-cart|hydrated
     [_ _ {:keys [order advertised-price validate-price?] on-success :on/success} _ state]
     (if (and validate-price?
              (not= (-> advertised-price mf/as-money)
                    (-> order :total mf/as-money)))
       (do
         (history/enqueue-navigate events/navigate-cart {:query-params {:error "discounts-changed"}})
         (messages/handle-later events/clear-shared-cart-redirect))
       (on-success))))

(defmethod transitions/transition-state events/api-failure-shared-cart
  [_ _ _ state]
  (-> state
      (assoc-in keypaths/errors {:error-message "Sorry, something went wrong. Please try again."})
      (update-in keypaths/shared-cart dissoc :redirect)))

;; TODO paypal checkout
;; unique react keys
