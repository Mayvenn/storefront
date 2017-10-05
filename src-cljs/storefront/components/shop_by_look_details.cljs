(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.components.ugc :as ugc]
            [storefront.assets :as assets]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.components.order-summary :as order-summary]
            [cemerick.url :as url]
            [clojure.string :as str]
            [storefront.accessors.products :as products]
            [spice.core :as spice]
            [spice.maps :as maps]))

(defn add-to-cart-button [sold-out? creating-order? {:keys [number]}]
  (if sold-out?
    [:div.btn.col-12.h5.btn-primary.bg-gray.white
     {:on-click nil}
     "Sold Out"]
    (ui/teal-button
     (assoc (utils/fake-href events/control-create-order-from-shared-cart {:shared-cart-id number})
            :spinning? creating-order?)
     "Add items to bag")))

(defn carousel [imgs]
  (om/build carousel/component
            {:slides   imgs
             :settings {:dots true}}
            {:react-key "look-carousel"}))

(defn distinct-product-imgs [shared-cart products]
  (->> shared-cart
       :line-items
       (map :product-id)
       (map (partial products/large-img products))
       (remove nil?)
       distinct
       (map (fn [img] [:img.col-12 img]))))

(defn imgs [look shared-cart products]
  (cons (ugc/content-view look)
        (distinct-product-imgs shared-cart products)))

(defn decode-title [title]
  (try
    ;; Sometimes Pixlee gives us URL encoded titles
    (js/decodeURIComponent title)
    (catch :default e
      title)))

(defn component [{:keys [creating-order? sold-out? look shared-cart products back fetching-shared-cart? discount-warning?]} owner opts]
  (om/component
   (html
    (let [shared-cart-type-copy (or (:short-name back) "look")]
      [:div.container.mb4
       [:div.clearfix
        [:div.col-6-on-tb-dt
         [:a.p2.px3-on-tb-dt.left.col-12.dark-gray
          (utils/route-back-or-to back events/navigate-shop-by-look)
          [:span
           [:img.px1.mbnp4 {:style {:height "1.25rem"}
                            :src   (assets/path "/images/icons/caret-left.png")}]
           (or (:back-copy back) "back")]]

         [:h1.h3.medium.center.dark-gray.mb2 (str "Get this " shared-cart-type-copy)]]]

       [:div.clearfix
        (when look
          [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
           (carousel (imgs look shared-cart products))
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
               (order-summary/display-line-items line-items products)
               (when discount-warning? [:div.center.teal.medium.mt2 "*Discounts applied at check out"])
               [:div.mt2
                (add-to-cart-button sold-out? creating-order? shared-cart)]])))]]))))

(defn sold-out? [variant-ids product]
  (->> product
       :variants
       (filter (comp variant-ids :id))
       (not-every? :in_stock)))

(defn query [data]
  (let [shared-cart (get-in data keypaths/shared-cart-current)
        variant-ids (set (map :id (:line-items shared-cart)))
        products    (get-in data keypaths/products)]
    {:shared-cart           shared-cart
     :look                  (pixlee/selected-look data)
     :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
     :products              products
     :sold-out?             (some (partial sold-out? variant-ids) (vals products))
     :fetching-shared-cart? (utils/requesting? data request-keys/fetch-shared-cart)
     :back                  (first (get-in data keypaths/navigation-undo-stack))
     :discount-warning?     (experiments/bundle-deals-2? data)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
