(ns storefront.effects
  (:require [ajax.core :refer [-abort]]
            [storefront.accessors.credit-cards :refer [parse-expiration]]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.accessors.taxons :refer [taxon-name-from taxon-path-for]]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.events :as events]
            [storefront.hooks.analytics :as analytics]
            [storefront.hooks.experiments :as experiments]
            [storefront.hooks.opengraph :as opengraph]
            [storefront.hooks.reviews :as reviews]
            [storefront.hooks.riskified :as riskified]
            [storefront.keypaths :as keypaths]
            [storefront.messages :refer [send send-later]]
            [storefront.routes :as routes]
            [storefront.utils.query :as query]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/app-start [_ event args app-state]
  (experiments/insert-optimizely)
  (riskified/insert-beacon (get-in app-state keypaths/session-id))
  (analytics/insert-tracking))

(defmethod perform-effects events/app-stop [_ event args app-state]
  (experiments/remove-optimizely)
  (riskified/remove-beacon)
  (analytics/remove-tracking))

(defmethod perform-effects events/navigate [_ event args app-state]
  (if (experiments/display-variation app-state "bundle-builder")
    (api/get-builder-taxons (get-in app-state keypaths/handle-message)
                            (get-in app-state keypaths/api-cache))
    (api/get-taxons (get-in app-state keypaths/handle-message)
                    (get-in app-state keypaths/api-cache)))
  (api/get-store (get-in app-state keypaths/handle-message)
                 (get-in app-state keypaths/api-cache)
                 (get-in app-state keypaths/store-slug))
  (api/get-sms-number (get-in app-state keypaths/handle-message))
  (api/get-promotions (get-in app-state keypaths/handle-message)
                      (get-in app-state keypaths/api-cache))

  (when-let [order-number (get-in app-state keypaths/order-number)]
    (api/get-order (get-in app-state keypaths/handle-message)
                   order-number
                   (get-in app-state keypaths/order-token)))
  (opengraph/set-site-tags)
  (scroll/scroll-to-top)

  (let [[flash-event flash-args] (get-in app-state keypaths/flash-success-nav)]
    (when-not (or
               (empty? (get-in app-state keypaths/flash-success-nav))
               (= [event (seq args)] [flash-event (seq flash-args)]))
      (send app-state
            events/flash-dismiss-success)))
  (let [[flash-event flash-args] (get-in app-state keypaths/flash-failure-nav)]
    (when-not (or
               (empty? (get-in app-state keypaths/flash-failure-nav))
               (= [event (seq args)] [flash-event (seq flash-args)]))
      (send app-state
            events/flash-dismiss-failure)))

  (riskified/track-page (routes/path-for app-state event args))

  (when-not (contains? #{events/navigate-product
                         events/navigate-category
                         events/navigate-cart
                         events/navigate-checkout-address
                         events/navigate-checkout-delivery
                         events/navigate-checkout-payment
                         events/navigate-checkout-confirmation} event)
    (analytics/track-page (routes/path-for app-state event args))))

(defmethod perform-effects events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (api/get-products (get-in app-state keypaths/handle-message)
                    (get-in app-state keypaths/api-cache)
                    taxon-path
                    (if (experiments/display-variation app-state "bundle-builder")
                      "bundle-builder"
                      "original")
                    (get-in app-state keypaths/user-token)))

(defn bundle-builder-redirect [app-state product]
  (when product
    (routes/enqueue-navigate app-state
                             events/navigate-category
                             {:taxon-path (-> product :product_attrs :style first taxon-path-for)})))

(defmethod perform-effects events/navigate-product [_ event {:keys [product-path]} app-state]
  (if (experiments/display-variation "bundle-builder")
    (bundle-builder-redirect app-state
                             (query/get (get-in data keypaths/browse-product-query)
                                        (vals (get-in data keypaths/products))))
    (do
      (api/get-product (get-in app-state keypaths/handle-message)
                       product-path)
      (reviews/insert-reviews app-state))))

