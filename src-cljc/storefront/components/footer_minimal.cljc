(ns storefront.components.footer-minimal
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.ui :as ui]))

(defcomponent component
  [{:keys [call-number]} owner opts]
  [:div.center.px3.py2.bg-cool-gray
   [:div.title-2.proxima.my1.caps "Need Help?"]
   [:div.content-3.proxima
    [:span.hide-on-tb-dt
     (ui/link :link/phone :a.inherit-color {} call-number)]
    [:span.hide-on-mb
     (ui/link :link/phone :a.inherit-color {} call-number)]
    " | 8am-5pm PST M-F"]
   (component/build footer-links/component {:minimal? true} nil)])

(defn query
  [data]
  {:call-number "+1 (888) 562-7952"})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
