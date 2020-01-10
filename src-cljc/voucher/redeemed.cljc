(ns ^:figwheel-load voucher.redeemed
  (:require #?@(:cljs [[storefront.history :as history]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.accessors.service-menu :as service-menu]
            [storefront.effects :as effects]
            [storefront.platform.component-utils :as utils]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]))

(def unit-type->menu-kw-payout
  {"Free Install (with Closure)" "install-sew-in-closure"
   "Free Install (with 360)"     "install-sew-in-360-frontal"
   "Free Install (with Frontal)" "install-sew-in-frontal"
   "Free Install (Leave Out)"    "install-sew-in-leave-out"
   "Wig Customization"           "wig-customization"})

(def unit-type->menu-kw-advertised
  {"Free Install (with Closure)" "advertised-sew-in-closure"
   "Free Install (with 360)"     "advertised-sew-in-360-frontal"
   "Free Install (with Frontal)" "advertised-sew-in-frontal"
   "Free Install (Leave Out)"    "advertised-sew-in-leave-out"
   "Wig Customization"           "advertised-wig-customization"})

(def unit-type->display-name
  {"Free Install (with Closure)" "Closure Install"
   "Free Install (with 360)"     "360 Frontal Install"
   "Free Install (with Frontal)" "Frontal Install"
   "Free Install (Leave Out)"    "Leave Out Install"
   "Wig Customization"           "Wig Customization"})

(defn fine-print-molecule
  [{:fine-print/keys [id copy]}]
  (when id
    [:div.h8.mt6.line-height-3
     {:key       id
      :data-test id}
     [:span.p-color "*"]
     copy]))

(defn notification-molecule
  [{:notification/keys [id content]}]
  (when id
    [:div.mb2.h6.flex-wrap.flex.items-center.justify-center.border.border-width-2.border-cool-gray.bg-cool-gray.rounded.p-color.p1.medium.col-12
     {:key       id
      :data-test id}
     ^:inline (svg/check {:class  "stroke-p-color"
                          :height "32px"
                          :width  "32px"
                          :style  {:stroke-width "1"}})
     content]))

(def icon-molecule
  [:div.p-color.my2
   (svg/circled-check {:height "87px"
                       :width  "87px"
                       :class  "stroke-p-color"
                       :style  {:stroke-width "0.75"}})])

(defn ^:private informational-molecule
  [{:informational/keys [primary-id primary secondary-id secondary]}]
  (when primary-id
    [:div {:key primary-id}
     [:div.h5.p-color.bold {:data-test primary-id}
      primary]
     (when secondary-id
       (into [:div.h8.center
              {:data-test secondary-id}] secondary))]))

(defn cta-with-secondary-molecule
  [{:cta/keys [id copy target
               secondary-target
               secondary-id
               secondary-copy]}]
  [:div.center
   (ui/button-large-primary (assoc (apply utils/route-to target)
                                   :data-test id
                                   :class "mb3")
                            copy)
   [:a.medium.h6.black
    (merge
     (apply utils/route-to secondary-target)
     {:data-test secondary-id})
    secondary-copy]])

(defcomponent spinner-molecule [_ _ _]
  [:div.mt8
   (ui/large-spinner {:style {:height "6em"}})])

(defcomponent ^:private component
  [queried-data owner opts]
  [:div.flex.flex-column.items-center.m4
   (notification-molecule queried-data)

   icon-molecule

   (informational-molecule queried-data)

   [:div.mt6
    (cta-with-secondary-molecule queried-data)]

   (fine-print-molecule queried-data)])

(defn ^:private query [app-state]
  (let [voucher                   (get-in app-state voucher-keypaths/voucher-response)
        service-menu              (get-in app-state keypaths/user-stylist-service-menu)
        install-type              (-> voucher :discount :unit_type)
        payout-amount             (-> service-menu
                                      (get (keyword (get unit-type->menu-kw-payout install-type)))
                                      mf/as-money-without-cents)
        advertised-amount         (-> service-menu
                                      (get (keyword (get unit-type->menu-kw-advertised install-type)))
                                      mf/as-money-without-cents)
        install-type-display-name (get unit-type->display-name install-type)
        payout-equals-advertised? (= payout-amount advertised-amount)]
    (cond->
     {:spinning?                (utils/requesting? app-state request-keys/fetch-user-stylist-service-menu)
      :notification/id          (str "voucher-redeemed-" install-type-display-name)
      :notification/content     [:div "Voucher Redeemed:" [:span.pl1.bold install-type-display-name]]
      :cta/id                   "view-earnings"
      :cta/target               [events/navigate-v2-stylist-dashboard-payments]
      :cta/copy                 [:span.bold "View Earnings"]
      :cta/secondary-id         "redeem-voucher"
      :cta/secondary-target     [events/navigate-voucher-redeem]
      :cta/secondary-copy       "Redeem Another Voucher"
      :informational/primary-id "redemption-amount"
      :informational/primary    (str payout-amount " has been added to your earnings")}

      (not payout-equals-advertised?)
      (merge {:fine-print/id              "fine-print"
              :fine-print/copy            (str
                                           "The advertised price is the price that we display publicly to"
                                           " customers and should match your salon’s service prices. Your actual"
                                           " payout amount was set between you and Mayvenn at the start of your"
                                           " program. Charging customers for the difference between the advertised"
                                           " price and your payout amount will result in your removal from the"
                                           " program.")
              :informational/secondary-id "redemption-payout-and-advertised-amounts"
              :informational/secondary    (list
                                           [:div "Your " install-type-display-name " Payout Amount: "
                                            [:span.bold payout-amount]]
                                           [:div install-type-display-name " Advertised Price: "
                                            [:span.bold advertised-amount]
                                            [:span.p-color "*"]])}))))

(defn ^:export built-component
  [data opts]
  (let [queried-data (query data)]
    (if (:spinning? queried-data)
      (component/build spinner-molecule nil nil)
      (component/build component queried-data opts))))

(defmethod effects/perform-effects events/navigate-voucher-redeemed [_ _ _ _ app-state]
  #?(:cljs
     (when-not (-> (get-in app-state voucher-keypaths/voucher-response) :discount :type)
       (history/enqueue-redirect events/navigate-home))))
