(ns storefront.analytics
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.experiments :as experiments]
            [storefront.hooks.riskified :as riskified]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]))

(defmulti track identity)

(defmethod track :default [dispatch event args app-state])

(defn- track-page-view [app-state]
  (let [path (routes/current-path app-state)]
    (riskified/track-page path)
    (google-analytics/track-page path)
    (experiments/track-event path)))

(defmethod track events/app-start [_ event args app-state]
  (track-page-view app-state))

(defmethod track events/navigate [_ event args app-state]
  (when-not (= [event args] (get-in app-state keypaths/previous-navigation-message))
    (track-page-view app-state)))

(defmethod track events/control-cart-share-show [_ event args app-state]
  (google-analytics/track-page (str (routes/current-path app-state) "/Share_cart")))

(defmethod track events/order-completed [_ event order app-state]
  (when (stylists/own-store? app-state)
    (experiments/set-dimension "stylist-own-store" "stylists"))
  (experiments/track-event "place-order" {:revenue (* 100 (:total order))})
  (google-analytics/track-event "orders" "placed_total" nil (int (:total order)))
  (google-analytics/track-event "orders" "placed_total_minus_store_credit" nil (int (orders/non-store-credit-payment-amount order))))

(defmethod track events/control-add-to-bag [_ event {:keys [product]} app-state]
  (google-analytics/track-page (str (routes/current-path app-state) "/add_to_bag")))

(defmethod track events/api-success-add-to-bag [_ _ args app-state]
  (when (stylists/own-store? app-state)
    (experiments/set-dimension "stylist-own-store" "stylists"))
  (experiments/track-event "add-to-bag"))

(defmethod track events/optimizely [_ event {:keys [variation]} app-state]
  (experiments/activate-universal-analytics)
  (google-analytics/track-event "optimizely-experiment" variation))
