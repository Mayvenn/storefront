(ns storefront.components.shared-cart
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]
                       [storefront.hooks.quadpay :as quadpay]
                       [storefront.accessors.orders :as orders]
                       [storefront.accessors.auth :as auth]
                       storefront.trackings
                       [storefront.frontend-trackings :as trackings]
                       [storefront.components.payment-request-button :as payment-request-button]])
            [api.catalog :refer [select ?discountable ?physical ?service]]
            api.orders
            api.stylist
            [catalog.products :as products]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [checkout.ui.cart-summary-v202004 :as cart-summary]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.flash :as flash]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            catalog.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            [storefront.transitions :as transitions]
            [api.orders :as api.orders]
            [storefront.accessors.experiments :as experiments]))

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
       {:service-line-items (free-services<- items)
        :services-section/id "services-section"
        :services-section/title "Services"}))))

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
                               :cart-item-title/secondary                (ui/sku-card-secondary-text item)
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
         :cart-summary-total-line/label (if (not-empty discountable-item)
                                          "Hair + Install Total"
                                          "Total")
         :cart-summary-total-line/value (some-> waiter-order :total mf/as-money)
         :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                       :cart-summary-line/label "Subtotal"
                                       :cart-summary-line/value (some-> waiter-order :line-items-total mf/as-money-or-free)}]

                                     (when-let [shipping-method-summary-line
                                                shipping-method-summary-line-query]
                                       [shipping-method-summary-line])

                                     [(when service-is-discounted?
                                        {:cart-summary-line/id    "freeinstall-adjustment"
                                         :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                      :height "2em" :width "2em"}]
                                         :cart-summary-line/label (str "Free " (or (:product/essential-title discountable-item)
                                                                                   (:sku/title discountable-item)))
                                         :cart-summary-line/value (some-> discounted-service-amount mf/as-money-or-free)})
                                      (when explicit-promotion
                                        {:cart-summary-line/id    (str (text->data-test-name (:coupon-code explicit-promotion)) "-adjustment")
                                         :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                      :height "2em" :width "2em"}]
                                         :cart-summary-line/label (string/upper-case (:coupon-code explicit-promotion))
                                         :cart-summary-line/value (some-> explicit-promotion :price mf/as-money-or-free)})])}

      service-is-discounted?
      (merge {:cart-summary-total-incentive/id    "mayvenn-install"
              :cart-summary-total-incentive/label "Includes Mayvenn Install"
              :cart-summary-total-incentive/savings (when (neg? total-savings)
                                                      (mf/as-money (- total-savings)))}))))

(defn quadpay<-
  [data {:keys [waiter/order]}]
  {:quadpay/order-total (:total order)
   :quadpay/show?       (get-in data keypaths/loaded-quadpay)
   :quadpay/directive   :just-select})

(defn servicing-stylist-sms-info<-
  [items servicing-stylist]
  (when (select ?service items)
    (if servicing-stylist
      {:copy          (str "You've selected "
                           (stylists/->display-name servicing-stylist)
                           " as your stylist. A Concierge Specialist will reach out to you within 3 business days to coordinate your appointment.")
       :servicing-stylist-portrait-url (-> servicing-stylist :portrait :resizable-url)}
      {:copy          "You'll be able to select your Mayvenn Certified Stylist after checkout."
       :servicing-stylist-portrait-url "//ucarecdn.com/bc776b8a-595d-46ef-820e-04915478ffe8/"})))

(defn hero-component [{:hero/keys [title subtitle]}]
  [:div.center.my6
   [:div.canela.title-1.mb3 title]
   [:div.proxima.content-2.mx-auto
    {:data-test "cart-creator-nickname"
     :style {:width "270px"}}
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
  [{:services-section/keys [id title] :keys [service-line-items stylist] :as data}]
  (when id
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

     [:div.border-bottom.border-gray.hide-on-mb]]))

(defn physical-items-component
  [{:physical-items/keys [items id]}]
  (when id
    [:div
     {:key id}
     [:div.shout.title-2.proxima.mb1 "Items"]
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
              "edit cart"))]])]]]]]])

