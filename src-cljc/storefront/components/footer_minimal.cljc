(ns storefront.components.footer-minimal
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.ui :as ui]
            
            
            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component
  [{:keys [call-number]} owner opts]
  [:div.border-top.border-gray.bg-white
    [:div.container
     [:div.center.px3.my2
      [:div.my1.medium.dark-gray "Need Help?"]
      [:div.dark-gray.light.h5
       [:span.hide-on-tb-dt
        (ui/link :link/phone :a.dark-gray {} call-number)]
       [:span.hide-on-mb
        (ui/link :link/phone :a.dark-gray {} call-number)]
       " | 8am-5pm PST M-F"]
      [:div.my1.silver.h6
       (component/build footer-links/component {:minimal? true} nil)]]]])

(defn query
  [data]
  {:call-number "+1 (888) 562-7952"})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
