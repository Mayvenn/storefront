(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [clojure.set :as set]
            [clojure.string :as str]
            [spice.core :as spice]
            [sablono.core :refer [html]]
            [storefront.accessors.images :as images]
            [storefront.accessors.contentful :as contentful]
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
            [ui.molecules :as ui-molecules]
            
            
            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defn add-to-cart-button
  [sold-out? creating-order? look {:keys [number]}]
  (if sold-out?
    [:div.btn.col-12.h5.btn-primary.bg-gray.white
     {:on-click nil}
     "Sold Out"]
    (ui/teal-button
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
             :settings {:dots true}}
            {:react-key "look-carousel"}))

(defn distinct-product-imgs [{:keys [line-items]}]
  (->> (map (partial images/image-by-use-case "carousel") line-items)
       (remove nil?)
       distinct
       (map (fn [img] [:img.col-12 img]))))

(defn imgs [look shared-cart]
  (cons (ui/aspect-ratio
         1 1
         {:class "bg-black"}
         [:div.container-size.bg-cover.bg-no-repeat.bg-center
          {:style {:background-image (str "url(" (:image-url look) ")")}}])
        (distinct-product-imgs shared-cart)))

(defn display-line-item
  "Storeback now returns shared-cart line-items as a v2 Sku + item/quantity, aka
  'line-item-skuer' This component is also used to display line items that are
  coming off of a waiter order which is a 'variant' with a :quantity
  Until waiter is updated to return 'line-item-skuers', this function must handle
  the two different types of input"
  [line-item {:keys [catalog/sku-id] :as sku} thumbnail quantity-line]
  (let [legacy-variant-id (or (:legacy/variant-id line-item) (:id line-item))
        price             (or (:sku/price line-item)         (:unit-price line-item))
        title             (or (:sku/title line-item)         (products/product-title line-item))]
    [:div.clearfix.py3 {:key legacy-variant-id}
     [:a.left.mr1
      [:img.block.border.border-gray.rounded.hide-on-mb
       (assoc thumbnail :style {:width  "117px"
                                :height "117px"})]
      [:img.block.border.border-gray.rounded.hide-on-tb-dt
       (assoc thumbnail :style {:width  "132px"
                                :height "132px"})]]
     [:div.overflow-hidden
      [:div.ml1
       [:a.medium.titleize.h5
        {:data-test (str "line-item-title-" sku-id)}
        title]
       [:div.h6.mt1.line-height-1
        (when-let [length (:hair/length sku)]
          ;; TODO use facets once it's not painful to do so
          [:div.pyp2
           {:data-test (str "line-item-length-" sku-id)}
           "Length: " length "\""])
        [:div.pyp2
         {:data-test (str "line-item-price-ea-" sku-id)}
         "Price Each: " (mf/as-money-without-cents price)]
        quantity-line]]]]))

(defn display-line-items [line-items skus]
  (for [line-item line-items]
    (let [sku-id   (or (:catalog/sku-id line-item) (:sku line-item))
          sku      (get skus sku-id)
          quantity (or (:item/quantity line-item) (:quantity line-item))]
      (display-line-item line-item
                         sku
                         (images/cart-image sku)
                         [:div.pyp2 "Quantity: " quantity]))))

