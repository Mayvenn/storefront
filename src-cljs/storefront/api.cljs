(ns storefront.api
  (:require [ajax.core :refer [GET json-response-format POST PUT raw-response-format]]
            [clojure.string :as str]
            [storefront.accessors.orders :as orders]
            [storefront.routes :as routes]
            [storefront.cache :as c]
            [storefront.config :refer [api-base-url send-sonar-base-url send-sonar-publishable-key]]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [spice.maps :as maps]
            [clojure.set :as set]))

(defn is-rails-style? [resp]
  (or (seq (:error resp))
      (seq (:errors resp))))

(defn is-rails-exception? [resp]
  (seq (:exception resp)))

(defn convert-to-paths [errors]
  (for [[error-key error-messages] errors
        error-message error-messages]
    {:path (str/split (name error-key) #"\.")
     :long-message error-message}))

(defn rails-style->std-error [{:keys [error errors]}]
  {:error-message (or error "Something went wrong. Please refresh and try again or contact customer service.")
   :error-code (if errors "invalid-input" "generic-error")
   :field-errors (when errors (convert-to-paths errors))})

(defn rails-exception->std-error [resp]
  {:error-message "Something went wrong. Please refresh and try again or contact customer service."
   :error-code "backend-exception"
   :field-errors nil})

(defn waiter-style? [resp]
  (seq (:error-code resp)))

(defn waiter-style->std-error [{:keys [error-message error-code details]}]
  {:error-message error-message
   :error-code error-code
   :field-errors (when details (convert-to-paths details))})

(defn schema-3-style->std-error [resp]
  {:field-errors (-> resp :errors)
   :error-code "invalid-input"
   :error-message "Oops! Please fix the errors below."})

;; New Error Schema form, coerce all stranger ones to this:
;; {:field-errors [{:path ["referrals" 0 "phone"] :long-message "must be 10 digits"} ...]
;;  :error-code "invalid-input" ;; sort of up to the backend
;;  :error-message "There are invalid inputs"}

(defn default-error-handler [response]
  (let [response-body (get-in response [:response :body])]
    (cond
      ;; aborted request
      (#{:aborted} (:failure response))
      (messages/handle-message events/api-abort)

      ;; connectivity
      (zero? (:status response))
      (messages/handle-message events/api-failure-no-network-connectivity response)

      (= (:error-schema response-body) 3)
      (messages/handle-message events/api-failure-errors (schema-3-style->std-error response-body))

      (is-rails-style? response-body)
      (messages/handle-message events/api-failure-errors (rails-style->std-error response-body))

      (is-rails-exception? response-body)
      (messages/handle-message events/api-failure-errors (rails-exception->std-error response-body))

      (waiter-style? response-body)
      (messages/handle-message events/api-failure-errors (waiter-style->std-error response-body))

      :else
      (messages/handle-message events/api-failure-bad-server-response response))))

(defn app-version [xhrio]
  (some-> xhrio (.getResponseHeader "X-App-Version") int))

(defn header-json-response-format [config]
  (let [default (json-response-format config)
        read-json (:read default)]
    (assoc default :read (fn [xhrio]
                           {:body (read-json xhrio)
                            :app-version (app-version xhrio)}))))

(def default-req-opts {:format :json
                       :response-format (header-json-response-format {:keywords? true})})

(defn- wrap-api-end [req-key req-id handler]
  (fn [response]
    (messages/handle-message events/api-end {:request-key req-key :request-id req-id})
    (handler response)))

(defn merge-req-opts [req-key req-id {:keys [handler error-handler] :as request-opts}]
  (merge default-req-opts
         request-opts
         {:handler       (fn [{:keys [body app-version]}]
                           (messages/handle-message events/api-end {:request-key req-key
                                                                    :request-id  req-id
                                                                    :app-version app-version})
                           (handler body))
          :error-handler (fn [res]
                           (messages/handle-message events/api-end {:request-id  req-id
                                                                    :request-key req-key
                                                                    :app-version (-> res :response :app-version)})
                           ((or error-handler default-error-handler) res))}))

(defn api-req
  [method path req-key request-opts]
  (let [request-opts (update-in request-opts [:params] maps/remove-nils)
        req-id (str (random-uuid))
        request
        (method (str api-base-url path)
                (merge-req-opts req-key req-id request-opts))]
    (messages/handle-message events/api-start {:xhr request
                                               :request-key req-key
                                               :request-id req-id})))

(defn cache-req
  [cache method path req-key {:keys [handler params] :as request-opts}]
  (let [key (c/cache-key [path params])
        res (cache key)]
    (if res
      (handler res)
      (api-req method
               path
               req-key
               (merge request-opts
                      {:handler
                       (fn [result]
                         (messages/handle-message events/api-success-cache {key result})
                         (handler result))})))))

(defn get-promotions [cache promo-code]
  (cache-req
   cache
   GET
   "/promotions"
   request-keys/get-promotions
   {:params {:additional-promo-code promo-code}
    :handler #(messages/handle-message events/api-success-promotions %)}))

(defn criteria->query-params [criteria]
  (->> criteria
       (map (fn [[k v]]
              [(if-let [ns (namespace k)]
                 (str ns "/" (name k))
                 (name k))
               v]))
       (into {})))

(defn search-v2-products [cache criteria-or-id handler]
  (cache-req
   cache
   GET
   "/v2/products"
   (conj request-keys/search-v2-products criteria-or-id)
   {:params (if (map? criteria-or-id)
              (criteria->query-params criteria-or-id)
              {:id criteria-or-id})
    :handler handler}))

(defn search-v2-skus [cache criteria-or-id handler]
  (cache-req
   cache
   GET
   "/v2/skus"
   (conj request-keys/search-v2-skus criteria-or-id)
   {:params (criteria->query-params criteria-or-id)
    :handler handler}))

(defn get-saved-cards [user-id user-token]
  (api-req
   GET
   "/saved-cards"
   request-keys/get-saved-cards
   {:params {:user-id    user-id
             :user-token user-token}
    :handler
    #(messages/handle-message events/api-success-get-saved-cards (select-keys % [:cards :default-card]))}))

(defn get-states [cache]
  (cache-req
   cache
   GET
   "/states"
   request-keys/get-states
   {:handler
    #(messages/handle-message events/api-success-states
                              (select-keys % [:states]))}))

(defn select-user-keys [user]
  (select-keys user [:email :token :store_slug :id :is_new_user]))

(defn select-auth-keys [args]
  (-> args
      (update :user select-user-keys)
      (select-keys [:user :order])))

(defn sign-out [session-id browser-id user-id user-token]
  (api-req
   POST
   "/v2/signout"
   request-keys/sign-out
   {:params
    {:session-id session-id
     :browser-id browser-id
     :user-id user-id
     :user-token user-token}
    :handler identity
    :error-handler identity}))

(defn sign-in [session-id browser-id email password stylist-id order-number order-token]
  (api-req
   POST
   "/v2/login"
   request-keys/sign-in
   {:params
    {:session-id   session-id
     :browser-id   browser-id
     :email        (.toLowerCase (str email))
     :password     password
     :stylist-id   stylist-id
     :order-number order-number
     :order-token  order-token}
    :handler
    #(messages/handle-message events/api-success-auth-sign-in
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn facebook-sign-in [session-id browser-id uid access-token stylist-id order-number order-token]
  (api-req
   POST
   "/v2/login/facebook"
   request-keys/facebook-sign-in
   {:params
    {:session-id session-id
     :browser-id browser-id
     :uid uid
     :access-token access-token
     :stylist-id stylist-id
     :order-number order-number
     :order-token order-token}
    :handler
    (fn [response]
      (let [auth-keys (-> (select-auth-keys response)
                          (assoc :flow "facebook"))
            ;; Since we use facebook sign-in for both sign-in and sign-up, we
            ;; need to trigger the appropriate event. Diva tells us when this
            ;; flow has created a new user.
            new-user? (get-in auth-keys [:user :is_new_user])
            success-event (if new-user? events/api-success-auth-sign-up events/api-success-auth-sign-in)]
        (messages/handle-message success-event auth-keys)))}))

(defn sign-up [session-id browser-id email password stylist-id order-number order-token]
  (api-req
   POST
   "/v2/signup"
   request-keys/sign-up
   {:params
    {:session-id session-id
     :browser-id browser-id
     :email email
     :password password
     :stylist-id stylist-id
     :order-number order-number
     :order-token order-token}
    :handler
    #(messages/handle-message events/api-success-auth-sign-up
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn reset-password [session-id browser-id password reset-token order-number order-token stylist-id]
  (api-req
   POST
   "/v2/reset_password"
   request-keys/reset-password
   {:params
    {:session-id session-id
     :browser-id browser-id
     :password password
     :reset_password_token reset-token
     :order-number order-number
     :order-token order-token
     :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-auth-reset-password
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn facebook-reset-password [session-id browser-id uid access-token reset-token order-number order-token stylist-id]
  (api-req
   POST
   "/v2/reset_facebook"
   request-keys/reset-facebook
   {:params
    {:session-id session-id
     :browser-id browser-id
     :uid uid
     :access-token access-token
     :reset-password-token reset-token
     :order-number order-number
     :order-token order-token
     :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-auth-reset-password
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "facebook")))}))

(defn forgot-password [session-id email]
  (api-req
   POST
   "/forgot_password"
   request-keys/forgot-password
   {:params
    {:session-id session-id
     :email (.toLowerCase (str email))}
    :handler
    #(messages/handle-message events/api-success-forgot-password {:email email})}))

(defn diva->mayvenn-address [address]
  (-> address
      (dissoc :country_id)
      (set/rename-keys {:firstname :first-name
                        :lastname :last-name})
      (update-in [:state] :abbr)
      (select-keys [:address1 :address2 :city :first-name :last-name :phone :state :zipcode])))

(defn diva->mayvenn-addresses [contains-addresses]
  (-> contains-addresses
      (set/rename-keys {:bill_address :billing-address
                        :ship_address :shipping-address})
      (update-in [:billing-address] diva->mayvenn-address)
      (update-in [:shipping-address] diva->mayvenn-address)))

(defn get-account [id token]
  (api-req
   GET
   "/users"
   request-keys/get-account
   {:params
    {:id id
     :token token}
    :handler
    #(messages/handle-message events/api-success-account
                              (diva->mayvenn-addresses %))}))

(defn update-account [session-id id email password token]
  (api-req
   PUT
   "/users"
   request-keys/update-account
   {:params (merge  {:session-id session-id
                     :id    id
                     :email email
                     :token token}
                    (when (seq password)
                      {:password password}))
    :handler
    #(messages/handle-message events/api-success-manage-account
                              (select-user-keys %))}))

(defn select-stylist-account-keys [args]
  (-> args
      (select-keys [:birth_date_1i :birth_date_2i :birth_date_3i
                    :birth-date
                    :portrait
                    :chosen_payout_method
                    :venmo_payout_attributes
                    :paypal_payout_attributes
                    :green_dot_payout_attributes
                    :instagram_account
                    :styleseat_account
                    :user
                    :address])
      (assoc :original_payout_method (:chosen_payout_method args))))

(defn get-stylist-account [user-id user-token]
  (api-req
   GET
   "/stylist"
   request-keys/get-stylist-account
   {:params {:user-id    user-id
             :user-token user-token}
    :handler
    #(messages/handle-message events/api-success-stylist-account
                              {:stylist (select-stylist-account-keys %)})}))

(defn get-shipping-methods []
  (api-req
   GET
   "/v2/shipping-methods"
   request-keys/get-shipping-methods
   {:handler #(messages/handle-message events/api-success-shipping-methods
                                       (update-in % [:shipping-methods] reverse))}))

(defn update-stylist-account-profile [session-id user-id user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-profile
   {:params {:session-id session-id
             :user-id    user-id
             :user-token user-token
             :stylist    stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-profile
                              {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-password [session-id user-id user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-password
   {:params {:session-id session-id
             :user-id    user-id
             :user-token user-token
             :stylist    stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-password
                              {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-commission [session-id user-id user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-commission
   {:params {:session-id session-id
             :user-id    user-id
             :user-token user-token
             :stylist    stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-commission
                              {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-social [session-id user-id user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-social
   {:params {:session-id session-id
             :user-id    user-id
             :user-token user-token
             :stylist    stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-social
                              {:stylist (select-stylist-account-keys %)})}))

(defn refresh-stylist-portrait [user-id user-token]
  (api-req
   GET
   "/stylist"
   request-keys/refresh-stylist-portrait
   {:params
    {:user-id    user-id
     :user-token user-token}
    :handler
    #(messages/handle-message events/api-success-stylist-account-portrait
                              {:stylist (select-keys % [:portrait])})}))

(defn update-stylist-account-portrait [session-id user-id user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-portrait
   {:params {:session-id session-id
             :user-id    user-id
             :user-token user-token
             :stylist    stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-portrait
                              {:stylist (select-keys % [:portrait])
                               :updated? true})}))

(defn append-stylist-gallery [user-id user-token {:keys [gallery-urls]}]
  (api-req
   POST
   "/gallery"
   request-keys/append-gallery
   {:params {:user-id    user-id
             :user-token user-token
             :urls       gallery-urls}
    :handler
    #(messages/handle-message events/api-success-gallery %)}))

(defn get-gallery [params]
  (api-req
   GET
   "/gallery"
   request-keys/get-gallery
   {:params
    (select-keys params [:user-id :user-token :stylist-id])
    :handler
    #(messages/handle-message events/api-success-gallery %)}))

(defn delete-gallery-image [user-id user-token image-url]
  (api-req
   POST
   "/gallery/images/delete"
   request-keys/delete-gallery-image
   {:params
    {:user-id    user-id
     :user-token user-token
     :image-url  image-url}
    :handler
    #(messages/handle-message events/api-success-gallery %)}))

(defn get-stylist-stats [user-id user-token]
  (api-req
   GET
   "/stylist/stats"
   request-keys/get-stylist-stats
   {:params
    {:user-id    user-id
     :user-token user-token}
    :handler
    #(messages/handle-message events/api-success-stylist-stats
                              (select-keys % [:previous-payout :next-payout :lifetime-payouts]))}))

(defn get-stylist-commissions [user-id user-token {:keys [page]}]
  (api-req
   GET
   "/v3/stylist/commissions"
   request-keys/get-stylist-commissions
   {:params
    {:user-id user-id :user-token user-token :page page}
    :handler
    #(messages/handle-message events/api-success-stylist-commissions
                              (select-keys % [:rate :commissions :current-page :pages]))}))

(defn get-stylist-commission [user-id user-token {:keys [commission-id]}]
  (api-req
   GET
   (str "/v3/stylist/commission/" commission-id)
   request-keys/get-stylist-commission
   {:params
    {:user-id user-id :user-token user-token}
    :error-handler #(messages/handle-message events/get-stylist-commission-api-failure)
    :handler #(messages/handle-message events/api-success-stylist-commission
                              (select-keys % [:commission]))}))

(defn get-stylist-bonus-credits [user-id user-token {:keys [page]}]
  (api-req
   GET
   "/stylist/bonus-credits"
   request-keys/get-stylist-bonus-credits
   {:params
    {:user-id    user-id
     :user-token user-token
     :page       page}
    :handler
    #(messages/handle-message events/api-success-stylist-bonus-credits
                              (select-keys % [:bonus-amount
                                              :earning-amount
                                              :progress-to-next-bonus
                                              :lifetime-total
                                              :bonuses
                                              :current-page
                                              :pages]))}))

(defn get-stylist-referral-program [user-id user-token {:keys [page]}]
  (api-req
   GET
   "/stylist/referrals"
   request-keys/get-stylist-referral-program
   {:params
    {:user-id    user-id
     :user-token user-token
     :page       page}
    :handler
    #(messages/handle-message events/api-success-stylist-referral-program
                              (select-keys % [:sales-rep-email
                                              :bonus-amount
                                              :earning-amount
                                              :lifetime-total
                                              :referrals
                                              :current-page
                                              :pages]))}))

(defn place-order [session-id order utm-params]
  (api-req
   POST
   "/v2/place-order"
   request-keys/place-order
   {:params (merge (select-keys order [:number :token])
                   {:session-id session-id
                    :utm-params utm-params})
    :handler #(messages/handle-message events/api-success-update-order-place-order
                                       {:order %
                                        :navigate events/navigate-order-complete})}))


(defn ^:private remove-from-bag [request-key session-id {:keys [variant-id number token quantity]} handler]
  (api-req
   POST
   "/v2/remove-from-bag"
   request-key
   {:params {:session-id session-id
             :quantity quantity
             :variant-id variant-id
             :number number
             :token token}
    :handler handler}))

(defn remove-line-item [session-id {:keys [variant-id number token sku-code]} handler]
  (remove-from-bag
   (conj request-keys/update-line-item sku-code)
   session-id {:variant-id variant-id
               :number number
               :token token
               :quantity 1}
   handler))

(defn delete-line-item [session-id order variant-id]
  (remove-from-bag
   (conj request-keys/delete-line-item variant-id)
   session-id
   (merge (select-keys order [:number :token])
          {:session-id session-id
           :variant-id variant-id
           :quantity (->> order
                          orders/product-items
                          (orders/line-item-by-id variant-id)
                          :quantity)})
   #(messages/handle-message events/api-success-remove-from-bag {:order %})))

(defn update-addresses [session-id order]
  (api-req
   POST
   "/v2/update-addresses"
   request-keys/update-addresses
   {:params (-> order
                (select-keys [:number :token :billing-address :shipping-address])
                (assoc :session-id session-id))
    :handler #(messages/handle-message events/api-success-update-order-update-address
                                       {:order %
                                        :navigate events/navigate-checkout-payment})}))

(defn guest-update-addresses [session-id order]
  (api-req
   POST
   "/v2/guest-update-addresses"
   request-keys/update-addresses
   {:params (-> order
                (select-keys [:number :token :email :billing-address :shipping-address])
                (assoc :session-id session-id))
    :handler #(messages/handle-message events/api-success-update-order-update-guest-address
                                       {:order %
                                        :navigate events/navigate-checkout-payment})}))

(defn apple-pay-estimate [params successful-estimate failed-to-estimate]
  (POST
   (str api-base-url "/apple-pay-estimate")
   (merge default-req-opts
          {:params  params
           :handler (comp successful-estimate :body)
           :error-handler failed-to-estimate})))

(defn checkout [params successful-checkout failed-checkout]
  (api-req
   POST
   "/checkout"
   request-keys/checkout
   {:params  params
    :handler (juxt successful-checkout #(messages/handle-message events/api-success-update-order-place-order
                                                                 {:order %
                                                                  :navigate events/navigate-order-complete}))
    :error-handler (juxt failed-checkout default-error-handler)}))

(defn update-shipping-method [session-id order]
  (api-req
   POST
   "/v2/update-shipping-method"
   request-keys/update-shipping-method
   {:params (-> order
                (select-keys [:number :token :shipping-method-sku])
                (assoc :session-id session-id))
    :handler #(messages/handle-message events/api-success-update-order-update-shipping-method
                                       {:order %})}))

(defn update-cart-payments [session-id {:keys [order] :as args}]
  (api-req
   POST
   "/v2/update-cart-payments"
   request-keys/update-cart-payments
   {:params (-> order
                (select-keys [:number :token :cart-payments])
                (assoc :session-id session-id))
    :handler #(messages/handle-message events/api-success-update-order-update-cart-payments
                                       (merge args {:order %}))}))

(defn get-order [number token]
  (api-req
   GET
   (str "/v2/orders/" number)
   request-keys/get-order
   {:params
    {:token token}
    :handler
    #(messages/handle-message events/api-success-get-order %)}))

(defn get-completed-order [number token]
  (api-req
   GET
   (str "/v2/orders/" number)
   request-keys/get-order
   {:params
    {:token token}
    :handler
    #(messages/handle-message events/api-success-get-completed-order %)}))

(defn get-current-order [user-id user-token store-stylist-id]
  (api-req
   GET
   "/v2/current-order-for-user"
   request-keys/get-order
   {:params
    {:user-id user-id
     :user-token user-token
     :store-stylist-id store-stylist-id}
    :handler
    #(messages/handle-message events/api-success-get-order %)
    :error-handler (constantly nil)}))

(defn api-failure? [event]
  (= events/api-failure (subvec event 0 2)))

(defn add-promotion-code [session-id number token promo-code allow-dormant?]
  (api-req
   POST
   "/v2/add-promotion-code"
   request-keys/add-promotion-code
   {:params {:session-id session-id
             :number number
             :token token
             :code promo-code
             :allow-dormant allow-dormant?}
    :handler #(messages/handle-message events/api-success-update-order-add-promotion-code
                                       {:order %
                                        :promo-code promo-code
                                        :allow-dormant? allow-dormant?})
    :error-handler #(if allow-dormant?
                      (messages/handle-message events/api-failure-pending-promo-code %)
                      (let [response-body (get-in % [:response :body])]
                        (if (and (waiter-style? response-body)
                                 (= (:error-code response-body) "promotion-not-found"))
                          (messages/handle-message events/api-failure-errors-invalid-promo-code
                                                   (assoc (waiter-style->std-error response-body) :promo-code promo-code))
                          (default-error-handler %))))}))

(defn add-sku-to-bag [session-id {:keys [token number sku] :as params} handler]
  (api-req
   POST
   "/v2/add-to-bag"
   (conj request-keys/add-to-bag (:catalog/sku-id sku))
   {:params (merge (select-keys params [:quantity :stylist-id :user-id :user-token])
                   {:session-id session-id
                    :sku (:catalog/sku-id sku)}
                   (when (and token number) {:token token :number number}))
    :handler handler}))

(defn remove-promotion-code [session-id {:keys [token number]} promo-code]
  (api-req
   POST
   "/v2/remove-promotion-code"
   request-keys/remove-promotion-code
   {:params {:session-id session-id
             :number number :token token :code promo-code}
    :handler #(messages/handle-message events/api-success-update-order-remove-promotion-code
                                       {:order %
                                        :promo-code promo-code})}))

(defn create-shared-cart [session-id order-number order-token]
  (api-req
   POST
   "/create-shared-cart"
   request-keys/create-shared-cart
   {:params  {:session-id session-id
              :order-number order-number
              :order-token  order-token}
    :handler #(messages/handle-message events/api-success-shared-cart-create
                                       {:cart %})}))

(defn fetch-shared-cart [shared-cart-id]
  (api-req
   GET
   "/fetch-shared-cart"
   request-keys/fetch-shared-cart
   {:params  {:shared-cart-id shared-cart-id}
    :handler #(messages/handle-message events/api-success-shared-cart-fetch
                                       {:shared-cart (:shared-cart %)
                                        :skus        (:skus %)})}))

(defn create-order-from-cart [session-id shared-cart-id user-id user-token stylist-id]
  (api-req
   POST
   "/create-order-from-shared-cart"
   request-keys/create-order-from-shared-cart
   {:params {:session-id session-id
             :shared-cart-id shared-cart-id
             :user-id        user-id
             :user-token     user-token
             :stylist-id     stylist-id}
    :handler #(messages/handle-message events/api-success-update-order-from-shared-cart
                                       {:order %
                                        :shared-cart-id shared-cart-id
                                        :navigate events/navigate-cart})
    :error-handler #(do
                      ;; Order is important here, for correct display of errors
                      (default-error-handler %)
                      (messages/handle-message events/api-failure-order-not-created-from-shared-cart))}))

(defn send-referrals [session-id referral]
  (api-req
   POST
   "/leads/referrals"
   request-keys/send-referrals
   {:params (assoc referral :session-id session-id)
    :handler #(messages/handle-message events/api-success-send-stylist-referrals
                                      {:referrals %})}))

(defn- static-content-req [method path req-key {:keys [handler] :as request-opts}]
  (let [req-id       (str (random-uuid))
        content-opts {:format          :raw
                      :handler         (wrap-api-end req-key req-id handler)
                      :response-format (raw-response-format)}
        request      (method path (merge request-opts content-opts))]
    (messages/handle-message events/api-start {:xhr         request
                                               :request-key req-key
                                               :request-id  req-id})))

(defn get-static-content [[_ _ & static-content-id :as nav-event]]
  (static-content-req
   GET
   (str "/static" (routes/path-for nav-event))
   request-keys/get-static-content
   {:handler #(messages/handle-message events/api-success-get-static-content
                                       {:id      static-content-id
                                        :content %})}))

(defn telligent-sign-in [session-id user-id token]
  (api-req
   POST
   "/v2/login/telligent"
   request-keys/login-telligent
   {:params {:session-id session-id
             :token token
             :user-id user-id}
    :handler #(messages/handle-message events/api-success-telligent-login (set/rename-keys % {:max_age :max-age}))}))

(defn create-lead [params]
  (api-req
   POST
   "/leads"
   request-keys/create-lead
   {:params params
    :handler #(messages/handle-message events/api-success-lead-created %)}))

(defn advance-lead-registration [params handler]
  (api-req POST
             "/leads/advance-in-flow"
             request-keys/advance-lead
             {:params  params
              :handler handler}))
