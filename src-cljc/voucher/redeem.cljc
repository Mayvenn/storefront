(ns voucher.redeem
  (:require #?@(:cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.components.popup :as popup]
                       [goog.events.EventType :as EventType]])
            [install.certified-stylists :as certified-stylists]
            [install.faq-accordion :as faq-accordion]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.accordion :as accordion]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [voucher.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]

            [storefront.components.free-install-video :as free-install-video]
            [spice.date :as date]
            [storefront.accessors.experiments :as experiments]))
(def divider
  [:hr.border-top.border-dark-silver.col-12.m0
   {:style {:border-bottom 0
            :border-left 0
            :border-right 0}}])

(defn ^:private component
  [{:keys [code time]} owner opts]
  (component/create
   [:div.pt4.bg-light-silver.center
    [:div (str time)]
    [:h3.pt6 "Scan the QR code to redeem a certificate"]
    [:h6 "Your camera will be used as the scanner."]
    [:div.py4 (ui/ucare-img {:width 50} "4bd0f715-fa5a-4d82-9cec-62dc993c5d23")]
    [:div.mx-auto.col-10 (ui/teal-button {:on-click     (utils/send-event-callback events/control-voucher-scan)
                                          :height-class "py2"
                                          :data-test    "voucher-scan"} "Scan")]

    [:div.mx-auto.col-10.pt10.pb2.flex.items-center.justify-between
     divider
     [:span.h6.px2 "or"]
     divider]

    [:div.p4
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
  #_(when-not (experiments/vouchers? app-state)
    (messages/handle-message events/navigate-home)))
