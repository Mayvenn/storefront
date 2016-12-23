(ns storefront.trackings
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.hooks.facebook-analytics :as facebook-analytics]
            [storefront.hooks.google-analytics :as google-analytics]
            [storefront.hooks.convert :as convert]
            [storefront.hooks.riskified :as riskified]
            [storefront.hooks.stringer :as stringer]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.videos :as videos]
            [storefront.components.money-formatters :as mf]
            [clojure.string :as str]))

(defn ^:private convert-revenue [{:keys [number total] :as order}]
  {:order-number   number
   :revenue        total
   :products-count (orders/product-quantity order)})

(defmulti perform-track identity)

(defmethod perform-track :default [dispatch event args app-state])

(defmethod perform-track events/app-start [_ event args app-state]
  (when (get-in app-state keypaths/user-id)
    (stringer/track-identify (get-in app-state keypaths/user))))

(defmethod perform-track events/navigate [_ event args app-state]
  (when (not (get-in app-state keypaths/redirecting?))
    (let [path (routes/current-path app-state)]
      (riskified/track-page path)
      (stringer/track-page)
      (google-analytics/track-page path)
      (facebook-analytics/track-page path))))

(defmethod perform-track events/navigate-categories [_ event args app-state]
  (convert/track-conversion "view-categories"))

(defmethod perform-track events/navigate-category [_ event args app-state]
  (facebook-analytics/track-event "ViewContent")
  (convert/track-conversion "view-category"))

(defmethod perform-track events/control-bundle-option-select [_ event {:keys [step-name selected-options]} app-state]
  (stringer/track-event "select_bundle_option" {:option_name  step-name
                                                :option_value (step-name selected-options)})
  (when-let [last-step (bundle-builder/last-step (get-in app-state keypaths/bundle-builder))]
    (google-analytics/track-page (str (routes/current-path app-state)
                                      "/choose_"
                                      (clj->js last-step)))))

(defmethod perform-track events/control-add-to-bag [_ event {:keys [variant quantity] :as args} app-state]
  (facebook-analytics/track-event "AddToCart")
  (google-analytics/track-page (str (routes/current-path app-state) "/add_to_bag")))

(defmethod perform-track events/api-success-add-to-bag [_ _ {:keys [variant quantity] :as args} app-state]
  (when variant
    (stringer/track-event "add_to_cart" {:variant_id       (:id variant)
                                         :variant_sku      (:sku variant)
                                         :variant_price    (:price variant)
                                         :variant_name     (:variant-name variant)
                                         :variant_origin   (-> variant :variant_attrs :origin)
                                         :variant_style    (-> variant :variant_attrs :style)
                                         :variant_color    (-> variant :variant_attrs :color)
                                         :variant_length   (-> variant :variant_attrs :length)
                                         :variant_material (-> variant :variant_attrs :material)
                                         :order_number     (get-in app-state keypaths/order-number)
                                         :order_total      (get-in app-state keypaths/order-total)
                                         :quantity         quantity})))

(defmethod perform-track events/api-success-update-order-from-shared-cart [_ _ {:keys [order]} app-state]
  (let [line-items (orders/product-items order)]
    (stringer/track-event "bulk_add_to_cart" {:skus (str/join "," (map :sku line-items))
                                              :variant_ids (str/join "," (map :id line-items))})))

(defmethod perform-track events/control-cart-share-show [_ event args app-state]
  (google-analytics/track-page (str (routes/current-path app-state) "/Share_cart")))

(defmethod perform-track events/api-success-get-saved-cards [_ event args app-state]
  (google-analytics/set-dimension "dimension2" (count (get-in app-state keypaths/checkout-credit-card-existing-cards))))

