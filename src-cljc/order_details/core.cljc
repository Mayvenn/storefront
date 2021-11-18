(ns order-details.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]])
            [api.catalog :as catalog]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [spice.date]
            [storefront.component :as c]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.formatters :as f]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.experiments :as experiments]
            [mayvenn.concept.follow :as follow]))

(defn titled-content [title content]
  [:div.my6
   [:div.title-2.shout.proxima title]
   [:div.content-1.proxima content]])

(c/defcomponent template
  [{:keys [order-number
           placed-at
           fulfillments]} _ _]
  [:div.py6.px8.max-960.mx-auto
   [:div.title-1.canela "My Next Look"]
   (titled-content "Order Number" order-number)
   (when placed-at
     (titled-content "Placed On" placed-at))
   (titled-content "Estimated Delivery" (if (seq fulfillments)
                                          (for [{:keys [url carrier tracking-number shipping-estimate]} fulfillments]
                                            [:div [:div shipping-estimate]
                                             (if url
                                               [:div
                                                carrier
                                                " Tracking: "
                                                [:a
                                                 (utils/fake-href e/external-redirect-url {:url url})
                                                 tracking-number]]
                                               "Tracking: Waiting for Shipment")])
                                          "Tracking: Waiting for Shipment"))
   [:p.mt8 "If you need to edit or cancel your order, please contact our customer service at "
    (ui/link :link/email :a {} "help@mayvenn.com")
    " or "
    (ui/link :link/phone :a.inherit-color {} config/support-phone-number)
    "."]])

(defn generate-tracking-url [carrier tracking-number]
  (when tracking-number
    (some-> carrier
            clojure.string/lower-case
            (case "ups"   "https://www.ups.com/track?loc=en_US&tracknum="
                  "fedex" "https://www.fedex.com/fedextrack/?trknbr="
                  "usps"  "https://tools.usps.com/go/TrackConfirmAction?qtc_tLabels1="
                  "dhl"   "https://www.dhl.com/en/express/tracking.html?AWB="
                  nil)
            (str tracking-number))))

;; TODO Switch to using spice.date/date?
(defn date? [value]
  (try
    (some? (spice.date/to-iso value))
    (catch #?(:clj Throwable
              :cljs :default) _
      false)))

