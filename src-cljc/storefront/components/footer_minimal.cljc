(ns storefront.components.footer-minimal
  (:require [mayvenn.live-help.core :as live-help]
            [storefront.accessors.experiments :as experiments]
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
       [:div.left.mt3 (component/build footer-links/component {:minimal? true} nil)]]]
     [:div (component/build live-help/button-component query)]]]

   [:div.hide-on-tb-dt.py2.px3 ;; mobile
    [:div.flex.justify-between
     [:div.title-2.proxima.my1-on-mb.shout "Need Help?"]
     (component/build live-help/button-component query)]
    [:div.content-3.proxima (ui/link :link/phone :a.inherit-color {} call-number) " | 8am-5pm PST M-F"]
    [:div.left (component/build footer-links/component {:minimal? true} nil)]]
   ;; Space for promotion helper
   [:div {:style {:padding-bottom "100px"}}]])

(defn query
  [data]
  (merge {:call-number config/support-phone-number}
         (when (experiments/live-help? data)
           {:live-help-button/cta-label              "Chat with us"
            :live-help-button/cta-target             [events/flow|live-help|opened {:location "minimal-footer"}]
            :live-help-button/id                     "chat with us"
            :live-help-button/label-and-border-color "#4427c1"
            :live-help-button/icon                   [:svg/chat-bubble-diamonds {:class "fill-p-color mr1"
                                                                                 :style {:height "14px"
                                                                                         :width  "13px"}}]})))

(defn built-component
  [data opts]
  (component/build component (query data) nil))
