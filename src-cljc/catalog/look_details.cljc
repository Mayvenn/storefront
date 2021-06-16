(ns catalog.look-details
  "Shopping by Looks: Detail page for an individual 'look'"
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.hooks.quadpay :as quadpay]
                       ;; popups, must be required to load properly
                       looks.customization-modal])
            [api.catalog :refer [select  ?discountable ?physical ?service ?model-image ?cart-product-image]]
            api.orders
            [catalog.facets :as facets]
            [catalog.images :as catalog-images]
            [catalog.look-details-v202105 :as look-details-v202105]
            [checkout.ui.cart-item-v202004 :as cart-item]
            [clojure.string :as str]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as products]
            [storefront.accessors.shared-cart :as shared-cart]
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
            [storefront.ugc :as ugc]
            [api.orders :as api.orders]))

(defn add-to-cart-button
  [sold-out? creating-order? look {:keys [number]}]
  (ui/button-large-primary
   (merge (utils/fake-href events/control-create-order-from-look
                           {:shared-cart-id number
                            :look-id        (:id look)})
          {:data-test        "add-to-cart-submit"
           :disabled?        (or (not look) sold-out?)
           :disabled-content (when sold-out? "Sold Out")
           :spinning?        creating-order?})
   "Add items to bag"))

(defmethod effects/perform-effects events/control-create-order-from-look
  [_ event {:keys [look-id shared-cart-id] :as args} _ app-state]
  #?(:cljs
     (api/create-order-from-look (get-in app-state keypaths/session-id)
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
                   {:dependencies data}
                   {:opts {:settings {:nav         true
                                      :edgePadding 0
                                      :controls    true
                                      :items       1}
                           :slides   imgs}}))

(component/defcomponent customize-the-look-cta
  [{:look-customization.button/keys [target title id]} _ _]
  (when id
    [:div.my6.mt0-on-tb-dt.flex
     [:div.hide-on-mb {:style {:height "0px" :width "80px"}}]
     (ui/button-medium-underline-primary
      (assoc (apply utils/fake-href target)
             :data-test id
             :class "mx-auto-on-mb")
      title)]))

(component/defcomponent look-card
  [{:keys [shared-cart look yotpo-data-attributes] :as queried-data} _ _]
  [:div.bg-cool-gray.slides-middle.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
   [:div {:style {:min-height "390px"}}
    (when shared-cart
      (carousel {:look look :shared-cart shared-cart} (:carousel/images queried-data)))]
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

(defn look-details-body
  [{:keys [creating-order? sold-out? look shared-cart fetching-shared-cart?
           base-price discounted-price quadpay-loaded? discount-text
           yotpo-data-attributes cart-items service-line-items look-customization] :as queried-data}]
  [:div.clearfix
   (when look
     (component/build look-card queried-data "look-card"))
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

        (component/build customize-the-look-cta look-customization)

        [:div.border-top.border-cool-gray.mxn2.mt3]
        [:div.center.pt4
         (when discount-text
           [:div.center.flex.items-center.justify-center.title-2.bold
            (svg/discount-tag {:height "28px"
                               :width  "28px"})
            discount-text])
         (when-not (= discounted-price base-price)
           [:div.strike.content-3.proxima.mt2 (mf/as-money base-price)])
         [:div.title-1.proxima.bold.my1 (mf/as-money discounted-price)]]
        [:div.col-11.mx-auto
         (add-to-cart-button sold-out? creating-order? look shared-cart)]
        #?(:cljs
           (component/build quadpay/component
                            {:quadpay/show?       quadpay-loaded?
                             :quadpay/order-total discounted-price
                             :quadpay/directive   :just-select}
                            nil))
        (component/build reviews/reviews-component {:yotpo-data-attributes yotpo-data-attributes} nil)]))])

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

(defn ^:private get-model-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (select ?model-image)
                        (sort-by :order)
                        first)]
    (ui/img {:class    "col-12 mb4"
             :alt      title
             :max-size 749
             :src      (:url image)})))

(defn ^:private get-cart-product-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (select ?cart-product-image)
                        (sort-by :order)
                        first)]
    (ui/img {:class    "col-12 mb4"
             :alt      title
             :max-size 749
             :src      (:url image)})))

(defn ^:private imgs [images-catalog look skus]
  (let [sorted-line-items (shared-cart/sort-by-depart-and-price skus)]
    (list
     (ui/img {:class    "col-12"
              :max-size 749
              :src      (:image-url look)})
     (get-model-image images-catalog (first sorted-line-items))
     (get-cart-product-image images-catalog (first sorted-line-items)))))

