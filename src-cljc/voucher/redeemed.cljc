(ns voucher.redeemed
  (:require [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [voucher.keypaths :as voucher-keypaths]))

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

                          "AMOUNT"
                          (-> voucher :discount :amount_off (/ 100) mf/as-money-without-cents))]
    [:div.h4 "has been added to your earnings"]

    [:div.pb4.my8.col-6 (ui/underline-button (utils/route-to events/navigate-stylist-dashboard-earnings) "View Earnings")]

    [:a.pt4.my8.medium.h6.border-bottom.border-teal.border-width-2.black (utils/route-to events/navigate-voucher-redeem) "Redeem Another Voucher"]]))

(defn ^:private query [app-state]
  {:voucher (get-in app-state voucher-keypaths/voucher)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
