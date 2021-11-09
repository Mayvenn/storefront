(ns yourlooks.order-details.core
  (:require   [spice.date :as date]
              [storefront.component :as c]
              [storefront.components.formatters :as f]
              [storefront.components.ui :as ui]
              [storefront.config :as config]
              [storefront.keypaths :as k]
              ))

(c/defcomponent template
  [{:keys [order-number
           shipping-estimate
           placed-at
           tracking-url] :as data} _ _]
  [:div
   [:div.title-1.proxima.center "Your Next Look"]
   [:div
    [:div.title-2.shout.proxima "Order number"]
    [:div order-number]]
   (when placed-at
     [:div
      [:div.title-2.shout.proxima "Placed At"]
      [:div placed-at]])
   (when shipping-estimate
     [:div
      [:div.title-2.shout.proxima "Estimated Delivery"]
      [:div shipping-estimate]])
   (when tracking-url
     [:div "Tracking url" tracking-url])
   [:div "If you need to edit or cancel your order, please contact our customer service at "
    (ui/link :link/email :a {} "help@mayvenn.com")
    " or "
    (ui/link :link/phone :a.inherit-color {} config/support-phone-number)
    "."]])

(defn query
  [state]
  (let [{:keys [tu pa se]} (get-in state k/navigation-query-params)
        order-number       (:order-number (last (get-in state k/navigation-message)))
        placed-at          (when pa
                             (str (f/day->day-abbr pa) ", " #?(:cljs (f/long-date pa))))
        shipping-estimate  (when se
                             (str (f/day->day-abbr se) ", " #?(:cljs (f/long-date se))))]
    {:order-number      order-number
     :placed-at         placed-at
     :shipping-estimate shipping-estimate
     :tracking-url      tu}))

(defn ^:export page
  [app-state]
  (c/build template (query app-state)))