(defn ^:private hacky-cart-image
  [item]
  (some->> item
           :selector/images
           (filter #(= "cart" (:use-case %)))
           first
           :url
           ui/ucare-img-id))

(defn service-line-item-query
  [{:keys [catalog/sku-id join/addon-facets] :as service-line-item}]
  (merge
   {:react/key                             "service-line-item"
    :cart-item-title/id                    "line-item-title-upsell-service"
    :cart-item-title/primary               (or
                                            (:product/essential-title service-line-item)
                                            (:legacy/product-name service-line-item))
    :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                             :value (:copy/whats-included service-line-item)}
                                            {:id    (str "line-item-quantity-" sku-id)
                                             :value (str "qty. " (:item/quantity service-line-item))}]
    :cart-item-floating-box/id             "line-item-service-price"
    :cart-item-floating-box/contents       [{:text (some-> service-line-item
                                                           ((some-fn
                                                             :product/essential-price
                                                             :sku/price))
                                                           mf/as-money) :attrs {:class "strike"}}
                                            {:text "FREE" :attrs {:class "s-color"}}]
    :cart-item-service-thumbnail/id        "service"
    :cart-item-service-thumbnail/image-url (hacky-cart-image service-line-item)}
   (merge  (when-let [addon-services (-> service-line-item :item.service/addons seq)]
            {:cart-item-sub-items/id    "add-on-services"
             :cart-item-sub-items/title "Add-On Services"
             :cart-item-sub-items/items (map (fn [{:sku/keys [title price] sku-id :catalog/sku-id}]
                                               {:cart-item-sub-item/title  title
                                                :cart-item-sub-item/price  (mf/as-money price)
                                                :cart-item-sub-item/sku-id sku-id})
                                             addon-services)})
           (when (seq addon-facets)
             {:cart-item.addons/id    "addon-services"
              :cart-item.addons/title "Add-On Services"
              :cart-item.addons/elements
              (->> addon-facets
                   (mapv (fn [facet]
                           {:cart-item.addon/title (:facet/name facet)
                            :cart-item.addon/price (some-> facet :service/price mf/as-money)
                            :cart-item.addon/id    (:service/sku-part facet)})))}))))

(defn cart-items-query
  [items]
  (for [{sku-id :catalog/sku-id
         images :selector/images
         :as    item} items
        :let [image-id (->> item
                            (catalog-images/image (maps/index-by :catalog/image-id images) "cart")
                            :ucare/id)]]
    {:react/key                                (str sku-id "-" (:item/quantity item))
     :cart-item-title/id                       (str "line-item-title-" sku-id)
     :cart-item-title/primary                  (:hacky/cart-title item)
     :cart-item-title/secondary                (-> item :join/facets :hair/color :sku/name)
     :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                 :value (str "qty. " (:item/quantity item))}]
     :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" sku-id)
     :cart-item-floating-box/contents          [{:text  (-> item :sku/price mf/as-money)
                                                 :attrs {:data-test (str "line-item-price-ea-" sku-id)}}
                                                {:text " each" :attrs {:class "proxima content-4"}}]
     :cart-item-square-thumbnail/id            sku-id
     :cart-item-square-thumbnail/sku-id        sku-id
     :cart-item-square-thumbnail/sticker-label (-> item :join/facets :hair/length :option/name)
     :cart-item-square-thumbnail/ucare-id      image-id}))

(defn query [data]
  (let [skus          (get-in data keypaths/v2-skus)
        skus-db       skus
        shared-cart   (get-in data keypaths/shared-cart-current)
        products      (get-in data keypaths/v2-products)
        facets        (get-in data keypaths/v2-facets)
        line-items    (some->> shared-cart
                               :line-items
                               (shared-cart/enrich-line-items-with-sku-data skus)
                               (map (partial add-product-title-and-color-to-line-item products facets)))
        album-keyword (get-in data keypaths/selected-album-keyword)
        look          (contentful/look->look-detail-social-card album-keyword
                                                                (contentful/selected-look data))
        album-copy    (get ugc/album-copy album-keyword)
        back          (first (get-in data keypaths/navigation-undo-stack))
        back-event    (:default-back-event album-copy)
        {:order/keys                                  [items]
         item-quantity                                :order.items/quantity
         {:keys [adjustments line-items-total total]} :waiter/order}
        (api.orders/shared-cart->order data skus-db shared-cart)

        discountable-services (select ?discountable items)]
    (merge {:shared-cart           shared-cart
            :look                  look
            :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
            :skus                  skus
            :sold-out?             (not-every? :inventory/in-stock? line-items)
            :fetching-shared-cart? (or (not look) (utils/requesting? data request-keys/fetch-shared-cart))
            :base-price            line-items-total
            :discounted-price      total
            :quadpay-loaded?       (get-in data keypaths/loaded-quadpay)
            :discount-text         (if (some (comp (partial = 0) :promo.mayvenn-install/hair-missing-quantity) discountable-services)
                                     "Hair + FREE Service"
                                     (->> adjustments
                                          (filter (comp not (partial contains? #{"freeinstall"}) :name))
                                          first
                                          :name))
            :cart-items            (->> items
                                        (select ?physical)
                                        cart-items-query
                                        shared-cart/sort-by-depart-and-price)
            :service-line-items    (mapv (partial service-line-item-query) discountable-services)

            :look-customization  (when (experiments/look-customization? data)
                                   {:look-customization.button/target [events/control-show-look-customization-modal]
                                    :look-customization.button/id     "customize-the-look"
                                    :look-customization.button/title  "Customize the look"})
            :carousel/images     (imgs (get-in data keypaths/v2-images) look items)
            :items-title/id      "item-quantity-in-look"
            :items-title/primary (str item-quantity " items in this " (:short-name album-copy))

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
  (if (experiments/look-customization? data)
    (component/build look-details-v202105/component (look-details-v202105/query data) opts)
    (component/build component (query data) opts)))
