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
            [storefront.accessors.experiments :as experiments]
            [cemerick.url :as url]))

(defn image-thumbnail [photo]
  [:img.col-12.block {:src photo}])

(defn buy-look-button [requesting? selected-look-id {:keys [id links]} view-look?]
  (let [{:keys [purchase view-look view-named-search]} links]
    (ui/teal-button
     (merge
      {:spinning? (and (= id selected-look-id) requesting?)}
      (if requesting?
        {:on-click utils/noop-callback}
        (if view-named-search
          (apply utils/route-to view-named-search)
          (if view-look?
            (apply utils/route-to view-look)
            (apply utils/fake-href purchase)))))
     (if view-look?
       "View this look"
       "Shop this look"))))

(defn image-attribution [requesting? selected-look-id {:keys [user-handle social-service] :as look} view-look?]
  [:div.bg-light-silver.p1
   [:div.flex.items-center.mt1.mb2.mx3-on-mb.mx1-on-tb-dt
    [:div.flex-auto.f4.gray.bold {:style {:word-break "break-all"}} "@" user-handle]
    [:div.ml1 {:style {:width "15px" :height "15px"}}
     (case social-service
       "instagram" svg/instagram
       "facebook"  svg/facebook-f
       "pinterest" svg/pinterest
       "twitter"   svg/twitter
       nil)]]
   (buy-look-button requesting? selected-look-id look view-look?)])

(defn component [{:keys [looks requesting? selected-look-id view-look?]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-silver.py3
      [:h1.h2.navy "shop by look"]
      [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.gray.col-10.col-6-on-tb-dt.mx-auto "Get inspired by #MayvennMade community. Find your favorite look and click it to easily add it to your bag!"]]
     [:div.container.clearfix.mtn2
      (for [{:keys [id content-type user-handle photo purchase-link source-url social-service] :as look} looks]
        [:div
         {:key id}
         [:div.py2.col-12.col.hide-on-tb-dt {:key (str "small-" id)}
          (image-thumbnail photo)
          (image-attribution requesting? selected-look-id look view-look?)]
         [:div.py2.px2.col.col-4.hide-on-mb {:key (str "large-" id)}
          [:div.relative.hoverable.overflow-hidden
           {:style {:padding-top "100%"}}
           [:div.absolute.top-0 (image-thumbnail photo)]
           [:div.absolute.bottom-0.col-12.show-on-hover (image-attribution requesting? selected-look-id look view-look?)]]]])]])))

(defn query [data]
  {:looks (remove (comp #{"video"} :content-type) (get-in data keypaths/ugc-looks))
   :requesting? (utils/requesting? data request-keys/create-order-from-shared-cart)
   :selected-look-id (get-in data keypaths/selected-look-id)
   :view-look? (experiments/view-look? data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
