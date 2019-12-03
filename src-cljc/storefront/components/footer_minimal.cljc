(ns storefront.components.footer-minimal
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.ui :as ui]))

(defcomponent component
  [{:keys [call-number]} owner opts]
  [:div.border-top.border-gray.bg-white
   [:div.container
    [:div.center.px3.my2
     [:div.my1.medium "Need Help?"]
     [:div.light.h5
      [:span.hide-on-tb-dt
       (ui/link :link/phone :a.inherit-color {} call-number)]
      [:span.hide-on-mb
       (ui/link :link/phone :a.inherit-color {} call-number)]
      " | 8am-5pm PST M-F"]
     [:div.my1.cool-gray.h6
      (component/build footer-links/component {:minimal? true} nil)]]]])

(defn query
  [data]
  {:call-number "+1 (888) 562-7952"})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
