(ns storefront.components.shared-cart
  (:require #?(:cljs [storefront.api :as api])
            api.stylist
            [catalog.images :as catalog-images]
            [catalog.products :as products]
            [catalog.services :as catalog.services]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [spice.maps :as maps]
            [spice.selector :as selector]
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

(defn service-line-item-query
  [{:keys [catalog/sku-id] :as service-sku} service-product]
  {:react/key                             (str "service-line-item-" sku-id)
   :cart-item-title/id                    "line-item-title-upsell-service"
   :cart-item-title/primary               (or (:copy/title service-product) (:legacy/product-name service-sku))
   :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                            :value (:copy/whats-included service-sku)}
                                           {:id    (str "line-item-quantity-" sku-id)
                                            :value (str "qty. " (:item/quantity service-sku))}]
   :cart-item-floating-box/id             "line-item-service-price"
   :cart-item-floating-box/contents       [{:text (some-> service-sku :sku/price mf/as-money)}]
   :cart-item-service-thumbnail/id        "service"
   :cart-item-service-thumbnail/image-url (->> service-sku
                                               (catalog-images/image (maps/index-by :catalog/image-id (:selector/images service-product)) "cart")
                                               :ucare/id)})
(defn service-items<-
  [stylist services products]
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

    (cond-> {}
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
      (merge {:stylist {:stylist-organism/id "no-stylist"}})

      (seq services)
      (merge {:service-line-items (for [line-item (->> (concat free-services standalone-services)
                                                       shared-cart/sort-by-depart-and-price)
                                        :let      [service-product (some->> line-item
                                                                            :selector/from-products
                                                                            first
                                                                            ;; TODO: not resilient to skus belonging to multiple products
                                                                            (get products))]]
                                    (cond-> (service-line-item-query line-item service-product)
                                      (and (shared-cart/discountable? line-item)
                                           (seq addon-services))
                                      (merge {:cart-item-sub-items/id    "add-on-services"
                                              :cart-item-sub-items/title "Add-On Services"
                                              :cart-item-sub-items/items (map (fn [{:sku/keys [title price] sku-id :catalog/sku-id}]
                                                                                {:cart-item-sub-item/title  title
                                                                                 :cart-item-sub-item/price  (mf/as-money price)
                                                                                 :cart-item-sub-item/sku-id sku-id})
                                                                              addon-services)})))} )

      (and stylist (empty? services))
      (merge {:no-services/id         "select-your-service"
              :no-services/title      "No Service Selected"
              :no-services/cta-label  "Select Your Service"
              :no-services/cta-target [events/navigate-category {:catalog/category-id "31"
                                                                 :page/slug           "free-mayvenn-services"}]}))))

(defn hero-component [{:hero/keys [title subtitle]}]
  [:div.center.my6
   [:div.canela.title-1.mb3 title]
   [:div.proxima.content-2.mx-auto
    {:style {:width "270px"}}
    subtitle]])

(defn service-items-component
  [{:keys [service-line-items stylist] :as data}]
  [:div.mb3
   [:div.title-2.proxima.mb1.shout "Services"]
   (component/build cart-item-v202004/stylist-organism stylist nil)
   (component/build cart-item-v202004/no-stylist-organism stylist nil) ;NOTE: button will need to make cart AND go to find your stylist flow

   (if (seq service-line-items)
     (for [service-line-item service-line-items]
       [:div {:key (:react/key service-line-item)}
        [:div.mt2-on-mb
         (component/build cart-item-v202004/organism {:cart-item service-line-item}
                          (component/component-id (:react/key service-line-item)))]])

     (component/build cart-item-v202004/no-services-organism data nil))

   [:div.border-bottom.border-gray.hide-on-mb]])

(defcomponent template
  [data _ _]
  [:div
   (hero-component data)
   [:div.bg-refresh-gray.p3.col-on-tb-dt.col-6-on-tb-dt.bg-white-on-tb-dt
    (service-items-component data)]])

(defn page [state _]
  (let [{:keys [line-items] :as shared-cart} (get-in state keypaths/shared-cart-current)
        sku-db                               (get-in state keypaths/v2-skus)

        products            (get-in state keypaths/v2-products)
        enriched-line-items (shared-cart/enrich-line-items-with-sku-data sku-db line-items)
        ;; sks (get-in state keypaths/v2-skus)
        cart-creator        (or ;; If stylist fails to be fetched, then it falls back to current store
                             (get-in state keypaths/shared-cart-creator)
                             (get-in state keypaths/store))
        cart-creator-copy   (if (= "salesteam"(:store-slug cart-creator))
                              "Your Mayvenn Concierge"
                              (stylists/->display-name cart-creator))
        servicing-stylist   (api.stylist/by-id state (:servicing-stylist-id shared-cart))
        services            (selector/match-all {:selector/strict? true} catalog.services/service enriched-line-items)]
    (component/build template (merge {:hero/title    "Your Bag"
                                      :hero/subtitle (str cart-creator-copy " has created a bag for you!")}
                                     (service-items<- servicing-stylist services products)))))

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