(defmethod perform-effects events/navigate-stylist-manage-account [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-states (get-in app-state keypaths/handle-message)
                    (get-in app-state keypaths/api-cache))
    (api/get-stylist-account (get-in app-state keypaths/handle-message)
                             user-token)))

(defmethod perform-effects events/navigate-stylist-commissions [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-stylist-commissions (get-in app-state keypaths/handle-message)
                                 user-token)))

(defmethod perform-effects events/navigate-stylist-bonus-credit [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-stylist-bonus-credits (get-in app-state keypaths/handle-message)
                                   user-token)))

(defmethod perform-effects events/navigate-stylist-referrals [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-stylist-referral-program (get-in app-state keypaths/handle-message)
                                      user-token)))

(defmethod perform-effects events/navigate-cart [_ event args app-state]
  (analytics/set-checkout-step 1 (get-in app-state [:order :line_items]))
  (analytics/track-page (routes/path-for app-state event)))

(defmethod perform-effects events/navigate-checkout [_ event args app-state]
  (when-not (get-in app-state keypaths/order-number)
    (routes/enqueue-redirect app-state events/navigate-cart)))

(defmethod perform-effects events/navigate-checkout-address [_ event args app-state]
  (analytics/set-checkout-step 2 (get-in app-state [:order :line_items]))
  (analytics/track-page (routes/path-for app-state event))
  (api/get-states (get-in app-state keypaths/handle-message)
                  (get-in app-state keypaths/api-cache)))

(defmethod perform-effects events/navigate-checkout-delivery [_ event args app-state]
  (analytics/set-checkout-step 3 (get-in app-state [:order :line_items]))
  (analytics/track-page (routes/path-for app-state event)))

(defmethod perform-effects events/navigate-checkout-payment [_ event args app-state]
  (analytics/set-checkout-step 4 (get-in app-state [:order :line_items]))
  (analytics/track-page (routes/path-for app-state event))
  (api/get-payment-methods (get-in app-state keypaths/handle-message)
                           (get-in app-state keypaths/api-cache)))

(defmethod perform-effects events/navigate-checkout-confirmation [_ event args app-state]
  (analytics/set-checkout-step 5 (get-in app-state [:order :line_items]))
  (analytics/track-page (routes/path-for app-state event)))

(defmethod perform-effects events/navigate-order [_ event args app-state]
  (api/get-past-order (get-in app-state keypaths/handle-message)
                      (get-in app-state keypaths/past-order-id)
                      (get-in app-state keypaths/user-token)))

(defmethod perform-effects events/navigate-order-complete [_ _ _ app-state]
  (analytics/set-purchase (get-in app-state keypaths/last-order))
  (analytics/track-page (apply routes/path-for app-state (get-in app-state keypaths/navigation-message)))
  (experiments/track-event "place-order"))

(defmethod perform-effects events/navigate-my-orders [_ event args app-state]
  (when-let [user-token (get-in app-state keypaths/user-token)]
    (api/get-my-orders (get-in app-state keypaths/handle-message)
                       user-token)))

(defmethod perform-effects events/navigate-not-found [_ event args app-state]
  (send app-state
        events/flash-show-failure {:message "The page you were looking for could not be found."
                                   :navigation [event args]}))

(defmethod perform-effects events/control-menu-expand
  [_ event {keypath :keypath} app-state]
  (when (#{keypaths/menu-expanded} keypath)
    (set! (.. js/document -body -style -overflow) "hidden")))

(defmethod perform-effects events/control-menu-collapse
  [_ event {keypath :keypath} app-state]
  (when (#{keypaths/menu-expanded} keypath)
    (set! (.. js/document -body -style -overflow) "auto")))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state keypaths/handle-message)
               (get-in app-state keypaths/sign-in-email)
               (get-in app-state keypaths/sign-in-password)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-token)))

