(ns storefront.components.footer-modal
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.components.footer-links :as footer-links]))

(defcomponent component
  [{:keys [call-number]} owner opts]
  [:div.border-top.border-gray.bg-white
   [:div.container
    [:div.center.px3.my2
     [:div.my1.medium "Have Questions?"]
     [:div.light.h5
      [:span.hide-on-tb-dt
       (ui/link :link/phone :a.inherit-color {} call-number)]
      [:span.hide-on-mb
       (ui/link :link/phone :a.inherit-color {} call-number)]
      " | 8am-5pm PST M-F"]]]])

(defn query
  [data]
  {:call-number "+1 (855) 287-6868"})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
