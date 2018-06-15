(ns voucher.redeem
  (:require #?@(:cljs [[storefront.accessors.auth :as auth]
                       [storefront.history :as history]])
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [voucher.keypaths :as keypaths]))

(def divider
  [:hr.border-top.border-dark-silver.col-12.m0
   {:style {:border-bottom 0
            :border-left 0
            :border-right 0}}])

(defn ^:private component
  [{:keys [code]} owner opts]
  (component/create
   [:div.pt4.bg-light-silver.center
    [:h3.pt6 "Scan the QR code to redeem a certificate"]
    [:h6 "Your camera will be used as the scanner."]
    [:div.py4 (ui/ucare-img {:width 50} "4bd0f715-fa5a-4d82-9cec-62dc993c5d23")]
    [:div.mx-auto.col-10.col-3-on-tb-dt (ui/teal-button {:on-click     (utils/send-event-callback events/control-voucher-scan)
                                                         :height-class "py2"
                                                         :data-test    "voucher-scan"} "Scan")]

    [:div.mx-auto.col-10.pt10.pb2.flex.items-center.justify-between
     divider
     [:span.h6.px2 "or"]
     divider]

    [:div.p4.col-4-on-tb-dt.center.mx-auto
     [:h3.pb4 "Enter the 8-digit code"]
     (ui/input-group
      {:keypath       keypaths/eight-digit-code
       :wrapper-class "col-8 pl3 flex items-center bg-white circled-item"
       :data-test     "promo-code"
       :focused       true
       :placeholder   "xxxx-xxxx"
       :value         code
       :errors        nil
       :data-ref      "promo-code"}
      {:ui-element ui/teal-button
       :content    "Redeem"
       :args       {:on-click     (utils/send-event-callback events/control-voucher-redeem)
                    :class        "flex justify-center items-center circled-item"
                    :size-class   "col-4"
                    :height-class "py2"
                    :data-test    "voucher-redeem"}})

     [:h6.pt6.line-height-2.dark-gray.center.my2 "Vouchers are sent to Mayvenn customers via text and/or email when they buy 3 or more bundles and use a special promo code."]]]))

(defn ^:private query [data]
  {:code (get-in data [:temp-location])})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-voucher-redeem
  [dispatch event args prev-app-state app-state]
  #?(:cljs
     (when-not (or (auth/stylist? (auth/signed-in app-state))
                   #_(experiments/vouchers? app-state))
       (history/enqueue-redirect events/navigate-home))))
