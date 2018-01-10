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
            [clojure.set :as set]
            [spice.maps :as maps]
            [storefront.accessors.images :as images]))

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
                         bundle-deal-look? shared-cart-type-copy back-copy]} owner opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      [:div.col-6-on-tb-dt
       [:a.p2.px3-on-tb-dt.left.col-12.dark-gray
        (if bundle-deal-look?
          (utils/route-to events/navigate-shop-bundle-deals)
          (utils/route-back-or-to back events/navigate-shop-by-look))
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
             (when bundle-deal-look?
               [:div.center.teal.medium.mt2 "*Discounts applied at check out"])
             [:div.mt2
              (add-to-cart-button sold-out? creating-order? shared-cart)]])))]])))

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
  (let [skus        (get-in data keypaths/v2-skus)

        shared-cart-with-skus (put-skus-on-shared-cart
                               (get-in data keypaths/shared-cart-current)
                               skus)

        look              (pixlee/selected-look data)
        bundle-deal-ids   (->> (pixlee/images-in-album (get-in data keypaths/ugc) :bundle-deals)
                               (remove (comp #{"video"} :content-type))
                               (mapv :id)
                               set)
        bundle-deal-look? (boolean (bundle-deal-ids (:id look)))
        back              (first (get-in data keypaths/navigation-undo-stack))]
    {:shared-cart           shared-cart-with-skus
     :look                  look
     :bundle-deal-look?     bundle-deal-look?
     :creating-order?       (utils/requesting? data request-keys/create-order-from-shared-cart)
     :skus                  skus
     :sold-out?             (not-every? :inventory/in-stock? (:line-items shared-cart-with-skus))
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
