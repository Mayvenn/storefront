(ns order-history.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.accessors.auth :as auth]
                       [storefront.history :as history]])
            [api.catalog :as catalog]
            [catalog.images :as catalog-images]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [spice.date :as date]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.platform.component-utils :as utils]
            [mayvenn.concept.follow :as follow]
            [mayvenn.concept.hard-session :as hard-session]
            [storefront.accessors.auth :as auth]
            [storefront.effects :as storefront.effects]
            [storefront.request-keys :as request-keys]
            [stylist-matching.search.accessors.filters :as stylist-filters]))

(c/defcomponent template
  [{:keys [orders count]} _ _]
  [:div.py2.max-960.mx-auto.bg-white.max-580
   [:h1.title-1.canela.m2 "Order History"]
   (for [{:keys [number placed-at shipping-status total appointment-notice appointment-date open?]} orders]
     [:a.py2.inherit-color.block.border-top.border-refresh-gray.px4.flex
      (merge {:data-test (str "order-details-row-" number)
              :key       (str "order-details-row-" number)}
             (when open? {:class "bold"})
             (utils/route-to e/navigate-yourlooks-order-details {:order-number number}))
      [:div.flex-auto
       [:div.flex.justify-between
        [:div placed-at]
        [:div shipping-status]
        [:div total]]
       [:div.flex.justify-between.content-3.bold
        [:div appointment-notice]
        [:div appointment-date]]]
      [:div.pl1.self-center
       (ui/forward-caret {})]])
   (when (> count 10)
         [:div.content-4.px4.center.border-top.border-refresh-gray "We are only able to provide the 10 most recent orders from your order history. For questions regarding older orders
please refer to your order confirmation emails or contact customer service: "
          (ui/link :link/phone :a.inherit-color {} config/support-phone-number)])])

(defn order-query [order]
  (let [{:keys [number placed-at appointment total]}     order
        {:keys [tracking-status tracking-number method]} (->> order
                                                              :fulfillments
                                                              (filter #(= "physical" (:type %)))
                                                              (sort-by :fulfilled-at)
                                                              last)

        appointment-date      (:appointment-date appointment)
        upcoming-appointment? (date/after? appointment-date (date/now))]
    (merge {:number          number
            ;; TODO: different date format
            :placed-at       (f/slash-date placed-at)
            :shipping-status (cond
                               (seq tracking-status) tracking-status
                               (= "in-store" method) "In Store"
                               tracking-number       "Delivered")
            :total (mf/as-money-without-cents total)}
           #?(:cljs
              (when appointment-date
                (let [appt-pacific-time  (-> (js/Date. appointment-date)
                                             (.toLocaleString "en-US" (clj->js {:timeZone "America/Los_Angeles"}))
                                             f/slash-date)
                      ;; TODO: add stylist if we can
                      appointment-notice (str (when upcoming-appointment? "Upcoming ")
                                              "Appointment")]
                  {:appointment-notice appointment-notice
                   :appointment-date   appt-pacific-time}))
              :clj nil)
           {:open? (or upcoming-appointment?
                       (and tracking-status (not (#{"Delivered" "Expired"} tracking-status))))})))

(defn query [app-state]
  (let [orders (take 10 (get-in app-state k/order-history-orders))]
    {:count  (get-in app-state k/order-history-count)
     :orders (sort :open? (mapv order-query orders))}))

(defn ^:export page
  [app-state]
  (c/build template (query app-state)))

(defmethod effects/perform-effects e/navigate-yourlooks-order-history
  ;; todo: this is almost the same as navigate order details . dry up

  ;; evt = email validation token
  [_ event {:keys [query-params] :as args} _ app-state]
(let [email-verified?        (boolean (get-in app-state k/user-verified-at))
        order-placed-as-guest? (:g query-params)
        sign-in-data            (hard-session/signed-in app-state)]
    (cond
      (and order-placed-as-guest?
           (not (::auth/at-all sign-in-data)))
      (messages/handle-message e/navigate-order-details-sign-up)

      (not (hard-session/allow? sign-in-data event))
      (effects/redirect e/navigate-sign-in)

      email-verified?
      #?(:cljs (api/get-orders {:limit      10
                                :user-id    (get-in app-state k/user-id)
                                :user-token (get-in app-state k/user-token)}
                               #(messages/handle-message e/flow--orderdetails--resulted {:orders (:results %)
                                                                                         :count (:count %)})
                               #(messages/handle-message e/flash-show-failure
                                                         {:message (str "Unable to retrieve orders. Please contact support.")}))
         :clj nil)

      (not (:evt query-params))
      #?(:cljs (history/enqueue-navigate e/navigate-account-email-verification)
         :clj nil))))
