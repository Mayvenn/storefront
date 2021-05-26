(ns catalog.look-details-v202105
  "Shopping by Looks: Detail page for an individual 'look'"
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.hooks.quadpay :as quadpay]
                       ;; popups, must be required to load properly
                       looks.customization-modal])
            [api.catalog :refer [select  ?discountable ?physical ?service ?model-image ?cart-product-image]]
            api.orders
            [catalog.facets :as facets]
            [catalog.images :as catalog-images]
            [catalog.looks :as looks]
            [clojure.string :as str]
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as products]
            [storefront.accessors.shared-cart :as shared-cart]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
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

(defn carousel [data imgs]
  (component/build carousel/component
                   {:dependencies data}
                   {:opts {:settings {:nav         true
                                      :edgePadding 0
                                      :controls    true
                                      :items       1}
                           :slides   imgs}}))

(component/defcomponent look-card-v202105
  [{:keys [shared-cart look yotpo-data-attributes] :as queried-data} _ _]
  [:div.bg-refresh-gray.p3
   [:div.bg-white
    [:div.slides-middle.col-on-tb-dt.col-6-on-tb-dt.p3
     [:div
      (when shared-cart
        (carousel {:look look :shared-cart shared-cart} (:carousel/images queried-data)))]
     [:div.pb3.pt1
      [:div.flex.items-center
       [:div.flex-auto.proxima {:style {:word-break "break-all"}}
        (:title look)]
       [:div.ml1.line-height-1 {:style {:width  "21px"
                                        :height "21px"}}
        ^:inline (svg/instagram {:class "fill-dark-gray"})]]
      (when yotpo-data-attributes
        (component/build reviews/reviews-summary-component {:yotpo-data-attributes yotpo-data-attributes} nil))
      (when-not (str/blank? (:description look))
        [:p.mt1.content-4.proxima.dark-gray (:description look)])]]]])

(component/defcomponent look-title
  [{:look/keys [title total-price discounted-price id secondary-id secondary cart-number discounted?] :as look} _ _]
  [:div.py2
   [:div.proxima.title-2.shout
    title]
   [:div.flex.justify-between.mt2
    [:div
     [:div
      discounted-price
      (when discounted?
        [:span.strike.content-4.ml2
         total-price])]
     [:div.shout.button-font-4 secondary]]
    ;; NOTE: update event to control-initalize-cart-from-look when color/length can be changed
    [:div.right-align (ui/button-small-primary (merge (utils/fake-href events/control-create-order-from-look
                                                                       {:shared-cart-id cart-number
                                                                        :look-id        (:id id)})
                                                      {:data-test        "add-to-cart-submit"
                                                       :disabled?        false #_(or (not look) sold-out?)
                                                       :disabled-content false #_(when sold-out? "Sold Out")
                                                       :spinning?        false #_creating-order?})
                                               "Add to bag")]]])

(defn look-details-body
  [{:keys [creating-order? sold-out? look shared-cart fetching-shared-cart?
           base-price discounted-price quadpay-loaded? discount-text
           yotpo-data-attributes] :as queried-data}]
  [:div.clearfix
   (when look
     (component/build look-card-v202105 queried-data "look-card"))
   (if fetching-shared-cart?
     [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
     (when shared-cart
       [:div.px2.bg-refresh-gray
        (component/build look-title queried-data)

        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt.bg-white
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
         (component/build reviews/reviews-component {:yotpo-data-attributes yotpo-data-attributes} nil)]]))])

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

(defn query [data]
  (let [skus            (get-in data keypaths/v2-skus)
        skus-db         skus
        shared-cart     (get-in data keypaths/shared-cart-current)
        products        (get-in data keypaths/v2-products)
        facets          (get-in data keypaths/v2-facets)
        line-items      (some->> shared-cart
                                 :line-items
                                 (shared-cart/enrich-line-items-with-sku-data skus)
                                 (map (partial add-product-title-and-color-to-line-item products facets)))
        album-keyword   (get-in data keypaths/selected-album-keyword)
        look            (get-in data (conj keypaths/cms-ugc-collection-all-looks (get-in data keypaths/selected-look-id))) ;;take out
        contentful-look (contentful/look->look-detail-social-card album-keyword
                                                                  (contentful/selected-look data))
        back            (first (get-in data keypaths/navigation-undo-stack))
        album-copy      (get ugc/album-copy album-keyword)
        back-event      (:default-back-event album-copy)
        {:order/keys                                  [items]
         {:keys [adjustments line-items-total total]} :waiter/order}
        (api.orders/shared-cart->order data skus-db shared-cart)

        discountable-services (select ?discountable items)

        ;; Looks query
        looks-shared-carts-db (get-in data storefront.keypaths/v1-looks-shared-carts)
        facets-db             (->> (get-in data storefront.keypaths/v2-facets)
                                   (maps/index-by (comp keyword :facet/slug))
                                   (maps/map-values (fn [facet]
                                                      (update facet :facet/options
                                                              (partial maps/index-by :option/slug)))))]
    (merge {:shared-cart               shared-cart
            :look                      contentful-look
            :creating-order?           (utils/requesting? data request-keys/create-order-from-shared-cart)
            :skus                      skus
            :sold-out?                 (not-every? :inventory/in-stock? line-items)
            :fetching-shared-cart?     (or (not contentful-look) (utils/requesting? data request-keys/fetch-shared-cart))
            :base-price                line-items-total
            :discounted-price          total
            :quadpay-loaded?           (get-in data keypaths/loaded-quadpay)
            :discount-text             (if (some (comp (partial = 0) :promo.mayvenn-install/hair-missing-quantity) discountable-services)
                                         "Hair + FREE Service"
                                         (->> adjustments
                                              (filter (comp not (partial contains? #{"freeinstall"}) :name))
                                              first
                                              :name))
            :carousel/images           (imgs (get-in data keypaths/v2-images) contentful-look items)
            :return-link/event-message (if (and (not back) back-event)
                                         [back-event]
                                         [events/navigate-shop-by-look {:album-keyword album-keyword}])
            :return-link/back          back}
           (looks/look<- skus-db looks-shared-carts-db facets-db look album-keyword nil)
           #?(:cljs (reviews/query-look-detail shared-cart data)))))

(defcomponent component
  [queried-data owner opts]
  [:div.container.mb4
   (look-details-body queried-data)])

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