(defmethod perform-effects events/control-sign-up-submit [_ event args app-state]
  (api/sign-up (get-in app-state keypaths/handle-message)
               (get-in app-state keypaths/sign-up-email)
               (get-in app-state keypaths/sign-up-password)
               (get-in app-state keypaths/sign-up-password-confirmation)
               (get-in app-state keypaths/store-stylist-id)
               (get-in app-state keypaths/order-token)))

(defn- abort-pending-requests [requests]
  (doseq [{xhr :xhr} requests] (when xhr (-abort xhr))))

(defmethod perform-effects events/control-sign-out [_ event args app-state]
  (cookie-jar/clear (get-in app-state keypaths/cookie))
  (send app-state
        events/flash-show-success {:message "Logged out successfully"
                                   :navigation [events/navigate-home {}]})
  (abort-pending-requests (get-in app-state keypaths/api-requests))
  (routes/enqueue-navigate app-state events/navigate-home))

(defmethod perform-effects events/control-browse-add-to-bag [_ event _ app-state]
  (let [product (query/get (get-in app-state keypaths/browse-product-query)
                           (vals (get-in app-state keypaths/products)))
        variant (if (experiments/display-variation app-state "bundle-builder")
                  (products/selected-variant app-state)
                  (query/get (get-in app-state keypaths/browse-variant-query)
                             (products/all-variants product)))]
    (api/add-to-bag (get-in app-state keypaths/handle-message)
                    variant
                    product
                    (get-in app-state keypaths/browse-variant-quantity)
                    (get-in app-state keypaths/store-stylist-id)
                    (get-in app-state keypaths/order-token)
                    (get-in app-state keypaths/order-number)
                    (get-in app-state keypaths/user-token))))

(defmethod perform-effects events/control-forgot-password-submit [_ event args app-state]
  (api/forgot-password (get-in app-state keypaths/handle-message)
                       (get-in app-state keypaths/forgot-password-email)))

(defmethod perform-effects events/control-reset-password-submit [_ event args app-state]
  (if (empty? (get-in app-state keypaths/reset-password-password))
    (send app-state
          events/flash-show-failure {:message "Your password cannot be blank."
                                     :navigation (get-in app-state keypaths/navigation-message)})
    (api/reset-password (get-in app-state keypaths/handle-message)
                        (get-in app-state keypaths/reset-password-password)
                        (get-in app-state keypaths/reset-password-password-confirmation)
                        (get-in app-state keypaths/reset-password-token))))

(defn save-cookie [app-state remember?]
  (cookie-jar/save-order (get-in app-state keypaths/cookie)
                         (get-in app-state keypaths/order)
                         {:remember? remember?})
  (cookie-jar/save-user (get-in app-state keypaths/cookie)
                        (get-in app-state keypaths/user)
                        {:remember? remember?}))

(defmethod perform-effects events/control-manage-account-submit [_ event args app-state]
  (api/update-account (get-in app-state keypaths/handle-message)
                      (get-in app-state keypaths/user-id)
                      (get-in app-state keypaths/manage-account-email)
                      (get-in app-state keypaths/manage-account-password)
                      (get-in app-state keypaths/manage-account-password-confirmation)
                      (get-in app-state keypaths/user-token)))

