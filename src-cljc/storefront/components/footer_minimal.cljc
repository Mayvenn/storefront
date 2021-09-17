(ns storefront.components.footer-minimal
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as events]))

(defcomponent component
  [{:keys [call-number] :as query} owner opts]
  [:div.bg-cool-gray
   [:div.hide-on-mb.p8 ;; Desktop & Tablet
    [:div.flex.justify-between
     [:div.flex.items-start
      [:div.title-2.proxima.shout "Need Help?"]
      [:div.flex.flex-column.items-start.ml8.ptp3.content-2
       [:div call-number " | 8am-5pm PST M-F"]
       [:div.left.mt3 (component/build footer-links/component {:minimal? true} nil)]]]]]

   [:div.hide-on-tb-dt.py2.px3.flex.flex-column ;; mobile
    [:div.title-2.proxima.shout.mx-auto.mb1 "Need Help?"]
    [:div.content-3.proxima.mx-auto (ui/link :link/phone :a.inherit-color {} call-number) " | 8am-5pm PST M-F"]
    [:div.left (component/build footer-links/component {:minimal? true} nil)]]
   ;; Space for promotion helper
   [:div {:style {:padding-bottom "100px"}}]])

(defn query
  [data]
  (merge {:call-number config/support-phone-number}))

(defn built-component
  [data opts]
  (component/build component (query data) nil))
