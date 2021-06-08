(ns catalog.look-details-v202105
  "Shopping by Looks: Detail page for an individual 'look'"
  (:require #?@(:cljs [[storefront.browser.scroll :as scroll]
                       [storefront.hooks.quadpay :as quadpay]
                       [storefront.platform.messages :as messages]
                       ;; popups, must be required to load properly
                       looks.customization-modal])
            [api.catalog :refer [select  ?discountable ?physical ?service ?model-image ?cart-product-image]]
            api.orders
            api.products
            [catalog.facets :as facets]
            catalog.keypaths
            catalog.products
            [catalog.selector.sku :as sku-selector]
            [catalog.looks :as looks]
            [clojure.string :as string]
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as products]
            [storefront.accessors.shared-cart :as shared-cart]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.picker.picker-two :as picker]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.reviews :as reviews]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.ugc :as ugc]
            ))

(defn first-when-only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn product-skus
  [app-state]
  (let [shared-cart-sku-ids (->> (get-in app-state keypaths/shared-cart-current)
                                 :line-items
                                 (remove (comp #(string/starts-with? % "SV2") :catalog/sku-id))
                                 (map :catalog/sku-id))
        skus           (get-in app-state keypaths/v2-skus)
        product-ids    (mapcat
                        #(-> (get skus %)
                             :selector/from-products
                             first)
                        shared-cart-sku-ids)
        products       (mapv (partial catalog.products/product-by-id app-state) product-ids)]
    (mapcat (partial catalog.products/extract-product-skus app-state) products)))

(defn ^:private get-product-from-sku
  [app-state sku-id]
  (-> app-state
      (get-in keypaths/v2-skus)
      (get sku-id)
      :selector/from-products
      first))

(defn determine-new-case-sku-from-selection
  [app-state old-case-sku new-selections]
  (let [product-sku    (get-product-from-sku app-state old-case-sku)
        old-selections (get-in app-state catalog.keypaths/detailed-look-selections)]
    (->> product-sku
         vec
         (selector/match-all {} (merge old-selections new-selections))
         first-when-only)))

;; shared cart -> sku -> product
(defn- generate-look-options
  [app-state case-sku-id]
  (let [product-id   (-> app-state
                         (get-in keypaths/v2-skus)
                         (get case-sku-id)
                         :selector/from-products
                         first)
        product      (catalog.products/product-by-id app-state product-id)
        facets       (facets/by-slug app-state)
        product-skus (product-skus app-state)
        images       (get-in app-state keypaths/v2-images)]
    (sku-selector/product-options facets product product-skus images)))

(defn- generate-look-noptions
  [app-state physical-line-items]
  (let [look-options (->> physical-line-items
                          (map :catalog/sku-id)
                          (map (partial generate-look-options app-state)))]
    {:hair/color (->> look-options
                      (mapcat :hair/color)
                      (map #(dissoc % :price :stocked? :filter/order))
                      distinct)
     :hair/lengths (map #(dissoc % :hair/color) look-options)}))

(defmethod transitions/transition-state events/control-look-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (update-in app-state catalog.keypaths/detailed-look-selections merge {selection value}))

(defmethod effects/perform-effects events/control-look-detail-picker-option-select
  [_ event {:keys [selection value]} _ _]
  #?(:cljs (messages/handle-later events/control-look-detail-picker-close)))

(defmethod transitions/transition-state events/control-look-detail-picker-open
  [_ event {:keys [facet-slug]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-look-selected-picker facet-slug)
      (assoc-in catalog.keypaths/detailed-look-picker-visible? true)))

(defmethod transitions/transition-state events/control-look-detail-picker-close
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/detailed-look-picker-visible? false))

#?(:cljs
   (defmethod effects/perform-effects events/control-look-detail-picker-open
     [_ _ _ _ _]
     (scroll/disable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/control-look-detail-picker-close
     [_ _ _ _ _]
     (scroll/enable-body-scrolling)))

(defmethod transitions/transition-state events/initialize-look-details
  [_ event {:as args :keys [shared-cart]} app-state]
  (let [physical-line-items (->> shared-cart
                                 :line-items
                                 (remove (comp #(string/starts-with? % "SV2") :catalog/sku-id))
                                 (map (fn [{:keys [catalog/sku-id item/quantity]}]
                                        (-> (get-in app-state (conj keypaths/v2-skus sku-id))
                                            (assoc :item/quantity quantity))))
                                 (mapcat #(repeat (:item/quantity %) (dissoc % :item/quantity))))
        noptions            (generate-look-noptions app-state physical-line-items)
        initial-selections  (let [{:hair/keys [origin texture color]} (-> physical-line-items first)]
                              ;; TODO make this more tolerable
                              {:hair/origin  (first origin)
                               :hair/texture (first texture)
                               :hair/color   (first color)
                               :per-item     (mapv #(select-keys % [:hair/family :hair/length]) physical-line-items)})]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-look-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-look-picker-visible? nil)
        (assoc-in catalog.keypaths/detailed-look-selections initial-selections)
        (assoc-in catalog.keypaths/detailed-look-options noptions))))

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
      (when-not (string/blank? (:description look))
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
    ;; TODO: update event to control-initalize-cart-from-look when color/length can be changed
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
           base-price discounted-price  discount-text
           color-picker
           yotpo-data-attributes color-picker-face] :as queried-data}]
  [:div.clearfix
   (when look
     (component/build look-card-v202105 queried-data "look-card"))
   (if fetching-shared-cart?
     [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
     (when shared-cart
       [:div.px2.bg-refresh-gray
        (component/build picker/open-picker-component color-picker)
        (component/build look-title queried-data)

        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
         [:div.my4 (picker/color-picker-face color-picker-face)]
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
                             {:quadpay/show?       (:quadpay-loaded? queried-data)
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
                                                              (partial maps/index-by :option/slug)))))
        look-data             (looks/look<- skus-db looks-shared-carts-db facets-db look album-keyword nil)
        selections            (get-in data catalog.keypaths/detailed-look-selections)
        color-options         (->> (get-in data catalog.keypaths/detailed-look-options)
                                   :hair/color
                                   (mapv (fn [{:option/keys [slug] :as option}]
                                           (merge option
                                                  {:id               (str "picker-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
                                                   :selection-target [events/control-look-detail-picker-option-select
                                                                      {:selection :hair/color
                                                                       :value     slug}]
                                                   :checked?         (= (:hair/color selections) slug)}))))]
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
            :return-link/back          back

            ;; TODO: unify face and modal?
            :color-picker      (picker/open-picker-query
                                {:data         data
                                 :options      color-options
                                 :open?        (and (= :hair/color (get-in data catalog.keypaths/detailed-look-selected-picker))
                                                    (get-in data catalog.keypaths/detailed-look-picker-visible?))
                                 :picker-type  :hair/color
                                 :close-target [events/control-look-detail-picker-close]})
            :color-picker-face {:selected-color   (first (filter :checked? color-options))
                                :selection-target [events/control-look-detail-picker-option-select {:selection :hair/color}]
                                :options          color-options
                                :open-target      [events/control-look-detail-picker-open {:facet-slug :hair/color}]}}
           look-data
           #?(:cljs (reviews/query-look-detail shared-cart data)))))

(defcomponent component
  [queried-data owner opts]
  [:div.container.mb4
   (look-details-body queried-data)])

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