(defn long-date [dt]
  (when (date? dt)
    (str (f/day->day-abbr dt) ", " #?(:cljs (f/long-date dt)))))

(defn facade-query
  [app-state]
  (let [{:keys [e  ; event (po=placedOrder, so=shippedOrder, sro=shippedReplacementOrder)
                sn ; shipment number
                c  ; carrier
                tn ; tracking number
                pa ; placed at (epoch ms)
                rs ; requested shipping method
                se] ; shipping estimate (datetime string)
         } (get-in app-state k/navigation-query-params)
        order-number      (cond-> (:order-number (last (get-in app-state k/navigation-message)))
                            sn (str "-" sn))
        shipping-estimate (long-date se)]
    {:order-number order-number
     :placed-at    (long-date pa)
     :fulfillments    (when-let [url (generate-tracking-url c tn)]
                        [{:url               url
                          :shipping-estimate shipping-estimate
                          :carrier           c
                          :tracking-number   tn}])}))

(defn ->shipping-days-estimate [drop-shipping?
                                shipping-sku
                                placed-at]
  (let [{:keys [weekday hour]} #?(:cljs
                                  (->> (.formatToParts
                                        (js/Intl.DateTimeFormat
                                         "en-US" #js
                                                  {:timeZone "America/New_York"
                                                   :weekday  "short"
                                                   :hour     "numeric"
                                                   :hour12   false}) placed-at)
                                       js->clj
                                       (mapv js->clj)
                                       (mapv (fn [{:strs [type value]}]
                                               {(keyword type) value}))
                                       (reduce merge {}))
                                  :clj nil)
        weekday?               (contains? #{"Mon" "Tue" "Wed" "Thu" "Fri"} weekday)
        parsed-hour            (spice/parse-int hour)
        {:keys [saturday-delivery?
                max-delivery]} (checkout-delivery/shipping-method-rules shipping-sku drop-shipping?)
        in-window?             (and weekday?
                                    hour
                                    (< parsed-hour 13)
                                    (or (not (= "Fri" weekday))
                                        (< parsed-hour 10)))]
    (checkout-delivery/number-of-days-to-ship
     weekday
     in-window?
     saturday-delivery?
     max-delivery)))

(defn non-facade-query [app-state]
  (let [order-number        (:order-number (last (get-in app-state k/navigation-message)))
        {:as   order
         :keys [fulfillments
                placed-at
                shipments]} (->> (get-in app-state k/order-history)
                                 (filter (fn [o] (= order-number (:number o))))
                                 first)]
    (when order
      {:order-number order-number
       :placed-at    (long-date placed-at)
       :fulfillments (for [{:keys [carrier
                                   tracking-number
                                   shipment-number]
                            :as   fulfillment} fulfillments
                           :let                [shipment (->> shipments
                                                              (filter (fn [s] (= shipment-number (:number s))))
                                                              first)
                                                shipping-sku (->> shipment :line-items first :sku)]
                           :when               (and shipping-sku (= "physical" (:type fulfillment)))]
                       (let [drop-shipping? (->> (map :variant-attrs (:line-items shipment))
                                                 (catalog/select {:warehouse/slug #{"factory-cn"}})
                                                 boolean)
                             url            (generate-tracking-url carrier tracking-number)]
                         {:shipping-estimate (when (date? placed-at)
                                               (-> placed-at
                                                   spice.date/to-datetime
                                                   (spice.date/add-delta {:days (->shipping-days-estimate drop-shipping? shipping-sku placed-at)})
                                                   long-date))
                          :url               url
                          :carrier           carrier
                          :tracking-number   tracking-number}))})))

(defn query [app-state]
  (or (when (experiments/order-details? app-state)
        (non-facade-query app-state))
      (facade-query app-state)))

(defn ^:export page
  [app-state]
  (c/build template (query app-state)))

(defmethod effects/perform-effects e/navigate-yourlooks-order-details
  [_ _ args _ app-state]
  #?(:cljs
     (when-let [user-id (get-in app-state k/user-id)]
       (api/get-order {:number     (:order-number args)
                       :user-id    user-id
                       :user-token (get-in app-state k/user-token)}
                      {:handler #(messages/handle-message e/api-success-get-orders
                                                          {:orders [%]})
                       ;; Swallow the error rather than displaying it, so a logged-in user
                       ;; doesn't see "no order found" when facade is displayed.
                       ;; We will likely want to change this.
                       :error-handler (constantly nil)}))))

(defmethod transitions/transition-state e/api-success-get-orders
  [_ _ {:keys [results]} app-state]
  (let [old-orders (maps/index-by :number (get-in app-state k/order-history))
        new-orders (maps/index-by :number results)]
    (assoc-in app-state k/order-history (->> (merge old-orders new-orders)
                                          vals
                                          (sort-by :placed-at >)))))

(defmethod effects/perform-effects e/api-success-get-orders
  [_ _ args _ app-state]
  (let [most-recent-open-order (first (get-in app-state k/order-history))]
    (follow/publish-all app-state e/api-success-get-orders {:order-number (:number most-recent-open-order) })))

(defmethod effects/perform-effects e/control-orderdetails-submit
  [_ _ _ _ app-state]
  (let [user-id    (get-in app-state k/user-id)
        user-token (get-in app-state k/user-token)]
    #?(:cljs (api/get-orders {:limit      1
                              :user-id    user-id
                              :user-token user-token}))
    (messages/handle-message e/biz|follow|defined
                             {:follow/after-id e/api-success-get-orders
                              :follow/then     [e/flow--orderdetails--resulted]})))

(defmethod transitions/transition-state e/flow--orderdetails--resulted
  [_ _ _ app-state]
  (follow/clear app-state e/api-success-get-orders))

(defmethod effects/perform-effects e/flow--orderdetails--resulted
  [_ _ args _ app-state]
  #?(:cljs
     (when-let [order-number (-> args :follow/args :order-number)]
       (history/enqueue-redirect e/navigate-yourlooks-order-details {:order-number order-number}))))
