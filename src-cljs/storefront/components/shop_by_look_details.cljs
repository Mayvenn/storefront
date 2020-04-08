(ns storefront.components.shop-by-look-details
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [spice.core :as spice]
            [spice.selector :as selector]
            [storefront.accessors.images :as images]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.products :as products]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.effects :as effects]
            [storefront.api :as api]
            [adventure.keypaths :as adv-keypaths]
            [storefront.ugc :as ugc]
            [storefront.events :as events]
            [storefront.hooks.quadpay :as quadpay]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.reviews :as reviews]
            [storefront.request-keys :as request-keys]
            [ui.molecules :as ui-molecules]))

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
  (api/create-order-from-cart (get-in app-state keypaths/session-id)
                              shared-cart-id
                              look-id
                              (get-in app-state keypaths/user-id)
                              (get-in app-state keypaths/user-token)
                              (get-in app-state keypaths/store-stylist-id)
                              (get-in app-state keypaths/order-servicing-stylist-id)))

(defn carousel [imgs]
  (component/build carousel/component
                   {:slides   imgs
                    :settings {:nav         true
                               :edgePadding 0
                               :controls    true
                               :items       1}}))

(defn get-model-image
  [{:keys [selector/images copy/title]}]
  (when-let [image (->> images
                        (selector/match-all {:selector/strict? true}
                                            {:image/of #{"model"}})
                        first)]
    [:img.col-12.mb4
     {:src (str (:url image) "-/format/auto/")
      :alt title}]))

(defn get-cart-product-image
  [{:keys [selector/images copy/title]}]
  (when-let [image (->> images
                        (selector/match-all {:selector/strict? true}
                                            {:use-case #{"cart"}
                                             :image/of #{"product"}})
                        first)]
    [:img.col-12.mb4
     {:src (str (:url image) "-/format/auto/")
      :alt title}]))

(defn- sort-by-depart-and-price
  [items]
  (sort-by (fn [{:keys [catalog/department sku/price]}]
             [(first department) price])
           items))

(defn imgs [look {:keys [line-items]}]
  (let [sorted-line-items (sort-by-depart-and-price line-items)]
    (list
     [:img.col-12 {:src (str (:image-url look)) :alt ""}]
     (get-model-image (first sorted-line-items))
     (get-cart-product-image (first sorted-line-items)))))

(defn ^:private display-line-item
  [line-item {:keys [catalog/sku-id] :as sku} thumbnail quantity]
  (let [legacy-variant-id (or (:legacy/variant-id line-item) (:id line-item))
        price             (or (:sku/price line-item)         (:unit-price line-item))
        title             (or (:sku/title line-item)         (products/product-title line-item))]
    [:div.clearfix.py3 {:key legacy-variant-id}
     [:a.left.mr1
      [:img.block
       (assoc thumbnail :style {:width  "69px"
                                :height "68px"})]]
     [:div.overflow-hidden
      [:div.ml1.content-2.proxima
       [:a.medium.titleize
        {:data-test (str "line-item-title-" sku-id)}
        title]
       [:div.mt1.line-height-1.flex.justify-between
        [:div "qty:" quantity]
        [:div {:data-test (str "line-item-price-ea-" sku-id)}
         (mf/as-money price)]]]]]))

(defn ^:private display-line-items [line-items skus]
  (for [line-item line-items]
    (let [sku-id   (or (:catalog/sku-id line-item) (:sku line-item))
          sku      (get skus sku-id)
          quantity (or (:item/quantity line-item) (:quantity line-item))]
      (display-line-item line-item
                         sku
                         (images/cart-image sku)
                         quantity))))

(defn look-details-body
  [{:keys [creating-order? sold-out? look shared-cart skus fetching-shared-cart?
           shared-cart-type-copy base-price discounted-price quadpay-loaded?
           discount-text desktop-two-column? yotpo-data-attributes]}]
  [:div.clearfix
   (when look
     [:div.bg-cool-gray.slides-middle
      (when desktop-two-column?
        {:class "col-on-tb-dt col-6-on-tb-dt px3-on-tb-dt"})
      (when shared-cart
        (carousel (imgs look shared-cart)))
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
       (let [line-items (->> shared-cart :line-items sort-by-depart-and-price)
             item-count (->> line-items (map :item/quantity) (reduce + 0))]
         [:div.px2
          {:class
           (if desktop-two-column?
             "col-on-tb-dt col-6-on-tb-dt px3-on-tb-dt"
             [:mt3])}
          [:div.pt2.proxima.title-2.shout
           {:data-test "item-quantity-in-look"}
           (str item-count " items in this " shared-cart-type-copy)]
          (display-line-items line-items skus)
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
          (component/build quadpay/component
                           {:quadpay/show?       quadpay-loaded?
                            :quadpay/order-total discounted-price
                            :quadpay/directive   :just-select}
                           nil)
          (component/build reviews/reviews-component {:yotpo-data-attributes yotpo-data-attributes} nil)])))])

(defcomponent component
  [queried-data owner opts]
  [:div.container.mb4
   (look-details-body queried-data)])

(defn put-skus-on-shared-cart [shared-cart skus]
  (let [shared-cart-variant-ids (into #{}
                                      (map :legacy/variant-id)
                                      (:line-items shared-cart))
        shared-cart-skus        (filterv (fn [sku]
                                           (contains? shared-cart-variant-ids (:legacy/variant-id sku)))
                                         (vals skus))]
    (update shared-cart :line-items
            #(set/join % shared-cart-skus
                       {:legacy/variant-id :legacy/variant-id}))))

(defn shared-cart->discount-with-bundle-discount
  [{:keys [promotions base-price shared-cart-promo base-service]}]
  (if base-service
    (let [service-price (:sku/price base-service)]
      {:discount-text    "10% OFF + FREE Install"
       :discounted-price (* 0.90 (- base-price service-price))})
    (let [promotion% (some->> promotions
                              (filter (comp #{shared-cart-promo} str/lower-case :code))
                              first
                              :description
                              (re-find #"\b(\d\d?\d?)%")
                              second)]
      {:discount-text    (some-> promotion% (str "% + 10% Bundle Discount"))
       :discounted-price (or
                          (some->> promotion%
                                   spice/parse-int
                                   (* 0.01)
                                   (+ 0.10) ;; bundle-discount
                                   (- 1.0)  ;; 100% - discount %
                                   (* base-price))
                          base-price)}))) ;; discounted price was unparsable

(defn shared-cart->discount
  [{:keys [promotions base-price shared-cart-promo base-service]}]
  (if base-service
    (let [service-price (:sku/price base-service)]
      {:discount-text    "Hair + FREE Install"
       :discounted-price (* 0.90 (- base-price service-price))})
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


(defn query [data]
  (let [shared-cart-discount-parser-fn           (if (experiments/remove-bundle-discount? data)
                                                   shared-cart->discount
                                                   shared-cart->discount-with-bundle-discount)
        skus                                     (get-in data keypaths/v2-skus)
        shared-cart                              (get-in data keypaths/shared-cart-current)
        {shared-cart-skus :line-items
         :as              shared-cart-with-skus} (some-> shared-cart
                                                         (put-skus-on-shared-cart skus))
        navigation-event                         (get-in data keypaths/navigation-event)
        album-keyword                            (get-in data keypaths/selected-album-keyword)

        look       (contentful/look->look-detail-social-card navigation-event album-keyword
                                                             (contentful/selected-look data))
        album-copy (get ugc/album-copy album-keyword)

        base-price (->> shared-cart-skus
                        (map (fn [line-item]
                               (* (:item/quantity line-item)
                                  (:sku/price line-item))))
                        (apply + 0))

        discount (shared-cart-discount-parser-fn
                  {:promotions        (get-in data keypaths/promotions)
                   :shared-cart-promo (some-> shared-cart-with-skus :promotion-codes first str/lower-case)
                   :base-price        base-price
                   :base-service      (->> shared-cart-skus
                                           (filter (comp #(contains? % "base") :service/type))
                                           first)})

        back       (first (get-in data keypaths/navigation-undo-stack))
        back-event (:default-back-event album-copy)]
    (merge {:shared-cart           shared-cart-with-skus
            :album-keyword         album-keyword
            :look                  look
            :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
            :skus                  skus
            :sold-out?             (not-every? :inventory/in-stock? shared-cart-skus)
            :fetching-shared-cart? (or (not look) (utils/requesting? data request-keys/fetch-shared-cart))
            :shared-cart-type-copy (:short-name album-copy)
            :base-price            base-price
            :discounted-price      (:discounted-price discount)
            :quadpay-loaded?       (get-in data keypaths/loaded-quadpay)
            :desktop-two-column?   true
            :discount-text         (:discount-text discount)

            :return-link/event-message (if (and (not back) back-event)
                                         [back-event]
                                         [events/navigate-shop-by-look {:album-keyword album-keyword}])
            :return-link/back          back}
           (reviews/query-look-detail shared-cart-with-skus data))))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