(defn updated-quantities [line-items quantities]
  (->>
   line-items
   (map #(select-keys % [:id :quantity :variant_id]))
   (map #(assoc % :quantity (-> % :id quantities)))))

(defmethod perform-effects events/control-click-category-product [_ _ {:keys [target taxon]} app-state]
  (analytics/add-product target)
  (analytics/set-action "click" :list (:name taxon))
  (analytics/track-event "UX" "click" "Results")
  (routes/enqueue-navigate app-state events/navigate-product {:product-path (:slug target)
                                                              :query-params {:taxon-id (taxon :id)}}))

(defn- modify-cart [app-state args f]
  (let [order (get-in app-state keypaths/order)
        coupon-code (get-in app-state keypaths/cart-coupon-code)]
    (f
     (get-in app-state keypaths/handle-message)
     (get-in app-state keypaths/user-token)
     order
     args)))

(defmethod perform-effects events/control-cart-line-item-inc [_ event {:keys [path]} app-state]
  (modify-cart app-state {:line-item-id (last path)} api/inc-line-item))

(defmethod perform-effects events/control-cart-line-item-dec [_ event {:keys [path]} app-state]
  (modify-cart app-state {:line-item-id (last path)} api/dec-line-item))

(defmethod perform-effects events/control-cart-remove [_ event args app-state]
  (modify-cart app-state {:line-item-id (:id args)} api/delete-line-item))

(defmethod perform-effects events/control-cart-update-coupon [_ event args app-state]
  (modify-cart app-state {:coupon_code (get-in app-state keypaths/cart-coupon-code)} api/update-coupon))

(defmethod perform-effects events/control-checkout-cart-submit [_ event _ app-state]
  (modify-cart app-state
               {:email (get-in app-state keypaths/user-email)
                :user_id (get-in app-state keypaths/user-id)}
               api/checkout-cart-submit))

(defmethod perform-effects events/control-stylist-profile-picture [_ events args app-state]
  (let [handle-message (get-in app-state keypaths/handle-message)
        user-token (get-in app-state keypaths/user-token)
        profile-picture (:file args)]
    (api/update-stylist-account-profile-picture handle-message user-token profile-picture)))

(defmethod perform-effects events/control-stylist-manage-account-submit [_ events args app-state]
  (let [handle-message (get-in app-state keypaths/handle-message)
        user-token (get-in app-state keypaths/user-token)
        stylist-account (get-in app-state keypaths/stylist-manage-account)]
    (api/update-stylist-account handle-message user-token stylist-account)
    (when (stylist-account :profile-picture)
      (api/update-stylist-account-profile-picture handle-message user-token stylist-account))))

(defmethod perform-effects events/control-checkout-update-addresses-submit [_ event args app-state]
  (let [handle-message (get-in app-state keypaths/handle-message)
        token (get-in app-state keypaths/user-token)
        use-billing (get-in app-state keypaths/checkout-shipping-address-use-billing-address)
        save-address (get-in app-state keypaths/checkout-billing-address-save-my-address)
        addresses {:bill_address (get-in app-state keypaths/checkout-billing-address)
                   :ship_address (get-in app-state
                                         (if use-billing
                                           keypaths/checkout-billing-address
                                           keypaths/checkout-shipping-address))}]
    (analytics/track-checkout-option 2 (str (if  save-address "save" "noSave")
                                            "/"
                                            (if use-billing "useBilling" "useDiff")))

    (when save-address
      (api/update-account-address handle-message
                                  (get-in app-state keypaths/user-id)
                                  (get-in app-state keypaths/user-email)
                                  (:bill_address addresses)
                                  (:ship_address addresses)
                                  token))
    (api/update-order handle-message token
                      (merge (get-in app-state keypaths/order)
                             addresses
                             {:state "address"
                              :email (get-in app-state keypaths/user-email)})
                      {:navigate [events/navigate-checkout-delivery]})))

(defmethod perform-effects events/control-checkout-shipping-method-submit [_ event args app-state]
  (let [shipping-method (get-in app-state keypaths/checkout-selected-shipping-method)]
    (analytics/track-checkout-option 3 (:name shipping-method))
    (api/update-order (get-in app-state keypaths/handle-message)
                      (get-in app-state keypaths/user-token)
                      (let [order (get-in app-state keypaths/order)]
                        (merge (select-keys order [:id :number :guest-token])
                               {:state "delivery"
                                :shipments_attributes
                                {:id (get-in order [:shipments 0 :id])
                                 :selected_shipping_rate_id (:id shipping-method)}}))
                      {:navigate [events/navigate-checkout-payment]})))

(defmethod perform-effects events/control-checkout-payment-method-submit [_ event args app-state]
  (let [use-store-credit (get-in app-state keypaths/checkout-use-store-credits)
        covered-by-store-credit (get-in app-state keypaths/order-covered-by-store-credit)]
    (analytics/track-checkout-option 4 (str (if use-store-credit "creditYes" "creditNo")
                                            "/"
                                            (if covered-by-store-credit "creditCovers" "partiatCovers")))
    (api/update-order (get-in app-state keypaths/handle-message)
                      (get-in app-state keypaths/user-token)
                      (let [order (get-in app-state keypaths/order)]
                        (merge (select-keys order [:id :number :guest-token])
                               {:state "payment"
                                :use-store-credits use-store-credit}
                               (if (and use-store-credit covered-by-store-credit)
                                 {:payments_attributes
                                  [{:payment_method_id (or (get-in order [:payment_methods 0 :id])
                                                           (get-in app-state (into keypaths/payment-methods [0 :id])))}]}

                                 {:payments_attributes
                                  [{:payment_method_id (or (get-in order [:payment_methods 0 :id])
                                                           (get-in app-state (into keypaths/payment-methods [0 :id])))
                                    :source_attributes
                                    {:number (get-in app-state keypaths/checkout-credit-card-number)
                                     :expiry (get-in app-state keypaths/checkout-credit-card-expiration)
                                     :verification_value (get-in app-state keypaths/checkout-credit-card-ccv)
                                     :name (get-in app-state keypaths/checkout-credit-card-name)}}]})))
                      {:navigate [events/navigate-checkout-confirmation]})))

(defmethod perform-effects events/control-checkout-confirmation-submit [_ event args app-state]
  (api/update-order (get-in app-state keypaths/handle-message)
                    (get-in app-state keypaths/user-token)
                    (merge (select-keys (get-in app-state keypaths/order) [:id :number :guest-token])
                           {:session_id (get-in app-state keypaths/session-id)})
                    {:navigate [events/navigate-order-complete {:order-id (get-in app-state keypaths/order-number)}]}))

(defmethod perform-effects events/api-success-sign-in [_ event args app-state]
  (save-cookie app-state (get-in app-state keypaths/sign-in-remember))
  (let [nav-message (get-in app-state keypaths/navigation-message)
        nav-event (first nav-message)]
    (if (= nav-event events/navigate-sign-in)
      (routes/enqueue-navigate app-state events/navigate-home)
      (apply send app-state nav-message)))
  (send app-state
        events/flash-show-success {:message "Logged in successfully"
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (send app-state
        events/flash-show-success {:message "Welcome! You have signed up successfully."
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-forgot-password [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home)
  (send app-state
        events/flash-show-success {:message "You will receive an email with instructions on how to reset your password in a few minutes."
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-reset-password [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (send app-state
        events/flash-show-success {:message "Your password was changed successfully. You are now signed in."
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-manage-account [_ event args app-state]
  (save-cookie app-state true)
  (routes/enqueue-navigate app-state events/navigate-home)
  (send app-state
        events/flash-show-success {:message "Account updated"
                                   :navigation [events/navigate-home {}]}))

(defmethod perform-effects events/api-success-stylist-manage-account [_ event args app-state]
  (save-cookie app-state true)
  (when (:updated args)
    (send app-state
          events/flash-show-success {:message "Account updated"
                                     :navigation [events/navigate-stylist-manage-account {}]})
    (send app-state
          events/flash-dismiss-failure)))

(defmethod perform-effects events/api-success-products [_ event {:keys [products]} app-state]
  (let [taxon (query/get (get-in app-state keypaths/browse-taxon-query)
                         (get-in app-state keypaths/taxons))]
    (doseq [product products]
      (analytics/add-impression product {:list (:name taxon)})))
  (analytics/track-page (routes/path-for app-state
                                         (get-in app-state keypaths/navigation-message))))

(defmethod perform-effects events/api-success-product [_ event {:keys [product]} app-state]
  (if (experiments/display-variation app-state "bundle-builder")
    (bundle-builder-redirect app-state product)
    (do
      (analytics/add-product product)
      (analytics/set-action "detail")
      (analytics/track-page (routes/path-for app-state
                                             (get-in app-state keypaths/navigation-message)))
      (opengraph/set-product-tags {:name (:name product)
                                   :image (when-let [image-url (->> product
                                                                    :master
                                                                    :images
                                                                    first
                                                                    :large_url)]
                                            (str "http:" image-url))})
      (send app-state
            events/control-browse-variant-select
            {:variant (if-let [variants (seq (-> product :variants))]
                        (or (->> variants (filter :can_supply?) first) (first variants))
                        (:master product))}))))


(defmethod perform-effects events/api-success-store [_ event order app-state]
  (let [user-id (get-in app-state keypaths/user-id)
        token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (when (and user-id token)
      (api/get-account (get-in app-state keypaths/handle-message) user-id token stylist-id))))

(defmethod perform-effects events/api-success-get-order [_ event order app-state]
  (if (orders/incomplete? order)
    (if (= (order :number) (get-in app-state keypaths/order-number))
      (save-cookie app-state true)
      (cookie-jar/clear-order (get-in app-state keypaths/cookie)))
    (cookie-jar/clear-order (get-in app-state keypaths/cookie))))

(defmethod perform-effects events/api-success-cart-update-checkout [_ _ _ app-state]
  (routes/enqueue-navigate app-state events/navigate-checkout-address))

(defmethod perform-effects events/api-success-cart-update-coupon [_ _ _ app-state]
  (send app-state
        events/flash-show-success {:message "The coupon code was successfully applied to your order."
                                   :navigation [events/navigate-cart {}]})
  (send app-state events/flash-dismiss-failure))

(defmethod perform-effects events/api-success-update-order [_ event {:keys [order navigate]} app-state]
  (save-cookie app-state true)
  (when navigate
    (apply routes/enqueue-navigate app-state navigate)))

(defmethod perform-effects events/api-failure-no-network-connectivity [_ event response app-state]
  (send app-state
        events/flash-show-failure
        {:message "Could not connect to the internet. Reload the page and try again."
         :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/api-failure-bad-server-response [_ event response app-state]
  (send app-state
        events/flash-show-failure
        {:message "Uh oh, an error occurred. Reload the page and try again."
         :navigation (get-in app-state keypaths/navigation-message)}))

(defmethod perform-effects events/flash-show [_ event args app-state]
  (scroll/scroll-to-top))

(defmethod perform-effects events/api-failure-validation-errors [_ event validation-errors app-state]
  (send app-state events/flash-dismiss-success)
  (scroll/scroll-to-top)
  (if (seq (:fields validation-errors))
    (send app-state
          events/flash-show-failure
          {:message (:error validation-errors)
           :navigation (get-in app-state keypaths/navigation-message)})
    (send app-state
          events/flash-show-failure {:message (validation-errors :error)
                                     :navigation (get-in app-state keypaths/navigation-message)})))

(defmethod perform-effects events/api-success-add-to-bag [_ _ {:keys [product variant variant-quantity]} app-state]
  (experiments/track-event "add-to-bag")
  (analytics/add-product product {:quantity variant-quantity
                                  :variant (:sku variant)})
  (analytics/set-action "add")
  (analytics/track-event "UX" "click" "add to cart")
  (send-later app-state events/added-to-bag))

(defmethod perform-effects events/added-to-bag [_ _ _ app-state]
  (when-let [el (.querySelector js/document
                                (if (experiments/display-variation app-state "bundle-builder")
                                  "#summary"
                                  ".cart-button"))]
    (scroll/scroll-to-elem el)))

(defmethod perform-effects events/reviews-component-mounted [_ event args app-state]
  (reviews/start))

(defmethod perform-effects events/reviews-component-will-unmount [_ event args app-state]
  (reviews/stop))

(defmethod perform-effects events/optimizely [_ event args app-state]
  (when (= (:variation args) "bundle-builder")
    (apply send app-state (get-in app-state keypaths/navigation-message))))
