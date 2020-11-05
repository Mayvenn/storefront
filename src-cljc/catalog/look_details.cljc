(ns catalog.look-details
  "Shopping by Looks: Detail page for an individual 'look'"
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.hooks.quadpay :as quadpay]])
            [catalog.facets :as facets]
            [catalog.images :as catalog-images]
            [checkout.ui.cart-item-v202004 :as cart-item]
            [clojure.string :as str]
            [spice.core :as spice]
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as products]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.reviews :as reviews]
            [storefront.request-keys :as request-keys]
            [storefront.ugc :as ugc]))

(defn add-to-cart-button
  [sold-out? creating-order? look {:keys [number]}]
  (if sold-out?
    [:div.btn.col-12.h5.btn-primary.bg-gray.white
     {:on-click nil}
     "Sold Out"]
    (ui/button-large-primary
     (merge (utils/fake-href events/control-create-order-from-shared-cart
                             {:shared-cart-id number
                              :look-id        (:id look)})
            {:data-test "add-to-cart-submit"
             :disabled? (not look)
             :spinning? creating-order?})
     "Add items to cart")))

(defmethod effects/perform-effects events/control-create-order-from-shared-cart
  [_ event {:keys [look-id shared-cart-id] :as args} _ app-state]
  #?(:cljs
     (api/create-order-from-cart (get-in app-state keypaths/session-id)
                                 shared-cart-id
                                 look-id
                                 (get-in app-state keypaths/user-id)
                                 (get-in app-state keypaths/user-token)
                                 (get-in app-state keypaths/store-stylist-id)
                                 (get-in app-state keypaths/order-servicing-stylist-id)
                                 (and (= :shop (sites/determine-site app-state))
                                      (= events/navigate-shop-by-look-details (get-in app-state keypaths/navigation-event))))))

(defn carousel [data imgs]
  (component/build carousel/component
                   {:dependencies data
                    :settings     {:nav         true
                                   :edgePadding 0
                                   :controls    true
                                   :items       1}}
                   {:opts {:slides imgs}}))

(defn look-details-body
  [{:keys [creating-order? sold-out? look shared-cart skus fetching-shared-cart?
           base-price discounted-price quadpay-loaded? discount-text
           yotpo-data-attributes cart-items service-line-items] :as queried-data}]
  [:div.clearfix
   (when look
     [:div.bg-cool-gray.slides-middle.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
      (when shared-cart
        (carousel {:look look :shared-cart shared-cart} (:carousel/images queried-data)))
      [:div.px3.pb3.pt1
       [:div.flex.items-center
        [:div.flex-auto.content-1.proxima {:style {:word-break "break-all"}}
         (:title look)]
        [:div.ml1.line-height-1 {:style {:width  "21px"
                                         :height "21px"}}
         ^:inline (svg/instagram)]]
       (when yotpo-data-attributes
         (component/build reviews/reviews-summary-component {:yotpo-data-attributes yotpo-data-attributes} nil))
       (when-not (str/blank? (:description look))
         [:p.mt1.content-4.proxima.dark-gray (:description look)])]])
   (if fetching-shared-cart?
     [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
     (when shared-cart
       [:div.px2.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
        [:div.pt2.proxima.title-2.shout
         {:data-test (:items-title/id queried-data)}
         (:items-title/primary queried-data)]
        [:div.bg-white-on-tb-dt
         [:div
          (for [[index cart-item] (map-indexed vector cart-items)
                :let              [react-key (:react/key cart-item)]
                :when             react-key]
            [:div
             {:key (str index "-cart-item-" react-key)}
             (component/build cart-item/organism {:cart-item cart-item}
                              (component/component-id (str index "-cart-item-" react-key)))])]
         (when (seq service-line-items)
           [:div.mb3
            [:div
             (for [service-line-item service-line-items]
               [:div
                [:div.mt2-on-mb
                 (component/build cart-item/organism {:cart-item service-line-item}
                                  (component/component-id (:react/key service-line-item)))]])]])]
        [:div.border-top.border-cool-gray.mxn2.mt3]
        [:div.center.pt4
         (when discount-text
           [:div.center.flex.items-center.justify-center.title-2.bold
            (svg/discount-tag {:height "28px"
                               :width  "28px"})
            discount-text])
         (when-not (= discounted-price base-price)
           [:div.strike.content-3.proxima.mt2 (mf/as-money base-price)])
         [:div.title-1.proxima.bold (mf/as-money discounted-price)]]
        [:div.col-11.mx-auto
         (add-to-cart-button sold-out? creating-order? look shared-cart)]
        #?(:cljs
           (component/build quadpay/component
                            {:quadpay/show?       quadpay-loaded?
                             :quadpay/order-total discounted-price
                             :quadpay/directive   :just-select}
                            nil))
        (component/build reviews/reviews-component {:yotpo-data-attributes yotpo-data-attributes} nil)]))])

