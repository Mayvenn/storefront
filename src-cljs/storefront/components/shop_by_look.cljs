(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.routes :as routes]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]
            [cemerick.url :as url]))

(defn ^:private purchase-link-behavior [purchase-link]
  (let [[nav-event nav-args :as nav-message] (-> purchase-link
                                                 url/url-decode
                                                 url/url
                                                 :path
                                                 routes/navigation-message-for)]
    (if (= nav-event events/navigate-shared-cart)
      (utils/fake-href events/control-create-order-from-shared-cart nav-args)
      (apply utils/route-to nav-message))))

(defn image-thumbnail [photo]
  [:div.overflow-hidden
   [:img.col-12.block {:src photo}]])

(defn buy-look-button [purchase-link]
  (ui/large-teal-button (purchase-link-behavior purchase-link) "Buy Look"))

(defn image-attribution [user-handle purchase-link social-service]
  [:div
   [:div.flex.items-center.py2.mx3
    [:div.flex-auto.gray.bold "@" user-handle]
    [:div.fill-gray.stroke-gray {:style {:width "15px" :height "15px"}}
     (case social-service
       "instagram" svg/instagram
       "facebook"  svg/facebook-f
       "pinterest" svg/pinterest
       "twitter"   svg/twitter
       nil)]]
   [:div.px1.fill-gray (buy-look-button purchase-link)]])

(defn component [{:keys [looks]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-silver.py3
      [:h1.h2.navy "shop by look"]
      [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.gray.col-10.md-up-col-6.mx-auto "Get inspired by #MayvennMade community. Find your favorite look and click it to easily add it to your bag!"]]
     [:div.clearfix.mtn2
      (for [{:keys [id user-handle photo purchase-link social-service]} looks]
        [:div
         {:key id}
         [:div.py2.col-12.col.md-up-col-3.md-up-hide {:key (str "small-" id)}
          (image-thumbnail photo)
          (image-attribution user-handle purchase-link social-service)]
         [:div.py2.px2.col-12.col.md-up-col-3.to-md-hide {:key (str "large-" id)}
          (image-thumbnail photo)
          (buy-look-button purchase-link)]])]])))

(defn query [data]
  {:looks (get-in data keypaths/ugc-looks)})

(defn built-component [data opts]
  (om/build component (query data) opts))
