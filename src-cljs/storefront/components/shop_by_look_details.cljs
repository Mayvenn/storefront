(ns storefront.components.shop-by-look-details
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.assets :as assets]
            [storefront.events :as events]
            [storefront.routes :as routes]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.components.order-summary :as order-summary]
            [cemerick.url :as url]
            [clojure.string :as str]))

(def social-icon {"instagram" svg/instagram
                  "facebook"  svg/facebook-f
                  "pinterest" svg/pinterest
                  "twitter"   svg/twitter})

(defn add-to-cart-button [requesting? {:keys [number]}]
  (ui/teal-button
   (assoc (utils/fake-href events/control-create-order-from-shared-cart {:shared-cart-id number})
          :spinning? requesting?)
   "Add items to bag"))

(defn component [{:keys [requesting? look shared-cart products]} owner opts]
  (om/component
   (html
    [:div
     [:div.col-on-tb-dt.col-6-on-tb-dt.px3.mb3
      [:a.p2.left.col-12.gray
       (utils/route-to events/navigate-shop-by-look)
       [:span
        [:img.px1.mbnp4 {:style {:height "1.25rem"}
                         :src   (assets/path "/images/icons/carat-left.png")}]
        "back to shop by look"]]
      (when look
        [:div
         [:div.col-12
          [:h1.h3.medium.center.dark-gray.mb2 "Get this look"]]
         [:img.block.col-12 {:src (:photo look)}]
         [:div
          [:div.px3.py2.mbp1.bg-light-silver
           [:div.medium.gray.h5.inline-block (str "@" (:user-handle look))]
           [:div.right.inline-block {:style {:width  "20px"
                                             :height "20px"}}
            (social-icon (:social-service look))]]
          (when-not (str/blank? (:title look))
            [:p.f4.px3.py1.gray.bg-light-silver (:title look)])]])]
     (let [line-items (:line-items shared-cart)
           item-count (->> line-items (map :quantity) (reduce +))]
       [:div.col-on-tb-dt.col-6-on-tb-dt.px3.mb3
        [:div.p2
         [:div.p2.center.h3.border-bottom.border-dark-silver (str item-count " items in this look")]
         (order-summary/display-line-items line-items products)]
        [:div (add-to-cart-button requesting? shared-cart)]])])))

(defn query [data]
  (let [shared-cart (get-in data keypaths/shared-cart-current)]
    {:shared-cart shared-cart
     :look        (->> keypaths/ugc-looks
                       (get-in data)
                       (remove (comp #{"video"} :content-type))
                       (filter #(= (str (:shared-cart-id %)) (:number shared-cart)))
                       (first))
     :requesting? (utils/requesting? data request-keys/create-order-from-shared-cart)
     :products    (get-in data keypaths/products)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
