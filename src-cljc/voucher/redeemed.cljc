(ns voucher.redeemed
  (:require #?@(:cljs [[storefront.history :as history]])
            [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.accessors.service-menu :as service-menu]
            [storefront.effects :as effects]
            [storefront.platform.component-utils :as utils]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private component
  [{:keys [spinning? voucher service-menu v2-dashboard?]} owner opts]
  (component/create
   [:div
    (if spinning?
      [:div.mt8
       (ui/large-spinner {:style {:height "6em"}})]
      [:div.flex.flex-column.items-center
       [:div.mt4.mb10.h6.flex.items-center.justify-center
        (svg/check {:class  "stroke-black"
                    :height "3em"
                    :width  "3em"})
        "Voucher Redeemed"]
       [:div.h00.teal.bold {:data-test "redemption-amount"}
        (service-menu/display-voucher-amount service-menu voucher)]
       [:div.h4 "has been added to your earnings"]
       [:div.pb4.my8.col-6
        (let [earnings-page-event (if v2-dashboard?
                                    events/navigate-v2-stylist-dashboard-payments
                                    events/navigate-stylist-dashboard-earnings)]
          (ui/underline-button (assoc (utils/route-to earnings-page-event)
                                      :data-test "view-earnings") "View Earnings"))]
       [:a.pt4.my8.medium.h6.border-bottom.border-teal.border-width-2.black
        (utils/route-to events/navigate-voucher-redeem) "Redeem Another Voucher"]])]))

(defn ^:private query [app-state]
  {:v2-dashboard? (experiments/v2-dashboard? app-state)
   :voucher       (get-in app-state voucher-keypaths/voucher-response)
   :spinning?     (utils/requesting? app-state request-keys/fetch-stylist-service-menu)
   :service-menu  (get-in app-state keypaths/stylist-service-menu)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-voucher-redeemed [_ _ _ _ app-state]
  #?(:cljs
     (when-not (-> (get-in app-state voucher-keypaths/voucher-response) :discount :type)
       (history/enqueue-redirect events/navigate-home))))


