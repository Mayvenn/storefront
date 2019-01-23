(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [clojure.set :as set]
            [clojure.string :as str]
            [sablono.core :refer [html]]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.products :as products]
            [storefront.assets :as assets]
            [storefront.components.money-formatters :as mf]
            [storefront.components.order-summary :as order-summary]
            [storefront.components.ugc :as ugc]
            [storefront.components.ui :as ui]
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
  (cons (ugc/content-view look)
        (distinct-product-imgs shared-cart)))

(defn decode-title [title]
  (try
    ;; Sometimes Pixlee gives us URL encoded titles
    (js/decodeURIComponent title)
    (catch :default e
      title)))

(defn component [{:keys [creating-order? sold-out? look shared-cart skus back fetching-shared-cart? discount-warning?
                         shared-cart-type-copy back-copy back-event above-button-copy album-keyword look-detail-price?
                         base-price discounted-price]} owner opts]
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
         [:div.px3.py2.mbp1.bg-light-gray (ugc/user-attribution look)]
         (when-not (str/blank? (:title look))
           [:p.h5.px3.py1.dark-gray.bg-light-gray (decode-title (:title look))])])
      (if fetching-shared-cart?
        [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
        (when shared-cart
          (let [line-items (:line-items shared-cart)
                item-count (->> line-items (map :item/quantity) (reduce + 0))]
            [:div.col-on-tb-dt.col-6-on-tb-dt.px2.px3-on-tb-dt
             [:div.p2.center.h3.medium.border-bottom.border-gray (str item-count " items in this " shared-cart-type-copy)]
             (order-summary/display-line-items line-items skus)
             (when look-detail-price?
               [:div.center.mt4.mb3
                [:div.h6.dark-gray "15% Off + 10% Bundle Discount"]
                [:div.h2.medium (mf/as-money discounted-price)]
                [:div.strike.dark-gray (mf/as-money base-price)]])
             (when above-button-copy
               [:div.center.teal.medium.mt2 above-button-copy])
             [:div.mt2
              (add-to-cart-button sold-out? creating-order? look shared-cart)]])))]])))

[:div {:style 5} {}]

(defn adventure-component [{:keys [creating-order? sold-out? look shared-cart skus back fetching-shared-cart? discount-warning?
                                   shared-cart-type-copy back-copy back-event above-button-copy album-keyword look-detail-price?
                                   base-price discounted-price]} owner opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      (when look
        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
         (carousel (imgs look shared-cart))
         [:div.px3.py2.mbp1.bg-light-gray (ugc/adventure-user-attribution look)]
         (when-not (str/blank? (:title look))
           [:p.h5.px3.py1.dark-gray.bg-light-gray (decode-title (:title look))])])
      (if fetching-shared-cart?
        [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
        (when shared-cart
          (let [line-items (:line-items shared-cart)
                item-count (->> line-items (map :item/quantity) (reduce + 0))]
            [:div.col-on-tb-dt.col-6-on-tb-dt.px2.px3-on-tb-dt
             [:div.p2.center.h3.medium.border-bottom.border-gray
              {:data-test "item-quantity-in-look"}
              (str item-count " items in this " shared-cart-type-copy)]
             (order-summary/display-line-items line-items skus)
             (when look-detail-price?
               [:div.center.mt4.mb3
                [:div.h6.dark-gray "15% Off + 10% Bundle Discount"]
                [:div.h2.medium (mf/as-money discounted-price)]
                [:div.strike.dark-gray (mf/as-money base-price)]])
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
  (let [skus (get-in data keypaths/v2-skus)

        shared-cart-with-skus (put-skus-on-shared-cart
                               (get-in data keypaths/shared-cart-current)
                               skus)

        look          (pixlee/selected-look data)
        album-keyword (get-in data keypaths/selected-album-keyword)
        album-copy    (-> config/pixlee :copy album-keyword)
        base-price    (apply + (map (fn [line-item]
                                      (* (:item/quantity line-item)
                                         (:sku/price line-item)))
                                    (:line-items shared-cart-with-skus)))]
    {:shared-cart           shared-cart-with-skus
     :album-keyword         album-keyword
     :look                  look
     :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
     :skus                  skus
     :sold-out?             (not-every? :inventory/in-stock? (:line-items shared-cart-with-skus))
     :fetching-shared-cart? (utils/requesting? data request-keys/fetch-shared-cart)
     :back                  (first (get-in data keypaths/navigation-undo-stack))
     :back-event            (:default-back-event album-copy)
     :back-copy             (:back-copy album-copy)
     :above-button-copy     (:above-button-copy album-copy)
     :shared-cart-type-copy (:short-name album-copy)
     :look-detail-price?    (and (experiments/look-detail-price? data)
                                 (not= album-keyword :deals))
     :base-price            base-price
     :discounted-price      (* 0.75 base-price)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
