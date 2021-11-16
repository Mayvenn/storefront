(ns yourlooks.order-details.core
  (:require #?@(:cljs [[storefront.api :as api]])
            [clojure.string :as string]
            [spice.core :as spice]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.trackings :as trackings]
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
           shipping-estimate
           placed-at
           trackings]} _ _]
  [:div.py6.px8.max-960.mx-auto
   [:div.title-1.canela "Your Next Look"]
   (titled-content "Order Number" order-number)
   (when placed-at
     (titled-content "Placed On" placed-at))
   (titled-content "Estimated Delivery" (if (seq trackings)
                                          (for [{:keys [url carrier tracking-number]} trackings]
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
                se]; shipping estimate (datetime string)
         }                (get-in app-state k/navigation-query-params)
        order-number      (cond-> (:order-number (last (get-in app-state k/navigation-message)))
                            sn (str "-" sn))
        shipping-estimate (when (seq se)
                            (str (f/day->day-abbr se) ", " #?(:cljs (f/long-date se))))]
    {:order-number      order-number
     :placed-at         (when (seq pa)
                          (str (f/day->day-abbr pa) ", " #?(:cljs (f/long-date pa))))
     :shipping-estimate shipping-estimate
     ;; :shipping-method   rs
     :trackings         [(when-let [url (generate-tracking-url c tn)]
                          {:url             url
                           :carrier         c
                           :tracking-number tn})]}))

;; TODO better name
(defn real-query [app-state]
  (let [order-number                     (:order-number (last (get-in app-state k/navigation-message)))
        {:as order
         :keys [fulfillments placed-at]} (first (filter (fn [o] (= order-number (:number o))) (get-in app-state [:orders])))]
    (when order
      {:order-number      order-number
       :placed-at         (str (f/day->day-abbr placed-at) ", " #?(:cljs (f/long-date placed-at)))
       ;; TODO shipping estimate???
       :trackings          (for [{:keys [carrier tracking-number]} fulfillments]
                             (when-let [url (generate-tracking-url carrier tracking-number)]
                               {:url             url
                                :carrier         carrier
                                :tracking-number tracking-number}))})))

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
     (api/get-order {:number     (:order-number (last (get-in app-state k/navigation-message)))
                     :user-id    (get-in app-state k/user-id)
                     :user-token (get-in app-state k/user-token)}
                    #(messages/handle-message e/api-success-get-orders
                                              {:orders [%]}))))

(defmethod transitions/transition-state e/api-success-get-orders
  [_ _ {:keys [orders]} app-state]
  (assoc-in app-state [:orders] orders)) ;; TODO for gods sake don't leave this here
