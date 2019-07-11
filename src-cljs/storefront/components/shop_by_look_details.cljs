(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [clojure.set :as set]
            [clojure.string :as str]
            [sablono.core :refer [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.products :as products]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.component :as component]
            [storefront.ugc :as ugc]
            [storefront.events :as events]
            [storefront.hooks.quadpay :as quadpay]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn add-to-cart-button [sold-out? creating-order? look {:keys [number]}]
  (if sold-out?
    [:div.btn.col-12.h5.btn-primary.bg-gray.white
     {:on-click nil}
     "Sold Out"]
    (ui/teal-button
     (merge (utils/fake-href events/control-create-order-from-shared-cart {:shared-cart-id number
                                                                           :look-id        (:id look)})
            {:data-test "add-to-cart-submit"
             :disabled? (not look)
             :spinning? creating-order?})
     "Add items to bag")))

(defn carousel [imgs]
  (om/build carousel/component
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



(defn component
  [{:keys [creating-order? sold-out? look shared-cart skus back fetching-shared-cart?
           shared-cart-type-copy back-copy back-event above-button-copy album-keyword
           look-detail-price? base-price discounted-price quadpay-loaded?]} owner opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      [:div.col-6-on-tb-dt
       [:a.p2.px3-on-tb-dt.left.col-12.dark-gray
        (if (and (not back) back-event)
          (utils/fake-href back-event)
          (utils/route-back-or-to back events/navigate-shop-by-look {:album-keyword album-keyword}))
        (ui/back-caret back-copy)]]]
     [:div.clearfix
      (when look
        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
         (carousel (imgs look shared-cart))
         [:div.px3.pt2.bg-white
          [:div.flex.items-center
           [:div.flex-auto.dark-gray.medium {:style {:word-break "break-all"}}
            (:title look)]
           [:div.ml1.line-height-1 {:style {:width "21px"
                                            :height "21px"
                                            :opacity 0.2}}
            (svg/social-icon (:social-service look))]]]
         (when-not (str/blank? (:description look))
           [:p.h7.px3.py1.dark-gray.bg-white (:description look)])])
      (if fetching-shared-cart?
        [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
        (when shared-cart
          (let [line-items (:line-items shared-cart)
                item-count (->> line-items (map :item/quantity) (reduce + 0))]
            [:div.col-on-tb-dt.col-6-on-tb-dt.px2.px3-on-tb-dt
             [:div.border-top.border-light-gray.mt2.mxn2.hide-on-tb-dt]
             [:div.h4.medium.pt2
              {:data-test "item-quantity-in-look"}
              (str item-count " items in this " shared-cart-type-copy)]
             (display-line-items line-items skus)
             [:div.border-top.border-light-gray.mxn2]
             [:div.center.mb3
              [:p.center.h5.flex.items-center.justify-center.bold
               (svg/discount-tag {:class  "mxnp6"
                                  :height "40px"
                                  :width  "40px"})
               "15% Off + 10% Bundle Discount"]
              [:div.strike.dark-gray.h6 (mf/as-money base-price)]
              [:div.h2.medium (mf/as-money discounted-price)]]
             (when above-button-copy
               [:div.center.teal.medium.mt2 above-button-copy])
             [:div.mt2.col-11.mx-auto
              (add-to-cart-button sold-out? creating-order? look shared-cart)]
             (component/build quadpay/component
                              {:show?       quadpay-loaded?
                               :order-total discounted-price
                               :directive   [:div.flex.justify-center.items-center
                                             "Just select"
                                             [:div.mx1 {:style {:width "70px" :height "14px"}}
                                              svg/quadpay-logo]
                                             "at check out."]}
                              nil)])))]])))

(defn adventure-component
  [{:keys [creating-order? sold-out? look shared-cart skus fetching-shared-cart?
           shared-cart-type-copy above-button-copy base-price discounted-price
           quadpay-loaded?]}
   owner
   opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      (when look
        [:div
         (carousel (imgs look shared-cart))
         [:div.px3.pt2.bg-white
          [:div.flex.items-center
           [:div.flex-auto.medium
            {:style {:word-break "break-all"}}
            [:div.left (:title look)]]
           [:div.ml1.line-height-1 {:style {:width   "21px"
                                            :height  "21px"
                                            :opacity 0.2}}
            (svg/social-icon (:social-service look))]]]
         (when-not (str/blank? (:description look))
           [:p.h7.px3.py1.dark-gray.bg-white
            (:description look)])])
      (if fetching-shared-cart?
        [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
        (when shared-cart
          (let [line-items (:line-items shared-cart)
                item-count (->> line-items (map :item/quantity) (reduce + 0))]
            [:div.px2.mt3
             [:div.border-top.border-light-gray.mt2.mxn2]
             [:div.h4.medium.pt2
              {:data-test "item-quantity-in-look"}
              (str item-count " items in this " shared-cart-type-copy)]
             (display-line-items line-items skus)
             [:div.border-top.border-light-gray.mxn2]
             [:div.center.mb3
              [:p.center.h5.flex.items-center.justify-center.bold
               (svg/discount-tag {:class  "mxnp6"
                                  :height "40px"
                                  :width  "40px"})
               "10% OFF + FREE Install"]
              [:div.strike.dark-gray.h6 (mf/as-money base-price)]
              [:div.h2.medium (mf/as-money discounted-price)]]
             (when above-button-copy
               [:div.center.teal.medium.mt2 above-button-copy])
             [:div.mt2.col-11.mx-auto
              (add-to-cart-button sold-out? creating-order? look shared-cart)]
             (component/build quadpay/component
                              {:show?       quadpay-loaded?
                               :order-total discounted-price
                               :directive   [:div.flex.justify-center.items-center
                                             "Just select"
                                             [:div.mx1 {:style {:width "70px" :height "14px"}}
                                              svg/quadpay-logo]
                                             "at check out."]}
                              nil)])))]])))

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

(defn query [data]
  (let [skus                  (get-in data keypaths/v2-skus)
        shared-cart-with-skus (some-> (get-in data keypaths/shared-cart-current)
                                      (put-skus-on-shared-cart skus))
        navigation-event      (get-in data keypaths/navigation-event)
        album-keyword         (get-in data keypaths/selected-album-keyword)

        look       (contentful/look->look-detail-social-card navigation-event album-keyword (contentful/selected-look data))
        album-copy (get ugc/album-copy album-keyword)
        base-price (apply + (map (fn [line-item]
                                   (* (:item/quantity line-item)
                                      (:sku/price line-item)))
                                 (:line-items shared-cart-with-skus)))]
    {:shared-cart           shared-cart-with-skus
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
     :shared-cart-type-copy (:short-name album-copy)
     :look-detail-price?    true #_(and (experiments/look-detail-price? data)
                                 (not= album-keyword :deals))
     :base-price            base-price
     :discounted-price      (* 0.75 base-price)
     :quadpay-loaded?       (get-in data keypaths/loaded-quadpay)}))

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
    {:shared-cart           shared-cart-with-skus
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
     :look-detail-price?    (and (experiments/look-detail-price? data)
                                 (not= album-keyword :deals))
     :base-price            base-price
     :discounted-price      (* 0.90 base-price)
     :quadpay-loaded?       (get-in data keypaths/loaded-quadpay)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
