(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.routes :as routes]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [cemerick.url :as url]))

(defn image-thumbnail [photo]
  [:img.col-12.block {:src photo}])

(defn buy-look-button [requesting? selected-look-id {:keys [id purchase-link]}]
  (let [[nav-event nav-args :as nav-message] (-> purchase-link
                                                 url/url-decode
                                                 url/url
                                                 :path
                                                 routes/navigation-message-for)
        is-shared-cart-link? (= nav-event events/navigate-shared-cart)]
    (ui/large-teal-button
     (merge
      {:spinning? (and (= id selected-look-id) requesting?)}
      (if requesting?
        {:on-click utils/noop-callback}
        (if is-shared-cart-link?
          (utils/fake-href events/control-create-order-from-shared-cart (assoc nav-args :selected-look-id id))
          (apply utils/route-to nav-message))))
     "Buy this look")))

(defn image-attribution [requesting? selected-look-id {:keys [user-handle social-service] :as look}]
  [:div.bg-light-silver
   [:div.flex.items-center.py2.mx3
    [:div.flex-auto.gray.bold "@" user-handle]
    [:div {:style {:width "15px" :height "15px"}}
     (case social-service
       "instagram" svg/instagram
       "facebook"  svg/facebook-f
       "pinterest" svg/pinterest
       "twitter"   svg/twitter
       nil)]]
   [:div.p1.fill-gray (buy-look-button requesting? selected-look-id look)]])

(defn component [{:keys [looks requesting? selected-look-id]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-silver.py3
      [:h1.h2.navy "shop by look"]
      [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.gray.col-10.md-up-col-6.mx-auto "Get inspired by #MayvennMade community. Find your favorite look and click it to easily add it to your bag!"]]
     [:div.clearfix.mtn2
      (for [{:keys [id content-type user-handle photo purchase-link source-url social-service] :as look} looks]
        [:div
         {:key id}
         [:div.py2.col-12.col.md-up-hide {:key (str "small-" id)}
          (image-thumbnail photo)
          (image-attribution requesting? selected-look-id look)]
         [:div.py2.px2.col.col-4.to-md-hide {:key (str "large-" id)}
          [:div.relative.hoverable.overflow-hidden
           {:style {:padding-top "100%"}}
           [:div.absolute.top-0 (image-thumbnail photo)]
           [:div.absolute.bottom-0.col-12.show-on-hover (image-attribution requesting? selected-look-id look)]]]])]])))

(defn query [data]
  {:looks (remove (comp #{"video"} :content-type) (get-in data keypaths/ugc-looks))
   :requesting? (utils/requesting? data request-keys/create-order-from-shared-cart)
   :selected-look-id (get-in data keypaths/selected-look-id)})

(defn built-component [data opts]
  (om/build component (query data) opts))
