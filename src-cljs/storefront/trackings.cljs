(ns storefront.trackings
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.convert :as convert]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.woopra :as woopra]
            [storefront.hooks.stringer :as stringer]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.components.money-formatters :as mf]))

(defn ^:private convert-revenue [{:keys [number total] :as order}]
  {:order-number   number
   :revenue        total
   :products-count (orders/product-quantity order)})

(defmulti perform-track identity)

(defmethod perform-track :default [dispatch event args app-state])

(defn- track-page-view [app-state]
  (let [path (routes/current-path app-state)]
    (riskified/track-page path)
    (woopra/track-page (get-in app-state keypaths/session-id)
                       (get-in app-state keypaths/order-user)
                       path)
    (stringer/track-page path)
    (google-analytics/track-page path)
    (facebook-analytics/track-page path)))

(defmethod perform-track events/app-start [_ event args app-state]
  (track-page-view app-state))

(defmethod perform-track events/navigate [_ event args app-state]
  (let [[nav-event nav-args] (get-in app-state keypaths/navigation-message)]
    (when-not (= [nav-event nav-args] (get-in app-state keypaths/previous-navigation-message))
      (track-page-view app-state))))

(defmethod perform-track events/navigate-categories [_ event args app-state]
  (convert/track-conversion "view-categories"))

(defmethod perform-track events/navigate-category [_ event args app-state]
  (facebook-analytics/track-event "ViewContent")
  (convert/track-conversion "view-category"))

(defmethod perform-track events/control-bundle-option-select [_ event _ app-state]
  (when-let [last-step (bundle-builder/last-step (get-in app-state keypaths/bundle-builder))]
    (google-analytics/track-page (str (routes/current-path app-state)
                                      "/choose_"
                                      (clj->js last-step)))))

(defmethod perform-track events/control-add-to-bag [_ event {:keys [variant quantity] :as args} app-state]
  (facebook-analytics/track-event "AddToCart")
  (google-analytics/track-page (str (routes/current-path app-state) "/add_to_bag")))

(defmethod perform-track events/api-success-add-to-bag [_ _ {:keys [variant quantity] :as args} app-state]
  (when variant
    (woopra/track-add-to-bag {:variant variant
                              :session-id (get-in app-state keypaths/session-id)
                              :quantity quantity
                              :order (get-in app-state keypaths/order)})))

(defmethod perform-track events/control-cart-share-show [_ event args app-state]
  (google-analytics/track-page (str (routes/current-path app-state) "/Share_cart")))

(defmethod perform-track events/control-checkout-cart-submit [_ event args app-state]
  (google-analytics/track-event "orders" "initiate_checkout")
  (facebook-analytics/track-event "InitiateCheckout"))

(defmethod perform-track events/control-checkout-cart-paypal-setup [_ event _ app-state]
  (google-analytics/track-event "orders" "initiate_checkout")
  (facebook-analytics/track-event "InitiateCheckout"))

(defmethod perform-track events/api-success-get-saved-cards [_ event args app-state]
  (google-analytics/set-dimension "dimension2" (count (get-in app-state keypaths/checkout-credit-card-existing-cards))))

(defmethod perform-track events/order-completed [_ event {:keys [total] :as order} app-state]
  (facebook-analytics/track-event "Purchase" {:value (str total) :currency "USD"})
  (convert/track-conversion "place-order")
  (convert/track-revenue (convert-revenue order))
  (google-analytics/track-event "orders" "placed_total" nil (int total))
  (google-analytics/track-event "orders" "placed_total_minus_store_credit" nil (int (orders/non-store-credit-payment-amount order))))

(defmethod perform-track events/api-success-auth [_ event args app-state]
  (stringer/track-identify (get-in app-state keypaths/user))
  (woopra/track-identify {:session-id (get-in app-state keypaths/session-id)
                          :user       (get-in app-state keypaths/user)}))

(defmethod perform-track events/api-success-update-order-update-guest-address [_ event args app-state]
  (stringer/track-identify (:user (get-in app-state keypaths/order)))
  (woopra/track-identify {:session-id (get-in app-state keypaths/session-id)
                          :user       (:user (get-in app-state keypaths/order))}))

;; We have 2 ways to enable a feature: via convert.com, or our own code. Each
;; needs to report to GA, and both do it differently. Convert does everything
;; for us, as part of their `script` tag. Our own code sets up the dimension and
;; sends it to GA by tracking an event.
;;
;; We would like convert to be able to trigger events/enable-feature, because
;; that's what being put in a variation does. However, this code prevents
;; that... events/enable-feature would overwrite the dimension set by convert.
(defmethod perform-track events/convert [_ event {:keys [variation]} app-state]
  (woopra/track-experiment (get-in app-state keypaths/session-id)
                           (get-in app-state keypaths/order-user)
                           variation))

(defmethod perform-track events/enable-feature [_ event {:keys [feature ga-name]} app-state]
  (let [ga-name (or ga-name feature)]
    (google-analytics/set-dimension "dimension1" ga-name)
    (google-analytics/track-event "experiment-joined" ga-name))
  (woopra/track-experiment (get-in app-state keypaths/session-id)
                           (get-in app-state keypaths/order-user)
                           feature))

(defmethod perform-track events/control-email-captured-submit [_ event args app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (woopra/track-user-email-captured
     (get-in app-state keypaths/session-id)
     (get-in app-state keypaths/user)
     (get-in app-state keypaths/captured-email))
    (stringer/track-event "email_capture-capture"
                          {:email (get-in app-state keypaths/captured-email)})))

(defmethod perform-track events/control-checkout-cart-apple-pay [_ event args app-state]
  (convert/track-conversion "apple-pay-checkout"))

(defmethod perform-track events/control-checkout-cart-submit [_ event args app-state]
  (convert/track-conversion "checkout"))

(defmethod perform-track events/control-checkout-cart-paypal-setup [_ event args app-state]
  (convert/track-conversion "paypal-checkout"))

(defmethod perform-track events/apple-pay-availability [_ event {:keys [available?]} app-state]
  (when available?
    (convert/track-conversion "apple-pay-available")))

(defmethod perform-track events/control-sign-out [_ _ _ _]
  (stringer/track-clear))