(defn page [state _]
  (let [{:keys [servicing-stylist-id number]
         :as   shared-cart} (get-in state keypaths/shared-cart-current)
        sku-db              (get-in state keypaths/v2-skus)
        order               (api.orders/shared-cart->order state sku-db shared-cart)
        cart-creator        (or ;; If stylist fails to be fetched, then it falls back to current store
                             (get-in state keypaths/shared-cart-creator)
                             (get-in state keypaths/store))
        cart-creator-copy   (->> [(when (not= "salesteam" (:store-slug cart-creator))
                                    (stylists/->display-name cart-creator))
                                  "Your Mayvenn Concierge"]
                                 (remove string/blank?)
                                 first)
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
                                      :cta                        {:cta/id        "start-checkout-button"
                                                                   :cta/disabled? pending-request?
                                                                   :cta/target    [events/control-shared-cart-checkout-clicked
                                                                                   {:id               number
                                                                                    :advertised-price advertised-price
                                                                                    :upsell-addons?   (api.orders/requires-addons-followup? order)
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
                                      :edit                       {:cta/id        "shared-cart-edit"
                                                                   :cta/target    [events/control-shared-cart-edit-cart-clicked
                                                                                   {:id number}]
                                                                   :cta/spinning? (= :cart spinning-button)
                                                                   :cta/disabled? pending-request?}}))))

(defn ^:export built-component
  [data opts]
  (page data opts))

(defmethod transitions/transition-state events/api-success-shared-cart-fetch
  [_ event {:as args :keys [shared-cart skus products shared-cart-creator]} app-state]
  (-> app-state
      (assoc-in keypaths/shared-cart-current shared-cart)
      (assoc-in keypaths/shared-cart-creator shared-cart-creator)
      (update-in keypaths/v2-skus merge (products/index-skus skus))
      (update-in keypaths/v2-products merge (products/index-products products))))

(defmethod transitions/transition-state events/api-success-shared-carts-fetch
  [_ event {:keys [carts skus images]} app-state]
  (-> app-state
      (update-in keypaths/v2-skus merge (catalog.products/index-skus (vals skus)))
      (update-in keypaths/v2-images merge (maps/map-keys (fnil name "") images))
      (update-in keypaths/v1-looks-shared-carts merge (maps/index-by :number carts))))

(defn ^:private invalid-line-item?
  [stylist? {:keys [sku]}]
  (or (not (-> sku :inventory/in-stock?))
      (and (-> sku :catalog/department (= #{"stylist-exclusives"}))
           (not stylist?))))

(defmethod effects/perform-effects events/api-success-shared-cart-fetch
  [_ _ {{:keys [promotion-codes servicing-stylist-id]} :shared-cart :as args} _ app-state]
  #?(:cljs
     (let [shared-cart  (get-in app-state keypaths/shared-cart-current)
           catalog-skus (get-in app-state keypaths/v2-skus)
           api-cache    (get-in app-state keypaths/api-cache)
           stylist?     (auth/stylist? (auth/signed-in app-state))]
       (if (->> shared-cart
                (api.orders/enrich-line-items-with-sku-data catalog-skus)
                :line-items
                (some (partial invalid-line-item? stylist?)))
         (do (messages/handle-message events/flash-later-show-failure
                                      {:message "The bag that has been shared with you has items that are no longer available."})
             (history/enqueue-navigate events/navigate-home))
         (do (when servicing-stylist-id
               (api/fetch-matched-stylist api-cache servicing-stylist-id
                                          {:error-handler   #(publish events/shared-cart-error-matched-stylist-not-eligible %)
                                           :success-handler #(publish events/api-success-fetch-shared-cart-matched-stylist %)}))
             (when (and
                    (= events/navigate-shop-by-look-details (get-in app-state keypaths/navigation-event))
                    (experiments/look-customization? app-state))
               (messages/handle-message events/initialize-look-details
                                        (assoc args :shared-cart shared-cart))))))))

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

#?(:cljs
   (defn control-fx
     [state {:keys [id advertised-price]} on-success]
     (let [success-handler #(let [new-order (orders/TEMP-pretend-service-items-do-not-exist %)]
                              (messages/handle-message events/save-order
                                                       {:order new-order})
                              (messages/handle-message events/biz|shared-cart|hydrated
                                                       {:order            new-order
                                                        :advertised-price advertised-price
                                                        :shared-cart-id   id
                                                        :on/success       on-success}))
           error-handler   #(messages/handle-message events/api-failure-shared-cart)]
       (api/create-order-from-shared-cart {:session-id           (get-in state keypaths/session-id)
                                           :shared-cart-id       id
                                           :user-id              (get-in state keypaths/user-id)
                                           :user-token           (get-in state keypaths/user-token)
                                           :stylist-id           (get-in state keypaths/store-stylist-id)
                                           :servicing-stylist-id (get-in state keypaths/order-servicing-stylist-id)}
                                   success-handler
                                   error-handler))))

#?(:cljs
   (defmethod effects/perform-effects events/control-shared-cart-checkout-clicked
     [_ _ {:keys [upsell-addons?] :as args} _ state]
     (control-fx state args (partial history/enqueue-navigate
                                     (if upsell-addons?
                                       events/navigate-checkout-add
                                       events/navigate-checkout-returning-or-guest)))))

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

#?(:cljs
   (defmethod storefront.trackings/perform-track events/biz|shared-cart|hydrated
     [_ _ {:keys [order shared-cart-id]} state]
     (trackings/track-bulk-add-to-cart
      {:skus-db          (get-in state keypaths/v2-skus)
       :images-catalog   (get-in state keypaths/v2-images)
       :store-experience (get-in state keypaths/store-experience)
       :order            order
       :shared-cart-id   shared-cart-id})))

(defmethod transitions/transition-state events/api-failure-shared-cart
  [_ _ _ state]
  (-> state
      (assoc-in keypaths/errors {:error-message "Sorry, something went wrong. Please try again."})
      (update-in keypaths/shared-cart dissoc :redirect)))
