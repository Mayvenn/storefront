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
            [cemerick.url :as url]))

(def social-icon {"instagram" svg/instagram
                  "facebook"  svg/facebook-f
                  "pinterest" svg/pinterest
                  "twitter"   svg/twitter})

(defn add-to-button [requesting? {:keys [id purchase-link]}]
  (let [[nav-event nav-args :as nav-message] (-> purchase-link
                                                 url/url-decode
                                                 url/url
                                                 :path
                                                 routes/navigation-message-for)
        is-shared-cart-link? (= nav-event events/navigate-shared-cart)]
    (when (or (not is-shared-cart-link?) id)
      (ui/teal-button
       (if requesting?
         {:on-click utils/noop-callback}
         (if is-shared-cart-link?
           (utils/fake-href events/control-create-order-from-shared-cart (assoc nav-args :selected-look-id id))
           (apply utils/route-to nav-message)))
       "Add items to bag"))))

(defn component [{:keys [requesting? look item-count line-items products]} owner opts]
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
      [:div.col-12
       [:h1.h3.medium.center.dark-gray.mb2 "Get this look"]]
      [:img.block.col-12 {:src (:photo look)}]
      [:div
       [:div.px3.py2.mbp1.bg-light-silver
        [:div.medium.gray.h5.inline-block (str "@" (:user-handle look))]
        [:div.right.inline-block {:style {:width  "20px"
                                          :height "20px"}}
         (social-icon (:social-service look))]]
       [:p.f4.px3.py1.gray.bg-light-silver
        ;;TODO Figure out where this information will be, seems like (:description look)
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus bibendum ligula ex, vel ultrices velit fermentum sit amet. Vivamus condimentum nulla tincidunt ex vulputate sagittis. Mauris vestibulum nisl ac sapien commodo iaculis nec nec elit."]]]
     [:div.col-on-tb-dt.col-6-on-tb-dt.px3.mb3
      [:div.p2
       [:div.p2.center.h3.border-bottom.border-dark-silver (str item-count " items in this look")]
       (order-summary/display-line-items line-items products)]
      [:div (add-to-button requesting? look)]]])))

(defn query [data]
  (let [look-id (get-in data keypaths/selected-look-id)]
    {:look        (->> keypaths/ugc-looks
                       (get-in data)
                       (remove (comp #{"video"} :content-type))
                       (filter #(= (str (:id %)) look-id))
                       (first))
     :requesting? (utils/requesting? data request-keys/create-order-from-shared-cart)
     ;;TODO these must get fetched on load
     :products    (get-in data keypaths/products)
     ;;TODO these must get fetched on load
     :item-count  0
     ;;TODO these must get fetched on load
     :line-items  []}))

(defn built-component [data opts]
  (om/build component (query data) opts))