(defn look-details-body
  [{:keys [creating-order? sold-out? look shared-cart skus fetching-shared-cart?
           shared-cart-type-copy above-button-copy base-price discounted-price
           quadpay-loaded? discount-text desktop-two-column? yotpo-data-attributes]}]
  [:div.clearfix
   (when look
     [:div
      (when desktop-two-column?
        {:class "col-on-tb-dt col-6-on-tb-dt px3-on-tb-dt"})
      (carousel (imgs look shared-cart))
      [:div.px3.pt2.bg-white
       [:div.flex.items-center
        [:div.flex-auto.medium {:style {:word-break "break-all"}}
         (:title look)]
        [:div.ml1.line-height-1 {:style {:width  "21px"
                                         :height "21px"}}
         ^:inline (svg/instagram)]]]
      (when yotpo-data-attributes
        [:div (component/build reviews/reviews-summary-component {:yotpo-data-attributes yotpo-data-attributes} nil)])
      (when-not (str/blank? (:description look))
        [:p.h7.px3.pb1.dark-gray.bg-white.clearfix (:description look)])])
   (if fetching-shared-cart?
     [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
     (when shared-cart
       (let [line-items (:line-items shared-cart)
             item-count (->> line-items (map :item/quantity) (reduce + 0))]
         [:div.px2
          {:class
           (if desktop-two-column?
             "col-on-tb-dt col-6-on-tb-dt px3-on-tb-dt"
             [:mt3])}
          [:div.border-top.border-light-gray.mt2.mxn2
           (when desktop-two-column? {:class "hide-on-tb-dt"})]
          [:div.h4.medium.pt2
           {:data-test "item-quantity-in-look"}
           (str item-count " items in this " shared-cart-type-copy)]
          (display-line-items line-items skus)
          [:div.border-top.border-light-gray.mxn2]
          [:div.center.pt2.mb3
           (when discount-text
             [:p.center.h5.flex.items-center.justify-center.bold
              (svg/discount-tag {:height "40px"
                                 :width  "40px"})
              discount-text])
           (when-not (= discounted-price base-price)
             [:div.strike.dark-gray.h6 (mf/as-money base-price)])
           [:div.h2.bold (mf/as-money discounted-price)]]
          (when above-button-copy
            [:div.center.teal.medium.mt2 above-button-copy])
          [:div.mt2.col-11.mx-auto
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
     [:div.clearfix
      [:div.col-6-on-tb-dt.p2
       (ui-molecules/return-link queried-data)]]
     (look-details-body queried-data)])

(defcomponent adventure-component
  [look-details owner opts]
  [:div.container.mb4
     (look-details-body look-details)])

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

(defn shared-cart-promo->discount
  [promotions price shared-cart-promo]
  (if (= "freeinstall" shared-cart-promo)
    {:discount-text    "10% OFF + FREE Install"
     :discounted-price (* 0.90 price)}
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
                                  (* price))
                          price)}))) ;; discounted price was unparsable

(defn query [data]
  (let [skus                  (get-in data keypaths/v2-skus)
        shared-cart-with-skus (some-> (get-in data keypaths/shared-cart-current)
                                      (put-skus-on-shared-cart skus))
        navigation-event      (get-in data keypaths/navigation-event)
        album-keyword         (get-in data keypaths/selected-album-keyword)

        look              (contentful/look->look-detail-social-card navigation-event album-keyword (contentful/selected-look data))
        album-copy        (get ugc/album-copy album-keyword)
        base-price        (apply + (map (fn [line-item]
                                          (* (:item/quantity line-item)
                                             (:sku/price line-item)))
                                        (:line-items shared-cart-with-skus)))
        shared-cart-promo (some-> shared-cart-with-skus :promotion-codes first str/lower-case)
        discount          (shared-cart-promo->discount (get-in data keypaths/promotions)
                                                       base-price
                                                       shared-cart-promo)
        back              (first (get-in data keypaths/navigation-undo-stack))
        back-event        (:default-back-event album-copy)]
    (merge {:shared-cart                shared-cart-with-skus
            :album-keyword              album-keyword
            :look                       look
            :creating-order?            (utils/requesting? data request-keys/create-order-from-shared-cart)
            :skus                       skus
            :sold-out?                  (not-every? :inventory/in-stock? (:line-items shared-cart-with-skus))
            :fetching-shared-cart?      (or (not look) (utils/requesting? data request-keys/fetch-shared-cart))
            :above-button-copy          (if-not (:discount-text discount)
                                          "*Discounts applied at check out"
                                          (:above-button-copy album-copy))
            :shared-cart-type-copy      (:short-name album-copy)
            :look-detail-price?         (not= album-keyword :deals)
            :base-price                 base-price
            :discounted-price           (:discounted-price discount)
            :quadpay-loaded?            (get-in data keypaths/loaded-quadpay)
            :desktop-two-column?        true
            :discount-text              (:discount-text discount)

            :return-link/copy           (:back-copy album-copy)
            :return-link/event-message (if (and (not back) back-event)
                                         [back-event]
                                         [events/navigate-shop-by-look {:album-keyword album-keyword}])
            :return-link/back          back}
           (reviews/query-look-detail shared-cart-with-skus data))))

(defn adventure-query [data]
  (let [skus (get-in data keypaths/v2-skus)

        shared-cart-with-skus (some-> (get-in data keypaths/shared-cart-current)
                                      (put-skus-on-shared-cart skus))

        navigation-event (get-in data keypaths/navigation-event)
        album-keyword    (get-in data keypaths/selected-album-keyword)
        look             (contentful/look->look-detail-social-card navigation-event album-keyword (contentful/selected-look data))
        album-copy       (get ugc/album-copy album-keyword)
        base-price       (apply + (map (fn [line-item]
                                         (* (:item/quantity line-item)
                                            (:sku/price line-item)))
                                       (:line-items shared-cart-with-skus)))]
    (merge {:shared-cart           shared-cart-with-skus
            :album-keyword         album-keyword
            :look                  look
            :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
            :skus                  skus
            :sold-out?             (not-every? :inventory/in-stock? (:line-items shared-cart-with-skus))
            :fetching-shared-cart? (or (not look) (utils/requesting? data request-keys/fetch-shared-cart))
            :back                  (first (get-in data keypaths/navigation-undo-stack))
            :back-event            (:default-back-event album-copy)
            :back-copy             (:back-copy album-copy)
            :above-button-copy     (:above-button-copy album-copy)
            :shared-cart-type-copy (if (str/includes? (some-> album-keyword name str) "bundle-set")
                                     "bundle set"
                                     "look")
            :base-price            base-price
            :discounted-price      (* 0.90 base-price)
            :quadpay-loaded?       (get-in data keypaths/loaded-quadpay)
            :desktop-two-column?   false
            :discount-text         "10% OFF + FREE Install"}
           (reviews/query-look-detail shared-cart-with-skus data))))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