(defn ^:private sort-by-depart-and-price
  [items]
  (sort-by (fn [{:keys [catalog/department promo.mayvenn-install/discountable sku/price]}]
             [(first department) (not (first discountable)) price])
           items))

(defn ^:private enrich-line-items-with-sku-data
  [catalog-skus shared-cart-line-items]
  (let [indexed-catalog-skus (maps/index-by :legacy/variant-id (vals catalog-skus))]
    (map
     (fn [line-item]
       (merge line-item
              (get indexed-catalog-skus (:legacy/variant-id line-item))))
     shared-cart-line-items)))

(defn ^:private shared-cart->discount
  [{:keys [promotions base-price shared-cart-promo base-service]}]
  (if base-service
    (let [service-price (:sku/price base-service)]
      {:discount-text    "Hair + FREE Service"
       :discounted-price (- base-price service-price)})
    (let [promotion% (some->> promotions
                              (filter (comp #{shared-cart-promo} str/lower-case :code))
                              first
                              :description
                              (re-find #"\b(\d\d?\d?)%")
                              second)]
      {:discount-text    (some-> promotion% (str "%"))
       :discounted-price (or
                          (some->> promotion%
                                   spice/parse-int
                                   (* 0.01)
                                   (- 1.0)  ;; 100% - discount %
                                   (* base-price))
                          base-price)})))

(defn ^:private add-product-title-and-color-to-line-item [products facets line-item]
  (merge line-item {:product-title (->> line-item
                                        :catalog/sku-id
                                        (products/find-product-by-sku-id products)
                                        :copy/title)
                    :color-name    (-> line-item
                                       :hair/color
                                       first
                                       (facets/get-color facets)
                                       :option/name)}))

(defn ^:private service?
  [line-item]
  (-> line-item :catalog/department first #{"service"} boolean))

(defn ^:private base-service?
  [line-item]
  (-> line-item :service/type first #{"base"} boolean))

(defn ^:private discountable?
  [line-item]
  (-> line-item :promo.mayvenn-install/discountable first true?))

(defn ^:private get-model-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (selector/match-all {:selector/strict? true}
                                            {:image/of #{"model"}})
                        (sort-by :order)
                        first)]
    [:img.col-12.mb4
     {:src (str (:url image) "-/format/auto/")
      :alt title}]))

(defn ^:private get-cart-product-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (selector/match-all {:selector/strict? true}
                                            {:use-case #{"cart"}
                                             :image/of #{"product"}})
                        (sort-by :order)
                        first)]
    [:img.col-12.mb4
     {:src (str (:url image) "-/format/auto/")
      :alt title}]))

(defn ^:private imgs [images-catalog look skus]
  (let [sorted-line-items (sort-by-depart-and-price skus)]
    (list
     [:img.col-12 {:src (str (:image-url look)) :alt ""}]
     (get-model-image images-catalog (first sorted-line-items))
     (get-cart-product-image images-catalog (first sorted-line-items)))))

(defn service-line-item-query
  [{:keys [sku-id] :as service-sku} service-product]
  {:react/key                             "service-line-item"
   :cart-item-title/id                    "line-item-title-upsell-service"
   :cart-item-title/primary               (or (:copy/title service-product) (:legacy/product-name service-sku))
   :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                            :value (:copy/whats-included service-sku)}
                                           {:id    (str "line-item-quantity-" sku-id)
                                            :value (str "qty. " (:item/quantity service-sku))}]
   :cart-item-floating-box/id             "line-item-service-price"
   :cart-item-floating-box/value          (some-> service-sku :sku/price mf/as-money)
   :cart-item-service-thumbnail/id        "service"
   :cart-item-service-thumbnail/image-url (->> service-sku
                                               (catalog-images/image (:selector/images service-product) "cart")
                                               :ucare/id)})

(defn cart-items-query
  [line-items]
  (for [{sku-id :catalog/sku-id
         images :selector/images
         :as    line-item} line-items

        :let [price (or (:sku/price line-item)
                        (:unit-price line-item))]]
    {:react/key                                (str sku-id "-" (:quantity line-item))
     :cart-item-title/id                       (str "line-item-title-" sku-id)
     :cart-item-title/primary                  (or (:product-title line-item)
                                                   (:product-name line-item))
     :cart-item-title/secondary                (:color-name line-item)
     :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                 :value (str "qty. " (:item/quantity line-item))}]
     :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" sku-id)
     :cart-item-floating-box/value             ^:ignore-interpret-warning [:div {:data-test (str "line-item-price-ea-" sku-id)}
                                                                           (mf/as-money price)
                                                                           ^:ignore-interpret-warning
                                                                           [:div.proxima.content-4 " each"]]
     :cart-item-square-thumbnail/id            sku-id
     :cart-item-square-thumbnail/sku-id        sku-id
     :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (-> line-item :hair/length first)]
                                                 (str length-circle-value "”"))
     :cart-item-square-thumbnail/ucare-id      (->> line-item
                                                    (catalog-images/image images "cart")
                                                    :ucare/id)}))

