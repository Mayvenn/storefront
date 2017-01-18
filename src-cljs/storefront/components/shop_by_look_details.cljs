(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.assets :as assets]
            [storefront.events :as events]
            [storefront.routes :as routes]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.components.order-summary :as order-summary]
            [cemerick.url :as url]
            [clojure.string :as str]
            [storefront.accessors.products :as products]))

(defn add-to-cart-button [creating-order? {:keys [number]}]
  (ui/teal-button
   (assoc (utils/fake-href events/control-create-order-from-shared-cart {:shared-cart-id number})
          :spinning? creating-order?)
   "Add items to bag"))

(defn carousel [imgs]
  (om/build carousel/component
            {:slides (mapv (fn [img] [:img.col-12 img]) imgs)
             :settings {:dots true
                        :dotsClass "carousel-dots"}}
            {:react-key "look-carousel"}))

(defn distinct-product-imgs [shared-cart products]
  (->> shared-cart
       :line-items
       (map :product-id)
       (map (partial products/large-img products))
       (remove nil?)
       distinct))

(defn imgs [look shared-cart products]
  (cons (-> look :imgs :large)
        (distinct-product-imgs shared-cart products)))

(defn decode-title [title]
  (try
    ;; Sometimes Pixlee gives us URL encoded titles
    (js/decodeURIComponent title)
    (catch :default e
      title)))

(defn component [{:keys [creating-order? look shared-cart products will-return-to-shop-by-look? price-strikeout?]} owner opts]
  (om/component
   (html
    [:div.container.mb4
     [:div.clearfix
      [:div.col-6-on-tb-dt
       [:a.p2.px3-on-tb-dt.left.col-12.gray
        (utils/route-back-or-to will-return-to-shop-by-look? events/navigate-shop-by-look)
        [:span
         [:img.px1.mbnp4 {:style {:height "1.25rem"}
                          :src   (assets/path "/images/icons/carat-left.png")}]
         "back to shop by look"]]

       [:h1.h3.medium.center.dark-gray.mb2 "Get this look"]]]

     [:div.clearfix
      (when look
        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
         (carousel (imgs look shared-cart products))
         [:div
          [:div.px3.py2.mbp1.bg-light-silver
           [:div.medium.gray.h5.inline-block (str "@" (:user-handle look))]
           [:div.right.inline-block {:style {:width  "20px"
                                             :height "20px"}}
            (svg/social-icon (:social-service look))]]
          (when-not (str/blank? (:title look))
            [:p.h5.px3.py1.gray.bg-light-silver (decode-title (:title look))])]])
      (when shared-cart
        (let [line-items (:line-items shared-cart)
              item-count (->> line-items (map :quantity) (reduce +))]
          [:div.col-on-tb-dt.col-6-on-tb-dt.px2.px3-on-tb-dt
           [:div.p2.center.h3.medium.border-bottom.border-dark-silver (str item-count " items in this look")]
           (order-summary/display-line-items line-items products price-strikeout?)
           [:div.mt3
            (add-to-cart-button creating-order? shared-cart)]]))]])))

(defn query [data]
  {:shared-cart                  (get-in data keypaths/shared-cart-current)
   :look                         (->> (get-in data keypaths/ugc-looks)
                                      (query/get {:id (get-in data keypaths/selected-look-id)}))
   :creating-order?              (utils/requesting? data request-keys/create-order-from-shared-cart)
   :products                     (get-in data keypaths/products)
   ;; NOTE: not using current-page? because it would return true for navigate-shop-by-look-details too
   :will-return-to-shop-by-look? (= events/navigate-shop-by-look (get-in data keypaths/prior-navigation-event))
   :price-strikeout?             (experiments/price-strikeout? data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
