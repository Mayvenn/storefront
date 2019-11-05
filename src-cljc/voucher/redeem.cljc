(ns voucher.redeem
  (:require #?@(:cljs [[storefront.accessors.auth :as auth]
                       [storefront.history :as history]
                       [storefront.api :as api]
                       [storefront.hooks.exception-handler :as exception-handler]
                       [voucher.components.qr-reader :as qr-reader]
                       [storefront.loader :as loader]
                       ;; we need to load this namespace first for google closure's tree shaking to put this file (from the same module)
                       ;; before the set-load! call at the bottom of this file.
                       [voucher.redeemed :as _]])
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]

            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defn ^:private divider []
  (component/html
   [:hr.border-top.border-silver.col-12.m0
    {:style {:border-bottom 0
             :border-left 0
             :border-right 0}}]))

(defn ^:private inactive-qr-section []
  (component/html
   [:div
    [:h3.pt6 "Scan the QR code to redeem a certificate"]
    [:h6 "Your camera will be used as the scanner."]
    [:div.flex.justify-center.py4 (ui/ucare-img {:width "50"} "4bd0f715-fa5a-4d82-9cec-62dc993c5d23")]
    [:div.mx-auto.col-10.col-3-on-tb-dt.mb4
     (ui/teal-button {:on-click     (utils/send-event-callback events/control-voucher-scan)
                      :height-class "py2"
                      :data-test    "voucher-scan"} "Scan")]]))

(defn ^:private qr-preview-section []
  (component/html
   [:div.col-10.mt3.mx-auto #?(:cljs (component/build qr-reader/component nil nil))]))

(defn ^:private primary-component
  [{:keys [code redeeming-voucher? field-errors scanning?]}]
  [:div
   [:div.hide-on-dt.center

    (if scanning?
      (qr-preview-section)
      (inactive-qr-section))

    [:div.mx-auto.col-10.pb2.flex.items-center.justify-between
     ^:inline (divider)
     [:span.h6.px2 "or"]
     ^:inline (divider)]]
   [:div.p4.col-4-on-tb-dt.center.mx-auto
    [:div.hide-on-mb-tb.py4]
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
                    :width-class  "col-4"
                    :height-class "py2"
                    :spinning?    redeeming-voucher?
                    :data-test    "voucher-redeem"}})]

    [:h6.pt6.line-height-2.dark-gray.center.my2
     "Vouchers are sent to Mayvenn customers via text and/or email when they buy 3 or more bundles and use a special promo code."]]])

(def ^:private missing-service-menu
  [:div
   [:div.col-8.mx-auto.error.bg-error.border.border-error.rounded.light.letter-spacing-1.mt8
    [:div.px2.py1.bg-lighten-5.rounded.center
     "We need a little more information from you before you can use this feature. "
     "Please contact customer service at "
     (ui/link :link/phone :a.medium.error {} "+1 (888) 562-7952")]]
   [:div.mt8.center [:a (utils/route-to events/navigate-home) "Back to Home"]]])

(def ^:private spinner
  [:div.mt8 (ui/large-spinner {:style {:height "6em"}})])

(defcomponent ^:private component
  [{:keys [service-menu-fetching? service-menu-missing?] :as data} owner opts]
  [:div.bg-light-silver
   (cond service-menu-fetching?
         spinner

         service-menu-missing?
         missing-service-menu

         :default
         (primary-component data))])

(defn ^:private query [data]
  (let [service-menu-required? (experiments/dashboard-with-vouchers? data)
        service-menu           (get-in data
                                       keypaths/user-stylist-service-menu
                                       ::missing)]
    {:code                   (get-in data voucher-keypaths/eight-digit-code)
     :scanning?              (get-in data voucher-keypaths/scanning?)
     :service-menu-fetching? (and service-menu-required?
                                  (or (utils/requesting? data request-keys/fetch-user-stylist-service-menu)
                                      (nil? service-menu)))
     :service-menu-missing?  (and service-menu-required? (= service-menu ::missing))
     :redeeming-voucher?     (utils/requesting? data request-keys/voucher-redemption)
     :field-errors           (get-in data keypaths/field-errors)}))

;; exported for main module to know how to load this function
(defn ^:export built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-voucher-redeem
  [dispatch event args prev-app-state app-state]
  #?(:cljs (when-not (and (auth/stylist? (auth/signed-in app-state))
                          (experiments/dashboard-with-vouchers? app-state))
             (history/enqueue-redirect events/navigate-home))))

(defmethod transitions/transition-state events/navigate-voucher-redeem
  [dispatch event args app-state]
  (-> app-state
      (assoc-in voucher-keypaths/scanned-code nil)
      (assoc-in voucher-keypaths/eight-digit-code nil)
      (assoc-in voucher-keypaths/scanning? nil)))

(defmethod effects/perform-effects events/control-voucher-redeem
  [dispatch event {:keys [code]} prev-app-state app-state]
  #?(:cljs
     (when-not (utils/requesting? app-state request-keys/voucher-redemption)
       (api/voucher-redemption code (get-in app-state keypaths/user-store-id)))))

(defmethod effects/perform-effects events/control-voucher-qr-redeem
  [dispatch event {:keys [code]} prev-app-state app-state]
  #?(:cljs
     (when (and (not (utils/requesting? app-state request-keys/voucher-redemption))
                (not= (get-in app-state voucher-keypaths/scanned-code)
                      (get-in prev-app-state voucher-keypaths/scanned-code)))
       (api/voucher-redemption code (get-in app-state keypaths/user-store-id)))))

(defn ^:private interpret-redemption-error
  [voucherify-error-code]
  (let [[error-message field-errors] (case voucherify-error-code
                                       "quantity_exceeded"  ["This code has already been used and cannot be used again." nil]
                                       "voucher_expired"    ["This voucher has expired." nil]
                                       "resource_not_found" [(str "This is not a valid code. "
                                                                  "Please try again or contact customer service.") nil]
                                       "voucher_disabled"   [(str "There was an error in processing your code. "
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
    (let [redemption-error (interpret-redemption-error (-> response :response :body :key))]
      #?(:cljs
         (when-not (:error-message redemption-error)
           (try
             (exception-handler/report "voucherify-redemption-api-failure" redemption-error)
             (catch :default e ;; ignore error so we don't double report
               nil))))
      (messages/handle-message events/api-failure-errors redemption-error))))

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

#?(:cljs (loader/set-loaded! :redeem))