(defn query [data]
  (let [skus               (get-in data keypaths/v2-skus)
        shared-cart        (get-in data keypaths/shared-cart-current)
        products           (get-in data keypaths/v2-products)
        facets             (get-in data keypaths/v2-facets)
        line-items         (some->> shared-cart
                                    :line-items
                                    (enrich-line-items-with-sku-data skus)
                                    (map (partial add-product-title-and-color-to-line-item products facets)))
        album-keyword      (get-in data keypaths/selected-album-keyword)
        look               (contentful/look->look-detail-social-card album-keyword
                                                                     (contentful/selected-look data))
        album-copy         (get ugc/album-copy album-keyword)
        base-price         (->> line-items
                                (map (fn [line-item]
                                       (* (:item/quantity line-item)
                                          (:sku/price line-item))))
                                (apply + 0))
        discount           (shared-cart->discount
                            {:promotions        (get-in data keypaths/promotions)
                             :shared-cart-promo (some-> shared-cart :promotion-codes first str/lower-case)
                             :base-price        base-price
                             :base-service      (->> line-items
                                                     (filter (comp #(contains? % "base") :service/type))
                                                     first)})
        back               (first (get-in data keypaths/navigation-undo-stack))
        back-event         (:default-back-event album-copy)
        item-count         (->> line-items (map :item/quantity) (reduce + 0))
        {:keys
         [free-services
          standalone-services
          addon-services]} (group-by #(cond
                                        ((every-pred service?
                                                     base-service?
                                                     discountable?) %)
                                        :free-services
                                        (and (service? %)
                                             (base-service? %)
                                             (not (discountable? %)))
                                        :standalone-services
                                        (and (service? %)
                                             (->> % :service/type first #{"addon"}))
                                        :addon-services
                                        :else :product-line-items)
                                     line-items)]
    (merge {:shared-cart           shared-cart
            :look                  look
            :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
            :skus                  skus
            :sold-out?             (not-every? :inventory/in-stock? line-items)
            :fetching-shared-cart? (or (not look) (utils/requesting? data request-keys/fetch-shared-cart))
            :base-price            base-price
            :discounted-price      (:discounted-price discount)
            :quadpay-loaded?       (get-in data keypaths/loaded-quadpay)
            :discount-text         (:discount-text discount)
            :cart-items            (->> line-items
                                        (remove service?)
                                        cart-items-query
                                        sort-by-depart-and-price)
            :service-line-items    (for [line-item (->> (concat free-services standalone-services)
                                                        sort-by-depart-and-price)
                                         :let      [service-product (some->> line-item
                                                                             :selector/from-products
                                                                             first
                                                                             ;; TODO: not resilient to skus belonging to multiple products
                                                                             (get products))]]
                                     (cond-> (service-line-item-query line-item service-product)
                                       (and (discountable? line-item)
                                            (seq addon-services))
                                       (merge {:cart-item-sub-items/id    "add-on-services"
                                               :cart-item-sub-items/title "Add-On Services"
                                               :cart-item-sub-items/items (map (fn [{:sku/keys [title price] sku-id :catalog/sku-id}]
                                                                                 {:cart-item-sub-item/title  title
                                                                                  :cart-item-sub-item/price  (mf/as-money price)
                                                                                  :cart-item-sub-item/sku-id sku-id})
                                                                               addon-services)})))
            :carousel/images       (imgs (get-in data keypaths/v2-images) look line-items)
            :items-title/id        "item-quantity-in-look"
            :items-title/primary   (str item-count " items in this " (:short-name album-copy))

            :return-link/event-message (if (and (not back) back-event)
                                         [back-event]
                                         [events/navigate-shop-by-look {:album-keyword album-keyword}])
            :return-link/back          back}
           #?(:cljs (reviews/query-look-detail shared-cart data)))))

(defcomponent component
  [queried-data owner opts]
  [:div.container.mb4
   (look-details-body queried-data)])

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
