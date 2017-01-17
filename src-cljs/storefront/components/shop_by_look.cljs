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

(defn image-thumbnail [img]
  [:img.col-12.block img])

(defn buy-look-button [creating-order? selected-look-id {:keys [id links]} view-look?]
  (let [{:keys [purchase-look view-look view-other]} links]
    (ui/teal-button
     (merge
      {:spinning? (and (= id selected-look-id) creating-order?)
       :disabled? creating-order?}
      (if (or view-look purchase-look)
        (if view-look?
          (apply utils/route-to view-look)
          (apply utils/fake-href purchase-look))
        (apply utils/route-to view-other)))
     (if view-look?
       "View this look"
       "Shop this look"))))

(defn image-attribution [creating-order? selected-look-id {:keys [user-handle social-service] :as look} view-look?]
  [:div.bg-light-silver.p1
   [:div.flex.items-center.mt1.mb2.mx3-on-mb.mx1-on-tb-dt
    [:div.flex-auto.h5.gray.bold {:style {:word-break "break-all"}} "@" user-handle]
    [:div.ml1 {:style {:width "15px" :height "15px"}}
     (svg/social-icon social-service)]]
   (buy-look-button creating-order? selected-look-id look view-look?)])

(defn component [{:keys [looks creating-order? selected-look-id view-look?]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-silver.py3
      [:h1.h2.navy "shop by look"]
      [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.gray.col-10.col-6-on-tb-dt.mx-auto "Get inspired by #MayvennMade community. Find your favorite look and click it to easily add it to your bag!"]]
     [:div.container.clearfix.mtn2
      (for [{:keys [id content-type user-handle imgs purchase-link source-url social-service] :as look} looks]
        [:div
         {:key id}
         [:div.py2.col-12.col.hide-on-tb-dt {:key (str "small-" id)}
          (image-thumbnail (:medium imgs))
          (image-attribution creating-order? selected-look-id look view-look?)]
         [:div.py2.px2.col.col-4.hide-on-mb {:key (str "large-" id)}
          [:div.relative.hoverable.overflow-hidden
           {:style {:padding-top "100%"}}
           [:div.absolute.top-0 (image-thumbnail (:medium imgs))]
           [:div.absolute.bottom-0.col-12.show-on-hover (image-attribution creating-order? selected-look-id look view-look?)]]]])]])))

(defn query [data]
  {:looks            (get-in data keypaths/ugc-looks)
   :creating-order?  (utils/requesting? data request-keys/create-order-from-shared-cart)
   :selected-look-id (get-in data keypaths/selected-look-id)
   :view-look?       (experiments/view-look? data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
