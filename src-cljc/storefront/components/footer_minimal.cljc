(ns storefront.components.footer-minimal
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as events]
            [mayvenn.live-help.core :as live-help]))

(defcomponent component
  [{:keys [call-number] :as query} owner opts]
  [:div.px3.py2.bg-cool-gray
   [:div.flex.justify-between
    [:div.title-2.proxima.my1.shout "Need Help?"]
    (component/build live-help/button-component query nil)]
   [:div.content-3.proxima
    [:span.hide-on-tb-dt
     (ui/link :link/phone :a.inherit-color {} call-number)]
    [:span.hide-on-mb
     (ui/link :link/phone :a.inherit-color {} call-number)]
    " | 8am-5pm PST M-F"]
   [:div.left
    (component/build footer-links/component {:minimal? true} nil)]
   ;; Space for promotion helper
   [:div {:style {:margin-bottom "80px"}}]])

(defn query
  [data]
  (merge {:call-number config/support-phone-number}
         (when (experiments/live-help? data)
           {:live-help-button/cta-label  "Chat with us"
            :live-help-button/cta-target [events/flow|live-help|opened]
            :live-help-button/id         "chat with us"
            :live-help-button/icon       [:svg/chat-bubble-diamonds {:class "fill-p-color mr1"
                                                              :style {:height "14px"
                                                                      :width  "13px"}}]})))

(defn built-component
  [data opts]
  (component/build component (query data) nil))
