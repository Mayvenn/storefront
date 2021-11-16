(ns yourlooks.order-details.core
  (:require #?@(:cljs [[storefront.api :as api]])
            [api.catalog :as catalog]
            [clojure.string :as string]
            [spice.core :as spice]
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
            [storefront.accessors.experiments :as experiments]))

(defn titled-content [title content]
  [:div.my6
   [:div.title-2.shout.proxima title]
   [:div.content-1.proxima content]])

(c/defcomponent template
  [{:keys [order-number
           placed-at
           fulfillments]} _ _]
  [:div.py6.px8.max-960.mx-auto
   [:div.title-1.canela "Your Next Look"]
   (titled-content "Order Number" order-number)
   (when placed-at
     (titled-content "Placed On" placed-at))
   (titled-content "Estimated Delivery" (if (seq fulfillments)
                                          (for [{:keys [url carrier tracking-number shipping-estimate]} fulfillments]
                                            [:div [:div shipping-estimate]
                                             [:div
                                              carrier
                                              " Tracking: "
                                              [:a
                                               (utils/fake-href e/external-redirect-url {:url url})
                                               tracking-number]]])
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

(defn facade-query
  [app-state]
  (let [{:keys [e  ; event (po=placedOrder, so=shippedOrder, sro=shippedReplacementOrder)
                sn ; shipping number
                c  ; carrier
                tn ; tracking number
                pa ; placed at (epoch ms)
                rs ; requested shipping method
                se] ; shipping estimate (datetime string)
         }                (get-in app-state k/navigation-query-params)
        order-number      (cond-> (:order-number (last (get-in app-state k/navigation-message)))
                            sn (str "-" sn))
        shipping-estimate (when (seq se)
                            (str (f/day->day-abbr se) ", " #?(:cljs (f/long-date se))))]
    {:order-number order-number
     :placed-at    (when (seq pa)
                          (str (f/day->day-abbr pa) ", " #?(:cljs (f/long-date pa))))
     ;; :shipping-method   rs
     :fulfillments    [(when-let [url (generate-tracking-url c tn)]
                      {:url               url
                       :shipping-estimate shipping-estimate
                       :carrier           c
                       :tracking-number   tn})]}))

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

(defn long-date [dt]
  (str (f/day->day-abbr dt) ", " #?(:cljs (f/long-date dt))))

;; TODO better name
(defn real-query [app-state]
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
       :fulfillments    (for [{:keys [carrier
                                   tracking-number
                                   shipment-number]
                            :as   fulfillment} fulfillments
                           :let              [shipment (->> shipments
                                                            (filter (fn [s] (= shipment-number (:number s))))
                                                            first)
                                              drop-shipping? (->> (map :variant-attrs (:line-items shipment))
                                                                  (catalog/select {:warehouse/slug #{"factory-cn"}})
                                                                  boolean)
                                              shipping-sku (->> shipment :line-items first :sku)]
                           :when             (and shipping-sku (= "physical" (:type fulfillment)))]
                       (when-let [url (generate-tracking-url carrier tracking-number)]
                         {:shipping-estimate (-> placed-at
                                                 spice.date/to-datetime
                                                 (spice.date/add-delta {:days (->shipping-days-estimate drop-shipping? shipping-sku placed-at)})
                                                 long-date)
                          :url               url
                          :carrier           carrier
                          :tracking-number   tracking-number}))})))

(defn query [app-state]
  (or (when (experiments/order-details app-state)
        (real-query app-state))
      (facade-query app-state)))

(defn ^:export page
  [app-state]
  (c/build template (query app-state)))

(defmethod effects/perform-effects e/navigate-yourlooks-order-details
  [_ _ _ _ app-state]
  #?(:cljs
     (when-let [user-id (get-in app-state k/user-id)]
       (api/get-order {:number     (:order-number (last (get-in app-state k/navigation-message)))
                       :user-id    user-id
                       :user-token (get-in app-state k/user-token)}
                      #(messages/handle-message e/api-success-get-orders
                                                {:orders [%]})))))

(defmethod transitions/transition-state e/api-success-get-orders
  [_ _ {:keys [orders]} app-state]
  (assoc-in app-state k/order-history orders)) ;; TODO for gods sake don't leave this here
