(ns storefront.components.shared-cart
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]])
            api.orders
            api.stylist
            [catalog.images :as catalog-images]
            [catalog.products :as products]
            [catalog.services :as catalog.services]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [checkout.ui.cart-summary-v202004 :as cart-summary]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.shared-cart :as shared-cart]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.platform.messages :as messages]
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
  [{:keys [catalog/sku-id discounted-amount sku/price] :as service-sku} service-product]
  {:react/key                             (str "service-line-item-" sku-id)
   :cart-item-title/id                    "line-item-title-upsell-service"
   :cart-item-title/primary               (or (:copy/title service-product) (:legacy/product-name service-sku))
   :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                            :value (:copy/whats-included service-sku)}
                                           {:id    (str "line-item-quantity-" sku-id)
                                            :value (str "qty. " (:item/quantity service-sku))}]
   :cart-item-floating-box/id             "line-item-service-price"
   :cart-item-floating-box/contents       (if discounted-amount
                                            [{:text (mf/as-money price) :attrs {:class "strike"}}
                                             {:text "FREE" :attrs {:class "s-color"}}]
                                            [{:text (mf/as-money price)}])
   :cart-item-service-thumbnail/id        "service"
   :cart-item-service-thumbnail/image-url (->> service-sku
                                               (catalog-images/image (maps/index-by :catalog/image-id (:selector/images service-product)) "cart")
                                               :ucare/id)})

(defn service-items<-
  [stylist services products shared-cart-number]
  (let [{:keys
         [free-services
          standalone-services
          addon-services]} (group-by #(cond
                                        ((every-pred shared-cart/base-service?
                                                     shared-cart/discountable?) %)
                                        :free-services
                                        (and (shared-cart/base-service? %)
                                             (not (shared-cart/discountable? %)))
                                        :standalone-services
                                        (->> % :service/type first #{"addon"})
                                        :addon-services
                                        :else :product-line-items)
                                     services)]

    (when (seq services)
      (cond-> {:services-section/title "Services"
               :service-line-items     (for [line-item (->> (concat free-services standalone-services)
                                                            shared-cart/sort-by-depart-and-price)
                                             :let      [service-product (some->> line-item
                                                                                 :selector/from-products
                                                                                 first
                                                                                 ;; TODO: not resilient to skus belonging to multiple products
                                                                                 (get products))]]
                                     (cond-> (service-line-item-query line-item service-product) ;; need freeinstall-ness
                                       (and (shared-cart/discountable? line-item)
                                            (seq addon-services))
                                       (merge {:cart-item-sub-items/id    "add-on-services"
                                               :cart-item-sub-items/title "Add-On Services"
                                               :cart-item-sub-items/items (map (fn [{:sku/keys [title price] sku-id :catalog/sku-id}]
                                                                                 {:cart-item-sub-item/title  title
                                                                                  :cart-item-sub-item/price  (mf/as-money price)
                                                                                  :cart-item-sub-item/sku-id sku-id})
                                                                               addon-services)})))}
        stylist
        (merge {:stylist {:servicing-stylist-portrait-url                  (-> stylist :stylist/portrait :resizable-url)
                          :servicing-stylist-banner/id                     "servicing-stylist-banner"
                          :servicing-stylist-banner/title                  (:stylist/name stylist)
                          :servicing-stylist-banner/rating                 {:rating/value (:stylist.rating/score stylist)
                                                                            :rating/id    "stylist-rating-id"}
                          :servicing-stylist-banner/image-url              (some-> stylist :stylist/portrait :resizable-url)
                          :servicing-stylist-banner/title-and-image-target [events/navigate-adventure-stylist-profile {:stylist-id (:stylist/id stylist)
                                                                                                                       :store-slug (:stylist/slug stylist)}]}})

        (nil? stylist)
        (merge
         {:stylist {:no-stylist-organism/id     "no-stylist"
                    :no-stylist-organism/target [events/biz|shared-cart|hydrated
                                                 {:shared-cart/id shared-cart-number
                                                  :target/success [events/navigate-adventure-find-your-stylist]}]}})))))

