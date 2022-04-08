(ns order-history.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.accessors.auth :as auth]
                       [storefront.history :as history]])
            [clojure.string :as string]
            [spice.date :as date]
            [spice.maps :as maps]
            [storefront.accessors.auth :as auth]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.keypaths :as k]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.platform.component-utils :as utils]
            [mayvenn.concept.hard-session :as hard-session]))


(defn order-row
  [{:keys [number placed-at shipping-status total appointment-notice]}]
  [:a.block.flex.inherit-color.bg-white.my2.p2
   (merge {:data-test (str "order-history-row-" number)
           :key       (str "order-history-row-" number)}
          (utils/route-to e/navigate-yourlooks-order-details {:order-number number}))
   [:div.flex-auto
    [:div.flex.justify-between.items-center
     [:div.col-3.proxima.bold.h4 placed-at]
     [:div.col-6.center shipping-status]
     [:div.col-3.right-align total]]
    [:div.content-3 appointment-notice]]
   [:div.pl2.self-center
    (ui/forward-caret {})]])

(defn order-section
  [{:keys [id title orders]}]
  (when (seq orders)
    [:div.mx3.my5
     {:key id}
     [:div.proxima.bold.shout.h6 title]
     (map order-row orders)]))

(c/defcomponent template
  [{:keys [spinning? order-sections order-count]} _ _]
  [:div.py2.max-960.mx-auto.bg-cool-gray.max-580
   [:h1.title-1.canela.m2 "Order History"]
   (if spinning?
     [:div
      {:style {:min-height "400px"}}
      ui/spinner]
     [:div
      (map order-section order-sections)
      (when (->> order-sections (mapcat :orders) count zero?)
        [:div.p4
         [:div.center.mb4.content-2 "You have no recent orders"]
         (ui/button-medium-primary (utils/route-to e/navigate-category {:page/slug "mayvenn-install" :catalog/category-id "23"}) "Browse Products")])])
   (when (> order-count 10)
     [:div.content-4.px4.center.border-top.border-refresh-gray "We are only able to provide the 10 most recent orders from your order history. For questions regarding older orders
please refer to your order confirmation emails or contact customer service: "
      (ui/link :link/phone :a.inherit-color {} config/support-phone-number)])])

(defn order-query [stylist-db {:keys [number
                                      placed-at
                                      appointment
                                      total
                                      fulfillments
                                      servicing-stylist-id]}]
  (let [{:keys [tracking-status
                tracking-number
                method]}      (->> fulfillments
                                   (filter #(= "physical" (:type %)))
                                   (sort-by :fulfilled-at)
                                   last)
        appointment-date      (:appointment-date appointment)
        upcoming-appointment? (date/after? appointment-date (date/now))]
    (merge {:number          number
            :placed-at       (f/slash-date placed-at)
            :shipping-status (cond
                               (seq tracking-status) tracking-status
                               (= "in-store" method) "In Store"
                               ;; If there's a number but no tracking status, we can't track it and assume it was delivered.
                               tracking-number       "Delivered")
            :total           (mf/as-money total)}
           #?(:cljs
              (when (and stylist-db appointment-date)
                (let [appt-pacific-time  (-> (js/Date. appointment-date)
                                             (.toLocaleString "en-US" (clj->js {:timeZone "America/Los_Angeles"}))
                                             f/slash-date)
                      appointment-notice (str (when upcoming-appointment? "Upcoming ")
                                              "Appointment"
                                              (some->> servicing-stylist-id stylist-db :store-nickname (str " with "))
                                              " (" appt-pacific-time ")")]
                  {:appointment-notice appointment-notice}))
              :clj nil)
           {:open? (or upcoming-appointment?
                       (and tracking-status
                            (not (#{"Delivered" "Expired"} tracking-status))))})))

(defn query [app-state]
  (let [orders (->> (get-in app-state k/order-history-orders)
                    (take 10)
                    (mapv (partial order-query (get-in app-state k/order-history-stylists))))]
    {:spinning?      (utils/requesting? app-state request-keys/get-orders)
     :order-count    (get-in app-state k/order-history-count)
     :order-sections [{:id     :recent-orders
                       :title  "Recent Orders"
                       :orders (filter :open? orders)}
                      {:id     :past-orders
                       :title  "Past Orders"
                       :orders (remove :open? orders)}]}))

(defn ^:export page
  [app-state]
  (c/build template (query app-state)))

(defmethod effects/perform-effects e/navigate-yourlooks-order-history
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
                               #(messages/handle-message e/flow--orderhistory--resulted {:orders (:results %)
                                                                                         :count  (:count %)})
                               #(messages/handle-message e/flash-show-failure
                                                         {:message (str "Unable to retrieve orders. Please contact support.")}))
         :clj nil)

      (not (:evt query-params))
      #?(:cljs (history/enqueue-navigate e/navigate-account-email-verification)
         :clj nil))))

(defn save-order-history [orders count app-state]
  (let [old-orders (maps/index-by :number (get-in app-state k/order-history-orders))
        new-orders (maps/index-by :number orders)
        order-history (->> (merge old-orders new-orders)
                           vals
                           (sort-by :placed-at >))]
    (-> app-state
        (update-in k/order-history-count (fn [existing-count]
                                           (or count existing-count)))
        (assoc-in k/order-history-orders order-history)
        (assoc-in [:models :appointment :date] (:appointment (first order-history))))))

(defmethod transitions/transition-state e/flow--orderhistory--resulted
  [_ _ {:keys [orders count]} app-state]
  (save-order-history orders count app-state))

(defmethod effects/perform-effects e/flow--orderhistory--resulted
  [_ _ {:keys [orders]} _ app-state]
  #?(:cljs (when-let [servicing-stylist-ids (not-empty (distinct (remove nil? (map :servicing-stylist-id orders))))]
             (api/fetch-stylists (get-in app-state storefront.keypaths/api-cache)
                                 (string/join "," servicing-stylist-ids)
                                 #(messages/handle-message e/flow--orderhistory--stylists--resulted (:stylists %))))))

(defmethod transitions/transition-state e/flow--orderhistory--stylists--resulted
  [_ _ stylists app-state]
  (assoc-in app-state k/order-history-stylists (maps/index-by :stylist-id stylists)))
