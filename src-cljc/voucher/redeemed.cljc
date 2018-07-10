(ns voucher.redeemed
  (:require #?@(:cljs [[storefront.history :as history]])
   [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.platform.component-utils :as utils]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.effects :as effects]))

(defn ^:private component
  [{:keys [voucher]} owner opts]
  (component/create
   [:div.flex.flex-column.items-center
    [:div.mt4.mb10.h6.flex.items-center.justify-center
     (svg/check {:class  "stroke-black"
                 :height "3em"
                 :width  "3em"})
          "Voucher Redeemed"]
    [:div.h00.teal.bold (case (-> voucher :discount :type)
                          ;; TODO: What to do with percent off?
                          "PERCENT"
                          (str (-> voucher :discount :percent_off) "%")

                          ;; TODO: What to do with percent Unit?
                          "UNIT"
                          (-> voucher :discount :unit_off)

                          "AMOUNT"
                          (-> voucher :discount :amount_off (/ 100) mf/as-money-without-cents)

                          nil)]
    [:div.h4 "has been added to your earnings"]

    [:div.pb4.my8.col-6
     (ui/underline-button (assoc (utils/route-to events/navigate-stylist-dashboard-earnings)
                                 :data-test "view-earnings") "View Earnings")]

    [:a.pt4.my8.medium.h6.border-bottom.border-teal.border-width-2.black (utils/route-to events/navigate-voucher-redeem) "Redeem Another Voucher"]]))

(defn ^:private query [app-state]
  {:voucher (get-in app-state voucher-keypaths/voucher-response)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-voucher-redeemed [_ _ _ _ app-state]
  #?(:cljs
     (when-not (get-in app-state (concat voucher-keypaths/voucher-response [:discount :type]))
       (history/enqueue-redirect events/navigate-home))))