(defn ^:private hacky-cart-image
  [item]
  (some->> item
           :selector/images
           (filter #(= "cart" (:use-case %)))
           first
           :url
           ui/ucare-img-id))

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

(def promo-table
  {"heat"     (constantly 15)
   "flash15"  #(* % 0.15)
   "cash5"    (constantly 5)
   "welcome5" #(* % 0.05)})

(defn cart-summary<-
  "The cart has an upsell 'entered' because the customer has requested a service discount"
  [line-items promotion-codes]
  (let [subtotal                  (reduce (fn [rolling-total line-item]
                                            (-> line-item
                                                :sku/price
                                                (* (:item/quantity line-item))
                                                (+ rolling-total)))
                                          0 line-items)
        discountable-proto-li     (first (filter :discounted-amount line-items))
        wig-customization?        (and (contains? (:hair/family discountable-proto-li) "ready-wigs" )
                                       (contains? (:hair/family discountable-proto-li) "lace-front-wigs")
                                       (contains? (:hair/family discountable-proto-li) "360-wigs")
                                       (contains? (:service/category discountable-proto-li) "customization"))
        discounted-service-amount (or (:discounted-amount discountable-proto-li) 0)
        promotion-code            (first promotion-codes)
        promo-discount-amount     ((get promo-table promotion-code (constantly 0)) (- subtotal (or (:sku/price discountable-proto-li) 0)))
        total-savings             (+ promo-discount-amount discounted-service-amount)
        total                     (some-> (- subtotal (+ promo-discount-amount discounted-service-amount)) mf/as-money)]
    (cond->
        {:cart-summary/id               "shared-cart-summary"
         :cart-summary-total-line/id    "total"
         :cart-summary-total-line/label (if (and (not-empty discountable-proto-li) (not wig-customization?))
                                          "Hair + Install Total"
                                          "Total")
         :cart-summary-total-line/value total
         :cart-summary/lines (concat [{:cart-summary-line/id    "subtotal"
                                       :cart-summary-line/label "Subtotal"
                                       :cart-summary-line/value (mf/as-money subtotal)}]

                                     (when-let [shipping-method-summary-line
                                                shipping-method-summary-line-query]
                                       [shipping-method-summary-line])

                                     [(when-not (zero? discounted-service-amount)
                                        {:cart-summary-line/id    "freeinstall-adjustment"
                                         :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                      :height "2em" :width "2em"}]
                                         :cart-summary-line/label (str "Free " (:sku/title discountable-proto-li))
                                         :cart-summary-line/value (mf/as-money-or-free (- discounted-service-amount))})
                                      (when promotion-code
                                        {:cart-summary-line/id    (str (text->data-test-name promotion-code) "-adjustment")
                                         :cart-summary-line/icon  [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                                                      :height "2em" :width "2em"}]
                                         :cart-summary-line/label promotion-code
                                         :cart-summary-line/value (mf/as-money-or-free (- promo-discount-amount))})])}

      (pos? discounted-service-amount)
      (merge {  :cart-summary-total-incentive/savings (when (pos? total-savings)
                                                        (mf/as-money total-savings))})

      (and (pos? discounted-service-amount) (not wig-customization?))
      (merge {:cart-summary-total-incentive/id    "mayvenn-install"
              :cart-summary-total-incentive/label "Includes Mayvenn Service"})

      (and (pos? discounted-service-amount) wig-customization?)
      (merge {:cart-summary-total-incentive/id    "wig-customization"
              :cart-summary-total-incentive/label "Includes Wig Customization"}))))

(defn hero-component [{:hero/keys [title subtitle]}]
  [:div.center.my6
   [:div.canela.title-1.mb3 title]
   [:div.proxima.content-2.mx-auto
    {:style {:width "270px"}}
    subtitle]])

