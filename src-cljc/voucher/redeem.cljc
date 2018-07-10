(ns voucher.redeem
  (:require #?@(:cljs [[storefront.accessors.auth :as auth]
                       [storefront.history :as history]
                       [storefront.api :as api]
                       [voucher.components.qr-reader :as qr-reader]])
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]))

(def divider
  [:hr.border-top.border-dark-silver.col-12.m0
   {:style {:border-bottom 0
            :border-left 0
            :border-right 0}}])

(defn ^:private component
  [{:keys [code spinning? field-errors scanning?]} owner opts]
  (component/create
   [:div.bg-light-silver
    [:div.hide-on-dt.center
     (if scanning?
       [:div.col-10.mt3.mx-auto #?(:cljs (component/build qr-reader/component nil nil))]

       [:div
        [:h3.pt6 "Scan the QR code to redeem a certificate"]
        [:h6 "Your camera will be used as the scanner."]
        [:div.py4 (ui/ucare-img {:width 50} "4bd0f715-fa5a-4d82-9cec-62dc993c5d23")]
        [:div.mx-auto.col-10.col-3-on-tb-dt
         (ui/teal-button {:on-click     (utils/send-event-callback events/control-voucher-scan)
                          :height-class "py2"
                          :data-test    "voucher-scan"} "Scan")]])

     [:div.mx-auto.col-10.pb2.flex.items-center.justify-between
      divider
      [:span.h6.px2 "or"]
      divider]]

    [:div.p4.col-4-on-tb-dt.center.mx-auto
     [:div.hide-on-mb-tb.py4 ]
     [:h3.pb4 "Enter the 8-digit code"]
     [:form
      {:on-submit (utils/send-event-callback events/control-voucher-redeem {:code code})}
      (ui/input-group
       {:keypath       voucher-keypaths/eight-digit-code
        :wrapper-class "col-8 pl3 bg-white circled-item"
        :data-test     "voucher-code"
        :focused       true
        :placeholder   "xxxxxxxx"
        :value         code
        :errors        (get field-errors ["voucher-code"])
        :data-ref      "voucher-code"}
       {:ui-element ui/teal-button
        :content    "Redeem"
        :args       {:class        "flex justify-center items-center circled-item"
                     :on-click     (utils/send-event-callback events/control-voucher-redeem {:code code})
                     :size-class   "col-4"
                     :height-class "py2"
                     :spinning?    spinning?
                     :data-test    "voucher-redeem"}})]

     [:h6.pt6.line-height-2.dark-gray.center.my2
      "Vouchers are sent to Mayvenn customers via text and/or email when they buy 3 or more bundles and use a special promo code."]]]))

(defn ^:private query [data]
  {:code         (get-in data voucher-keypaths/eight-digit-code)
   :scanning?    (get-in data voucher-keypaths/scanning?)
   :spinning?    (utils/requesting? data request-keys/voucher-redemption)
   :field-errors (get-in data keypaths/field-errors)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-voucher-redeem
  [dispatch event args prev-app-state app-state]
  #?(:cljs
     (when-not (and (auth/stylist? (auth/signed-in app-state))
                    (experiments/vouchers? app-state))
       (history/enqueue-redirect events/navigate-home))))

(defmethod transitions/transition-state events/navigate-voucher-redeem
  [dispatch event args app-state]
  (-> app-state
      (assoc-in voucher-keypaths/scanned-code nil)
      (assoc-in voucher-keypaths/scanning? nil)))

(defmethod effects/perform-effects events/control-voucher-redeem
  [dispatch event {:keys [code]} prev-app-state app-state]
  #?(:cljs
     (when-not (utils/requesting? app-state request-keys/voucher-redemption)
       (api/voucher-redemption code (get-in app-state keypaths/store-stylist-id)))))

(defmethod effects/perform-effects events/control-voucher-qr-redeem
  [dispatch event {:keys [code]} prev-app-state app-state]
  #?(:cljs
     (when (and (not (utils/requesting? app-state request-keys/voucher-redemption))
                (not= (get-in app-state voucher-keypaths/scanned-code)
                      (get-in prev-app-state voucher-keypaths/scanned-code)))
       (api/voucher-redemption code (get-in app-state keypaths/store-stylist-id)))))

(defn ^:private redemption-error [voucherify-error-code]
  (let [[error-message field-errors] (case voucherify-error-code
                                       "quantity_exceeded"  ["This code has already been used and cannot be used again." nil]
                                       "voucher_expired"    ["This voucher has expired." nil]
                                       "resource_not_found" [(str "This is not a valid code. "
                                                                  "Please try again or contact customer service.") nil]
                                       [nil [{:long-message (str "There was an error in processing your code. "
                                                                 "Please try again or contact customer service.")
                                              :path         ["voucher-code"]}]])]
    {:field-errors  field-errors
     :error-code    voucherify-error-code
     :error-message error-message}))

(defmethod effects/perform-effects events/voucherify-api-failure
  [_ _ response _ _]
  (if (>= (:status response) 500)
    (messages/handle-message events/api-failure-bad-server-response response)
    (messages/handle-message events/api-failure-errors (redemption-error (-> response :response :body :key)))))

(defmethod transitions/transition-state events/api-success-voucher-redemption
  [_ event {:keys [date id object result voucher] :as data} app-state]
  (assoc-in app-state voucher-keypaths/voucher-response {:date     date
                                                         :id       id
                                                         :object   object
                                                         :result   result
                                                         :discount (:discount voucher)
                                                         :type     (:type voucher)}))

(defmethod effects/perform-effects events/api-success-voucher-redemption
  [_ _ response _ _]
  #?(:cljs
     (history/enqueue-navigate events/navigate-voucher-redeemed)))

(defmethod transitions/transition-state events/control-voucher-scan
  [_ event _ app-state]
  (assoc-in app-state voucher-keypaths/scanning? true))

(defmethod transitions/transition-state events/control-voucher-qr-redeem
  [_ event {:keys [code]} app-state]
  (assoc-in app-state voucher-keypaths/scanned-code code))

(defmethod transitions/transition-state events/voucher-camera-permission-denied
  [_ event _ app-state]
  (assoc-in app-state voucher-keypaths/scanning? nil))

(defmethod effects/perform-effects events/voucher-camera-permission-denied
  [_ event _ _ app-state]
  (messages/handle-message events/flash-show-failure
                           {:message (str "Unable to use the device's camera, "
                                          "please enter the code manually.")}))
