(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.routes :as routes]
            [storefront.components.ui :as ui]
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

(defn component [{:keys [looks]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-silver.py3
      [:h1.h2.navy "shop by look"]
      [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.gray.col-10.md-up-col-6.mx-auto "Get inspired by #MayvennMade community. Find your favorite look and click it to easily add it to your bag!"]]
     [:div
      (for [{:keys [id user-handle photo purchase-link]} looks]
        [:div.mb2 {:key id}
         [:div.relative
          [:img.col-12.block {:src photo}]
          [:div.absolute.bottom-0.right-0
           [:div.p1.light-gray.bold.h4 "@" user-handle]]]
         (ui/large-teal-button (merge (purchase-link-behavior purchase-link)
                                      {:class "not-rounded"})
                               "Buy Look")])]])))

(defn query [data]
  {:looks (get-in data keypaths/looks)})

(defn built-component [data opts]
  (om/build component (query data) opts))