(component/defcomponent no-stylist-organism
  [{:no-stylist-organism/keys [id target]} _ _]
  (when id
    [:div.bg-white
     [:div.pt3.pb4.px3.canela.title-2.dark-gray.items-center.flex.flex-column
      [:div.mb1 "No Stylist Selected"]
      [:div (ui/button-small-primary
             (merge {:data-test "pick-a-stylist"}
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
  [data _ _]
  [:main.bg-white.flex-auto
   [:div.col-7-on-dt.mx-auto
    [:div
     (hero-component data)
     [:div.bg-refresh-gray.p3.col-on-tb-dt.col-6-on-tb-dt.bg-white-on-tb-dt
      (service-items-component data)
      (physical-items-component data)]
     [:div.col-on-tb-dt.col-6-on-tb-dt.bg-refresh-gray.bg-white-on-mb.mbj1
      (component/build cart-summary/organism data nil)]]]])

(defn shared-cart->waiter-order
  [{:keys [line-items promotion-codes servicing-stylist-id number]}]
  {:shipments [{:storefront/all-line-items (for [line-item line-items]
                                             {:item/quantity (:item/quantity line-item)
                                              :item/sku      (:catalog/sku-id line-item)})
                :servicing-stylist-id      servicing-stylist-id
                :promotion-codes           promotion-codes}]})

(defn ^:private ensure-shared-cart-has-skus [sku-db shared-cart]
  (if (-> shared-cart :line-items first :catalog/sku-id)
    shared-cart
    (let [indexed-sku-db (maps/index-by :legacy/variant-id (vals sku-db))]
      (update shared-cart :line-items
              #(for [item %]
                 (->> item
                      :legacy/variant-id
                      (get indexed-sku-db)
                      :catalog/sku-id
                      (assoc item :catalog/sku-id)))))))

(defn page [state _]
  (let [{:keys [line-items
                promotion-codes
                servicing-stylist-id
                number]
         :as   shared-cart}       (get-in state keypaths/shared-cart-current)
        sku-db                    (get-in state keypaths/v2-skus)
        products                  (get-in state keypaths/v2-products)
        order                     (->> shared-cart
                                       (ensure-shared-cart-has-skus sku-db)
                                       shared-cart->waiter-order
                                       (api.orders/->order state))
        enriched-line-items       (shared-cart/enrich-line-items-with-sku-data sku-db line-items)
        promo-enriched-line-items (shared-cart/apply-promos enriched-line-items) ; maybe rename this? too bold
        cart-creator              (or ;; If stylist fails to be fetched, then it falls back to current store
                                   (get-in state keypaths/shared-cart-creator)
                                   (get-in state keypaths/store))
        cart-creator-copy         (if (= "salesteam"(:store-slug cart-creator))
                                    "Your Mayvenn Concierge"
                                    (stylists/->display-name cart-creator))
        servicing-stylist         (api.stylist/by-id state servicing-stylist-id)
        services                  (selector/match-all {:selector/strict? true} catalog.services/service promo-enriched-line-items)
        physical-items            (selector/match-all {:selector/strict? true} catalog.services/physical (:order/items order))]
    (component/build template (merge {:hero/title    "Your Bag"
                                      :hero/subtitle (str cart-creator-copy " has created a bag for you!")}
                                     (service-items<- servicing-stylist services products number)
                                     (physical-items<- physical-items)
                                     (cart-summary<- promo-enriched-line-items promotion-codes)))))

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
     (let [api-cache (get-in app-state keypaths/api-cache)]
       (api/get-promotions api-cache
                           (some-> promotion-codes
                                   first))
       (when (and servicing-stylist-id
                  (experiments/new-shared-cart? app-state))
         (api/fetch-matched-stylists
          api-cache [servicing-stylist-id]
          ;; TODO: may not be the appropriate success handler event because we
          ;; pulled this from stylist matching (this is more of a fetch
          ;; potential stylist for some new order the in future)
          #(messages/handle-message events/api-success-fetch-matched-stylists %))))))

(defmethod transitions/transition-state events/navigate-shared-cart
  [_ event {:keys [shared-cart-id]} app-state]
  (assoc-in app-state keypaths/shared-cart-id shared-cart-id))

(defmethod effects/perform-effects events/navigate-shared-cart
  [_ _ {:keys [shared-cart-id]} _ app-state]
  #?(:cljs (api/fetch-shared-cart shared-cart-id)))

(defn ^:private create-order-from-cart-params [app-state shared-cart-number]
  {:session-id           (get-in app-state keypaths/session-id)
   :shared-cart-id       shared-cart-number
   :user-id              (get-in app-state keypaths/user-id)
   :user-token           (get-in app-state keypaths/user-token)
   :stylist-id           (get-in app-state keypaths/store-stylist-id)
   :servicing-stylist-id (get-in app-state keypaths/order-servicing-stylist-id)})

(defmethod effects/perform-effects events/biz|shared-cart|hydrated
  [_ _ {:shared-cart/keys [id] :target/keys [success]} _ state]
  (-> (create-order-from-cart-params state id)
      #?(:cljs
         (api/create-order-from-cart
          #(messages/handle-message
            events/api-success-update-order-from-shared-cart
            (cond-> {:order
                     (orders/TEMP-pretend-service-items-do-not-exist %)}
              success
              (assoc :navigate (first success))))
          #(messages/handle-message
            events/api-failure-order-not-created-from-shared-cart)))))
