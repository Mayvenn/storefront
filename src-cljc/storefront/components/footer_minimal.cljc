(ns storefront.components.footer-minimal
  (:require [storefront.component :as component]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.ui :as ui]))

(defn component
  [{:keys [call-number]} owner opts]
  (component/create
   [:div.border-top.border-gray.bg-white
    [:div.container
     [:div.center.px3.my2
      [:div.my1.medium.dark-gray "Need Help?"]
      [:div.dark-gray.light.h5
       [:span.hide-on-tb-dt
        ^:inline (ui/link :link/phone :a.dark-gray {} call-number)]
       [:span.hide-on-mb
        ^:inline (ui/link :link/phone :a.dark-gray {} call-number)]
       " | 8am-5pm PST M-F"]
      [:div.my1.silver.h6
       ^:inline (footer-links/template {:minimal? true} nil)]]]]))

(defn query
  [data]
  {:call-number "+1 (888) 562-7952"})
