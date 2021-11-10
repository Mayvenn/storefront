(ns yourlooks.order-details.core
  (:require [clojure.string :as string]
            [spice.core :as spice]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]))

(defn titled-content [title content]
  [:div.my2
   [:div.title-2.shout.proxima title]
   [:div content]])

(c/defcomponent template
  [{:keys [order-number
           shipping-estimate
           shipping-method
           placed-at
           tracking] :as data} _ _]
  [:div.p3.max-960.mx-auto
   [:div.title-1.proxima.center "Your Next Look"]
   (titled-content "Order number" order-number)
   (when placed-at
     (titled-content "Placed At" placed-at))
   (if shipping-estimate
     (titled-content "Estimated Delivery" shipping-estimate)
     (titled-content "Shipping Method" shipping-method))
   (titled-content "Tracking" (if-let [{:keys [url carrier tracking-number]} tracking]
                                [:a
                                 (utils/fake-href e/external-redirect-url {:url url})
                                 carrier " " tracking-number]
                                "Pending"))
   [:p "If you need to edit or cancel your order, please contact our customer service at "
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

(defn query
  [state]
  (let [{:keys [e  ; event (po=placedOrder, so=shippedOrder, sro=shippedReplacementOrder)
                sn ; shipping number
                c  ; carrier
                tn ; tracking number
                pa ; placed at (epoch ms)
                rs ; requested shipping method
                se]; shipping estimate (datetime string)
         }                (get-in state k/navigation-query-params)
        order-number      (cond-> (:order-number (last (get-in state k/navigation-message)))
                            sn (str "-" sn))
        shipping-estimate (when se
                            (str (f/day->day-abbr se) ", " #?(:cljs (f/long-date se))))]
    {:order-number      order-number
     :placed-at         (when-let [pa (spice/parse-int pa)]
                          (str (f/day->day-abbr pa) ", " #?(:cljs (f/long-date pa))))
     :shipping-estimate shipping-estimate
     :shipping-method   rs
     :tracking          (when-let [url (generate-tracking-url c tn)]
                          {:url             url
                           :carrier         c
                           :tracking-number tn})}))

(defn ^:export page
  [app-state]
  (c/build template (query app-state)))
