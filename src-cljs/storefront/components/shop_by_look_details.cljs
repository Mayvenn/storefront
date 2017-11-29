(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.components.ugc :as ugc]
            [storefront.assets :as assets]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.order-summary :as order-summary]
            [clojure.string :as str]
            [storefront.accessors.products :as products]
            [storefront.accessors.black-friday :as black-friday]))

(defn add-to-cart-button [sold-out? creating-order? {:keys [number]}]
  (if sold-out?
    [:div.btn.col-12.h5.btn-primary.bg-gray.white
     {:on-click nil}
     "Sold Out"]
    (ui/teal-button
     (assoc (utils/fake-href events/control-create-order-from-shared-cart {:shared-cart-id number})
            :spinning? creating-order?)
     "Add items to bag")))

(def black-friday-run-up-button
  [:div.btn.mb1.col-12.h5.btn-primary.bg-gray.white
   {:on-click nil}
   "Get this deal on Black Friday"])

(defn carousel [imgs]
  (om/build carousel/component
            {:slides   imgs
             :settings {:dots true}}
            {:react-key "look-carousel"}))

(defn distinct-product-imgs [shared-cart products skus]
  (->> shared-cart
       :line-items
       (map :sku)
       (map (fn [sku-id]
              [(products/find-product-by-sku-id products sku-id)
               (get skus sku-id)]))
       (map (fn [[product sku]] (products/large-img product sku)))
       (remove nil?)
       distinct
       (map (fn [img] [:img.col-12 img]))))

(defn imgs [look shared-cart products skus]
  (cons (ugc/content-view look)
        (distinct-product-imgs shared-cart products skus)))

(defn decode-title [title]
  (try
    ;; Sometimes Pixlee gives us URL encoded titles
    (js/decodeURIComponent title)
    (catch :default e
      title)))

(defn component [{:keys [creating-order? sold-out? look shared-cart products skus back fetching-shared-cart? discount-warning?
                         show-run-up-button? bundle-deal-look? shared-cart-type-copy back-copy]} owner opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      [:div.col-6-on-tb-dt
       [:a.p2.px3-on-tb-dt.left.col-12.dark-gray
        (if bundle-deal-look?
          (utils/route-to events/navigate-shop-bundle-deals)
          (utils/route-back-or-to back events/navigate-shop-by-look))
        [:span
         [:img.px1.mbnp4 {:style {:height "1.25rem"}
                          :src   (assets/path "/images/icons/caret-left.png")}]
         back-copy]]

       [:h1.h3.medium.center.dark-gray.mb2 (str "Get this " shared-cart-type-copy)]]]
     [:div.clearfix
      (when look
        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
         (carousel (imgs look shared-cart products skus))
         [:div.px3.py2.mbp1.bg-light-gray (ugc/user-attribution look)]
         (when-not (str/blank? (:title look))
           [:p.h5.px3.py1.dark-gray.bg-light-gray (decode-title (:title look))])])
      (if fetching-shared-cart?
        [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
        (when shared-cart
          (let [line-items (:line-items shared-cart)
                item-count (->> line-items (map :quantity) (reduce +))]
            [:div.col-on-tb-dt.col-6-on-tb-dt.px2.px3-on-tb-dt
             [:div.p2.center.h3.medium.border-bottom.border-gray (str item-count " items in this " shared-cart-type-copy)]
             (order-summary/display-line-items-products line-items products skus)
             (when bundle-deal-look?
               [:div.center.teal.medium.mt2 "*Discounts applied at check out"])
             [:div.mt2
              (if show-run-up-button?
                black-friday-run-up-button
                (add-to-cart-button sold-out? creating-order? shared-cart))]])))]])))

(defn sold-out? [variant-ids skus]
  (->> skus
       (filter (fn [sku]
                 (contains? variant-ids (:legacy/variant-id sku))))
       (not-every? :inventory/in-stock?)))

(defn query [data]
  (let [shared-cart        (get-in data keypaths/shared-cart-current)
        variant-ids        (set (map :id (:line-items shared-cart)))
        skus               (get-in data keypaths/v2-skus)
        products           (get-in data keypaths/v2-products)
        look               (pixlee/selected-look data)
        bundle-deal-ids    (->> (pixlee/images-in-album (get-in data keypaths/ugc) :bundle-deals)
                                (remove (comp #{"video"} :content-type))
                                (mapv :id)
                                set)
        bundle-deal-look?  (boolean (bundle-deal-ids (:id look)))
        black-friday-stage (black-friday/stage data)
        back               (first (get-in data keypaths/navigation-undo-stack))]
    {:shared-cart           shared-cart
     :look                  look
     :bundle-deal-look?     bundle-deal-look?
     :show-run-up-button?   (and (not (#{:cyber-monday :black-friday :cyber-monday-extended} black-friday-stage))
                                 bundle-deal-look?)
     :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
     :products              products
     :skus                  skus
     :sold-out?             (some (partial sold-out? variant-ids) (vals skus))
     :fetching-shared-cart? (utils/requesting? data request-keys/fetch-shared-cart)
     :back                  (first (get-in data keypaths/navigation-undo-stack))
     :back-copy             (:back-copy back (if bundle-deal-look?
                                               "back to bundle deals"
                                               "back"))
     :shared-cart-type-copy (:short-name back (if bundle-deal-look?
                                                "deal"
                                                "look"))}))

(defn built-component [data opts]
  (om/build component (query data) opts))
