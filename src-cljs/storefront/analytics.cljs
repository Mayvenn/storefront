(ns storefront.analytics
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.experiments :as experiments]
            [storefront.hooks.riskified :as riskified]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.accessors.stylists :as stylists]))

(defmulti track identity)

(defmethod track :default [dispatch event args app-state])

(defn- track-page-view [app-state]
  (let [path (routes/current-path app-state)]
    (riskified/track-page path)
    (google-analytics/track-page path)
    (facebook-analytics/track-page path)
    (experiments/track-event path)))

(defmethod track events/app-start [_ event args app-state]
  (track-page-view app-state))

(defmethod track events/navigate [_ event args app-state]
  (let [[nav-event nav-args] (get-in app-state keypaths/navigation-message)]
    (when-not (= [nav-event nav-args] (get-in app-state keypaths/previous-navigation-message))
      (track-page-view app-state))))

(defmethod track events/navigate-category [_ event args app-state]
  (facebook-analytics/track-event "ViewContent"))

(defmethod track events/control-cart-share-show [_ event args app-state]
  (google-analytics/track-page (str (routes/current-path app-state) "/Share_cart")))

(defmethod track events/order-completed [_ event order app-state]
  (when (stylists/own-store? app-state)
    (experiments/set-dimension "stylist-own-store" "stylists"))
  (facebook-analytics/track-event "Purchase" {:value (str (:total order)) :currency "USD"})
  (experiments/track-event "place-order" {:revenue (* 100 (:total order))})
  (google-analytics/track-event "orders" "placed_total" nil (int (:total order)))
  (google-analytics/track-event "orders" "placed_total_minus_store_credit" nil (int (orders/non-store-credit-payment-amount order))))

(defmethod track events/control-add-to-bag [_ event args app-state]
  (facebook-analytics/track-event "AddToCart")
  (google-analytics/track-page (str (routes/current-path app-state) "/add_to_bag")))

(defmethod track events/api-success-add-to-bag [_ _ args app-state]
  (when (stylists/own-store? app-state)
    (experiments/set-dimension "stylist-own-store" "stylists"))
  (experiments/track-event "add-to-bag"))

(defmethod track events/optimizely [_ event {:keys [variation]} app-state]
  (experiments/activate-universal-analytics)
  (google-analytics/track-event "optimizely-experiment" variation))

(defmethod track events/control-checkout-cart-submit [_ event args app-state]
  (facebook-analytics/track-event "InitiateCheckout"))

(defmethod track events/control-checkout-cart-paypal-setup [_ event _ app-state]
  (facebook-analytics/track-event "InitiateCheckout"))

(defmethod track events/control-bundle-option-select [_ event _ app-state]
  (when-let [last-step (bundle-builder/last-step (get-in app-state keypaths/bundle-builder))]
    (google-analytics/track-page (str (routes/current-path app-state)
                                      "/choose_"
                                      (clj->js last-step)))))

(defmethod track events/api-success-get-saved-cards [_ event args app-state]
  (google-analytics/set-dimension "dimension2" (count (get-in app-state keypaths/checkout-credit-card-existing-cards))))