(defn payment-flow [{:keys [payments]}]
  (or (some #{"apple-pay" "paypal"} (map :payment-type payments))
      "mayvenn"))

(defn stringer-order-completed [{:keys [number total promotion-codes] :as order}]
  (let [items           (orders/product-items order)]
    {:flow                    (payment-flow order)
     :order_number            number
     :order_total             total
     :non_store_credit_amount (orders/non-store-credit-payment-amount order)
     :shipping_method         (:product-name (orders/shipping-item order))
     :skus                    (->> items (map :sku) (str/join ","))
     :variant_ids             (->> items (map :id) (str/join ","))
     :promo_codes             (->> promotion-codes (str/join ","))
     :total_quantity          (orders/product-quantity order)}))

(defmethod perform-track events/order-completed [_ event {:keys [total] :as order} app-state]
  (stringer/track-event "checkout-complete" (stringer-order-completed order))
  (facebook-analytics/track-event "Purchase" {:value (str total) :currency "USD"})
  (convert/track-conversion "place-order")
  (convert/track-revenue (convert-revenue order))
  (google-analytics/track-event "orders" "placed_total" nil (int total))
  (google-analytics/track-event "orders" "placed_total_minus_store_credit" nil (int (orders/non-store-credit-payment-amount order))))

(defmethod perform-track events/api-success-auth [_ event args app-state]
  (stringer/track-identify (get-in app-state keypaths/user)))

(defmethod perform-track events/api-success-auth-sign-in [_ event {:keys [flow] :as args} app-state]
  (if (routes/current-page? (get-in app-state keypaths/navigation-message)
                            events/navigate-checkout-sign-in)
    (stringer/track-event "checkout-sign_in" {:type flow
                                              :order_number (get-in app-state keypaths/order-number)})
    (stringer/track-event "sign_in" {:type flow})))

(defmethod perform-track events/api-success-auth-sign-up [_ event {:keys [flow] :as args} app-state]
  (if (= (first (get-in app-state keypaths/return-navigation-message))
         events/navigate-checkout-address)
    (stringer/track-event "checkout-sign_up" {:type flow
                                              :order_number (get-in app-state keypaths/order-number)})
    (stringer/track-event "sign_up" {:type flow})))

(defmethod perform-track events/api-success-auth-reset-password [_ events {:keys [flow] :as args} app-state]
  (stringer/track-event "reset_password" {:type flow}))

(defmethod perform-track events/api-success-update-order-update-guest-address [_ event args app-state]
  (stringer/track-identify (:user (get-in app-state keypaths/order))))

(defmethod perform-track events/enable-feature [_ event {:keys [feature experiment]} app-state]
  (google-analytics/track-event "experiment_join" feature)
  (stringer/track-event "experiment-joined" {:name experiment
                                             :variation feature}))

(defmethod perform-track events/control-email-captured-submit [_ event args app-state]
  (when (empty? (get-in app-state keypaths/errors))
    (let [captured-email (get-in app-state keypaths/captured-email)]
      (stringer/track-identify {:email captured-email})
      (stringer/track-event "email_capture-capture" {:email captured-email}))))

(defmethod perform-track events/show-email-popup [_ events args app-state]
  (stringer/track-event "email_capture-deploy" {}))

(defmethod perform-track events/control-email-captured-dismiss [_ events args app-state]
  (stringer/track-event "email_capture-dismiss" {}))

(defmethod perform-track events/apple-pay-availability [_ event {:keys [available?]} app-state]
  (when available?
    (convert/track-conversion "apple-pay-available")))

(defn- checkout-initiate [app-state flow]
  (stringer/track-event "checkout-initiate" {:flow flow
                                             :order_number (get-in app-state keypaths/order-number)})
  (google-analytics/track-event "orders" "initiate_checkout")
  (facebook-analytics/track-event "InitiateCheckout"))

(defmethod perform-track events/control-checkout-cart-submit [_ event args app-state]
  (checkout-initiate app-state "mayvenn")
  (convert/track-conversion "checkout"))

(defmethod perform-track events/control-checkout-cart-apple-pay [_ event args app-state]
  (checkout-initiate app-state "apple-pay")
  (convert/track-conversion "apple-pay-checkout"))

(defmethod perform-track events/control-checkout-cart-paypal-setup [_ event args app-state]
  (checkout-initiate app-state "paypal")
  (convert/track-conversion "paypal-checkout"))

(defmethod perform-track events/control-checkout-as-guest-submit [_ events args app-state]
  (stringer/track-event "checkout-continue_as_guest" {:order_number (get-in app-state keypaths/order-number)}))

(defmethod perform-track events/api-success-update-order-update-address [_ events args app-state]
  (stringer/track-event "checkout-address_enter" {:order_number (get-in app-state keypaths/order-number)}))

(defmethod perform-track events/api-success-update-order-update-cart-payments [_ events args app-state]
  (stringer/track-event "checkout-payment_enter" {:order_number (get-in app-state keypaths/order-number)}))

(defmethod perform-track events/api-success-update-order-update-shipping-method [_ events args app-state]
  (stringer/track-event "checkout-shipping_method_change"
                        {:order_number (get-in app-state keypaths/order-number)
                         :shipping_method (get-in app-state keypaths/checkout-selected-shipping-method-name)}))

(defmethod perform-track events/api-success-forgot-password [_ events {email :email} app-state]
  (stringer/track-event "request_reset_password" {:email email}))

(defmethod perform-track events/api-success-update-order-add-promotion-code [_ events {promo-code :promo-code} app-state]
  (stringer/track-event "promo_add" {:order_number (get-in app-state keypaths/order-number)
                                     :promotion_code promo-code}))

(defmethod perform-track events/api-success-update-order-remove-promotion-code [_ events {promo-code :promo-code} app-state]
  (stringer/track-event "promo_remove" {:order_number (get-in app-state keypaths/order-number)
                                        :promotion_code promo-code}))

(defmethod perform-track events/api-failure-errors-invalid-promo-code [_ events {promo-code :promo-code :as args} app-state]
  (stringer/track-event "promo_invalid" {:order_number (get-in app-state keypaths/order-number)
                                         :promotion_code promo-code}))

(defmethod perform-track events/video-played [_ events {:keys [video-id position]} app-state]
  (when-let [content (videos/id->name video-id)]
    (stringer/track-event "video-play" {:content  (name content)
                                        :position (name position)})))

(defmethod perform-track events/sign-out [_ _ _ _]
  (stringer/track-clear))

