(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [clojure.set :as set]
            [clojure.string :as str]
            [sablono.core :refer [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.money-formatters :as mf]
            [storefront.components.order-summary :as order-summary]
            [storefront.components.ugc :as ugc]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.config :as config]
            [storefront.events :as events]
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

(defn component
  [{:keys [creating-order? sold-out? look shared-cart skus back fetching-shared-cart?
           shared-cart-type-copy back-copy back-event above-button-copy album-keyword
           look-detail-price? base-price discounted-price]} owner opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      [:div.col-6-on-tb-dt
       [:a.p2.px3-on-tb-dt.left.col-12.dark-gray
        (if (and (not back) back-event)
          (utils/fake-href back-event)
          (utils/route-back-or-to back events/navigate-shop-by-look {:album-keyword album-keyword}))
        (ui/back-caret back-copy)]

       [:h1.h3.medium.center.dark-gray.mb2 (str "Get this " shared-cart-type-copy)]]]
     [:div.clearfix
      (when look
        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
         (carousel (imgs look shared-cart))
         [:div.px3.py2.mbp1.bg-light-gray
          [:div.flex.items-center
           [:div.flex-auto.dark-gray.medium {:style {:word-break "break-all"}}
            (:title look)]
           [:div.ml1.line-height-1 {:style {:width "1em" :height "1em"}}
            (svg/social-icon (:social-service look))]]]
         (when-not (str/blank? (:description look))
           [:p.h5.px3.py1.dark-gray.bg-light-gray (:description look)])])
      (if fetching-shared-cart?
        [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
        (when shared-cart
          (let [line-items (:line-items shared-cart)
                item-count (->> line-items (map :item/quantity) (reduce + 0))]
            [:div.col-on-tb-dt.col-6-on-tb-dt.px2.px3-on-tb-dt
             [:div.p2.center.h3.medium (str item-count " items in this " shared-cart-type-copy)]
             (order-summary/display-line-items line-items skus)
             [:div.px2.border-top.border-gray]
             (when look-detail-price?
               [:div.center.mt4.mb3
                [:div.h6.dark-gray "15% Off + 10% Bundle Discount"]
                [:div.h2.medium (mf/as-money discounted-price)]
                [:div.strike.dark-gray (mf/as-money base-price)]])
             (when above-button-copy
               [:div.center.teal.medium.mt2 above-button-copy])
             [:div.mt2
              (add-to-cart-button sold-out? creating-order? look shared-cart)]])))]])))

(defn adventure-component
  [{:keys [creating-order? sold-out? look shared-cart skus fetching-shared-cart?
           shared-cart-type-copy above-button-copy base-price discounted-price]}
   owner
   opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      (when look
        [:div
         (carousel (imgs look shared-cart))
         [:div.px3.py2.mbp1.bg-light-gray
          [:div.flex.items-center
           [:div.flex-auto.dark-gray.medium
            {:style {:word-break "break-all"}}
            [:div.left (:title look)]]
           [:div.ml1.line-height-1 {:style {:width "1.5em" :height "1.5em"}}
            (svg/social-icon (:social-service look))]]]
         (when-not (str/blank? (:description look))
           [:p.h5.px3.py1.dark-gray.bg-light-gray (:description look)])])
      (if fetching-shared-cart?
        [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
        (when shared-cart
          (let [line-items (:line-items shared-cart)
                item-count (->> line-items (map :item/quantity) (reduce + 0))]
            [:div.px2
             [:div.p2.center.h3.medium
              {:data-test "item-quantity-in-look"}
              (str item-count " items in this " shared-cart-type-copy)]
             (order-summary/display-line-items line-items skus)
             [:div.center.mb3
              [:div.h6.dark-gray "10% Bundle Discount + Free Install"]
              [:div.h2.medium (mf/as-money discounted-price)]
              [:div.strike.dark-gray (mf/as-money base-price)]]
             (when above-button-copy
               [:div.center.teal.medium.mt2 above-button-copy])
             [:div.mt2
              (add-to-cart-button sold-out? creating-order? look shared-cart)]])))]])))

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

        look       (if (experiments/pixlee-to-contentful? data)
                     (ugc/contentful-look->look-detail-social-card navigation-event album-keyword (ugc/selected-look data))
                     (ugc/pixlee-look->look-detail-social-card (pixlee/selected-look data)))
        album-copy (-> config/pixlee :copy album-keyword)
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
     :look-detail-price?    (and (experiments/look-detail-price? data)
                                 (not= album-keyword :deals))
     :base-price            base-price
     :discounted-price      (* 0.75 base-price)}))

(defn adventure-query [data]
  (let [skus (get-in data keypaths/v2-skus)

        shared-cart-with-skus (some-> (get-in data keypaths/shared-cart-current)
                                      (put-skus-on-shared-cart skus))

        navigation-event (get-in data keypaths/navigation-event)
        album-keyword    (get-in data keypaths/selected-album-keyword)
        look             (if (experiments/pixlee-to-contentful? data)
                           (ugc/contentful-look->look-detail-social-card navigation-event album-keyword (ugc/selected-look data))
                           (ugc/pixlee-look->look-detail-social-card (pixlee/selected-look data)))
        album-copy       (-> config/pixlee :copy album-keyword)
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
     :shared-cart-type-copy (:short-name album-copy)
     :look-detail-price?    (and (experiments/look-detail-price? data)
                                 (not= album-keyword :deals))
     :base-price            base-price
     :discounted-price      (* 0.90 base-price)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
