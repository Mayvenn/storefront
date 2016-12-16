(ns storefront.api
  (:require [ajax.core :refer [GET json-response-format POST PUT raw-response-format]]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.states :as states]
            [storefront.routes :as routes]
            [storefront.cache :as c]
            [storefront.config :refer [api-base-url send-sonar-base-url send-sonar-publishable-key]]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.utils.maps :refer [filter-nil]]
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
         {:handler (fn [response]
                     (messages/handle-message events/api-end {:request-key req-key
                                                              :request-id req-id
                                                              :app-version (:app-version response)})
                     (handler (:body response)))
          :error-handler (fn [res]
                           (messages/handle-message events/api-end {:request-id req-id
                                                                    :request-key req-key
                                                                    :app-version (-> res :response :app-version)})
                           ((or error-handler default-error-handler) res))}))

(defn api-req
  [method path req-key request-opts]
  (let [request-opts (update-in request-opts [:params] filter-nil)
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

(defn get-products-by-ids [product-ids user-token]
  (let [product-ids (->> product-ids (into (sorted-set)) vec)]
    (api-req
     GET
     "/products"
     (conj request-keys/get-products product-ids)
     {:params {:ids product-ids
               :user-token user-token}
      :handler
      #(messages/handle-message events/api-success-products
                                (select-keys % [:products]))})))

(defn get-saved-cards [user-id user-token]
  (api-req
   GET
   "/saved-cards"
   request-keys/get-saved-cards
   {:params {:user-token user-token :user-id user-id}
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

(defn sign-in [email password stylist-id order-number order-token]
  (api-req
   POST
   "/v2/login"
   request-keys/sign-in
   {:params
    {:email (.toLowerCase (str email))
     :password password
     :stylist-id stylist-id
     :order-number order-number
     :order-token order-token}
    :handler
    #(messages/handle-message events/api-success-auth-sign-in
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn facebook-sign-in [uid access-token stylist-id order-number order-token]
  (api-req
   POST
   "/v2/login/facebook"
   request-keys/facebook-sign-in
   {:params
    {:uid uid
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

(defn sign-up [email password stylist-id order-number order-token]
  (api-req
   POST
   "/v2/signup"
   request-keys/sign-up
   {:params
    {:email email
     :password password
     :stylist-id stylist-id
     :order-number order-number
     :order-token order-token}
    :handler
    #(messages/handle-message events/api-success-auth-sign-up
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn reset-password [password reset-token order-number order-token]
  (api-req
   POST
   "/v2/reset_password"
   request-keys/reset-password
   {:params
    {:password password
     :reset_password_token reset-token
     :order-number order-number
     :order-token order-token}
    :handler
    #(messages/handle-message events/api-success-auth-reset-password
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn facebook-reset-password [uid access-token reset-token order-number order-token]
  (api-req
   POST
   "/v2/reset_facebook"
   request-keys/reset-facebook
   {:params
    {:uid uid
     :access-token access-token
     :reset-password-token reset-token
     :order-number order-number
     :order-token order-token}
    :handler
    #(messages/handle-message events/api-success-auth-reset-password
                              (-> %
                                  select-auth-keys
                                  (assoc :flow "facebook")))}))

(defn forgot-password [email]
  (api-req
   POST
   "/forgot_password"
   request-keys/forgot-password
   {:params
    {:email (.toLowerCase (str email))}
    :handler
    #(messages/handle-message events/api-success-forgot-password {:email email})}))

(defn mayvenn->spree-address [states address]
  (-> address
      (select-keys [:address1 :address2 :city :first-name :last-name :phone :state :zipcode])
      (rename-keys {:first-name :firstname
                    :last-name :lastname
                    :state :state_id})
      (update-in [:state_id] (partial states/abbr->id states))
      (merge {:country_id 49})))

(defn spree->mayvenn-address [address]
  (-> address
      (dissoc :country_id)
      (rename-keys {:firstname :first-name
                    :lastname :last-name})
      (update-in [:state] :abbr)
      (select-keys [:address1 :address2 :city :first-name :last-name :phone :state :zipcode])))

(defn spree->mayvenn-addresses [contains-addresses]
  (-> contains-addresses
      (rename-keys {:bill_address :billing-address
                    :ship_address :shipping-address})
      (update-in [:billing-address] spree->mayvenn-address)
      (update-in [:shipping-address] spree->mayvenn-address)))

(defn get-account [id token stylist-id]
  (api-req
   GET
   "/users"
   request-keys/get-account
   {:params
    {:id id
     :token token
     :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-account
                              (spree->mayvenn-addresses %))}))

(defn update-account [id email password token]
  (api-req
   PUT
   "/users"
   request-keys/update-account
   {:params (merge  {:id    id
                     :email email
                     :token token}
                    (when (seq password)
                      {:password password}))
    :handler
    #(messages/handle-message events/api-success-manage-account
                              (select-user-keys %))}))

(defn update-account-address [states {:keys [id email user-token]} billing-address shipping-address]
  (api-req
   PUT
   "/users"
   request-keys/update-account-address
   {:params
    {:id id
     :email email
     :token user-token
     :bill_address (mayvenn->spree-address states billing-address)
     :ship_address (mayvenn->spree-address states shipping-address)}
    :handler identity}))

(defn select-stylist-account-keys [args]
  (-> args
      (select-keys [:birth_date_1i :birth_date_2i :birth_date_3i
                    :birth-date
                    :profile_picture_url
                    :chosen_payout_method
                    :venmo_payout_attributes
                    :paypal_payout_attributes
                    :instagram_account
                    :styleseat_account
                    :user
                    :address])
      (assoc :original_payout_method (:chosen_payout_method args))))

(defn get-stylist-account [user-token]
  (api-req
   GET
   "/stylist"
   request-keys/get-stylist-account
   {:params
    {:user-token user-token}
    :handler
    #(messages/handle-message events/api-success-stylist-account
                              {:updated false
                               :stylist (select-stylist-account-keys %)})}))

(defn get-shipping-methods []
  (api-req
   GET
   "/v2/shipping-methods"
   request-keys/get-shipping-methods
   {:handler #(messages/handle-message events/api-success-shipping-methods
                                       (update-in % [:shipping-methods] reverse))}))

(defn update-stylist-account-profile [user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-profile
   {:params {:user-token user-token :stylist stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-profile
                              {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-password [user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-password
   {:params {:user-token user-token :stylist stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-password
                              {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-commission [user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-commission
   {:params {:user-token user-token :stylist stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-commission
                              {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-social [user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-social
   {:params {:user-token user-token :stylist stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-social
                              {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-photo [user-token profile-picture]
  (let [form-data (doto (js/FormData.)
                    (.append "file" profile-picture (.-name profile-picture))
                    (.append "user-token" user-token))]
    (api-req PUT
             "/stylist/profile-picture"
             request-keys/update-stylist-account-photo
             {:params          form-data
              :format          "multipart/form-data"
              :error-handler   (fn [response]
                                 (if (= 413 (:status response))
                                   (messages/handle-message events/api-failure-stylist-account-photo-too-large response)
                                   (default-error-handler response)))
              :timeout         20000
              :handler         #(messages/handle-message events/api-success-stylist-account-photo
                                                         {:updated true
                                                          :stylist (select-keys % [:profile_picture_url])})})))

(defn get-stylist-stats [user-token]
  (api-req
   GET
   "/stylist/stats"
   request-keys/get-stylist-stats
   {:params
    {:user-token user-token}
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

(defn get-stylist-bonus-credits [user-token {:keys [page]}]
  (api-req
   GET
   "/stylist/bonus-credits"
   request-keys/get-stylist-bonus-credits
   {:params
    {:user-token user-token
     :page page}
    :handler
    #(messages/handle-message events/api-success-stylist-bonus-credits
                              (select-keys % [:bonus-amount
                                              :earning-amount
                                              :progress-to-next-bonus
                                              :lifetime-total
                                              :bonuses
                                              :current-page
                                              :pages]))}))

(defn get-stylist-referral-program [user-token {:keys [page]}]
  (api-req
   GET
   "/stylist/referrals"
   request-keys/get-stylist-referral-program
   {:params
    {:user-token user-token
     :page page}
    :handler
    #(messages/handle-message events/api-success-stylist-referral-program
                              (select-keys % [:sales-rep-email
                                              :bonus-amount
                                              :earning-amount
                                              :lifetime-total
                                              :referrals
                                              :current-page
                                              :pages]))}))

(defn get-sms-number []
  (letfn [(normalize-number [x] ;; smooth out send-sonar's two different number formats
            (apply str (if (= "+" (first x))
                         (drop 3 x)
                         x)))
          (callback [resp] (messages/handle-message events/api-success-sms-number
                                                    {:number (-> resp :available_number normalize-number)}))]
    (GET (str send-sonar-base-url "/phone_numbers/available")
         {:handler callback
          :headers {"X-Publishable-Key" send-sonar-publishable-key}
          :format :json
          :response-format (json-response-format {:keywords? true})})))

(defn place-order [order session-id utm-params]
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

(defn inc-line-item [order {:keys [variant]}]
  (api-req
   POST
   "/v2/add-to-bag"
   (conj request-keys/increment-line-item (:id variant))
   {:params (merge (select-keys order [:number :token])
                   {:variant-id (:id variant)
                    :quantity 1})
    :handler #(messages/handle-message events/api-success-add-to-bag
                                       {:order %
                                        :variant variant
                                        :quantity 1})}))

(defn dec-line-item [order {:keys [variant]}]
  (api-req
   POST
   "/v2/remove-from-bag"
   (conj request-keys/decrement-line-item (:id variant))
   {:params (merge (select-keys order [:number :token])
                   {:variant-id (:id variant)
                    :quantity 1})
    :handler #(messages/handle-message events/api-success-add-to-bag
                                       {:order %})}))

(defn delete-line-item [order variant-id]
  (api-req
   POST
   "/v2/remove-from-bag"
   (conj request-keys/delete-line-item variant-id)
   {:params (merge (select-keys order [:number :token])
                   {:variant-id variant-id
                    :quantity (->> order
                                   orders/product-items
                                   (orders/line-item-by-id variant-id)
                                   :quantity)})
    :handler #(messages/handle-message events/api-success-remove-from-bag
                                       {:order %})}))

(defn update-addresses [order]
  (api-req
   POST
   "/v2/update-addresses"
   request-keys/update-addresses
   {:params (select-keys order [:number :token :billing-address :shipping-address])
    :handler #(messages/handle-message events/api-success-update-order-update-address
                                       {:order %
                                        :navigate events/navigate-checkout-payment})}))

(defn guest-update-addresses [order]
  (api-req
   POST
   "/v2/guest-update-addresses"
   request-keys/update-addresses
   {:params (select-keys order [:number :token :email :billing-address :shipping-address])
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

(defn update-shipping-method [order]
  (api-req
   POST
   "/v2/update-shipping-method"
   request-keys/update-shipping-method
   {:params (select-keys order [:number :token :shipping-method-sku])
    :handler #(messages/handle-message events/api-success-update-order-update-shipping-method
                                       {:order %})}))

(defn update-cart-payments [{:keys [order place-order?] :as args}]
  (api-req
   POST
   "/v2/update-cart-payments"
   request-keys/update-cart-payments
   {:params (select-keys order [:number :token :cart-payments])
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

(defn add-promotion-code [number token promo-code allow-dormant?]
  (api-req
   POST
   "/v2/add-promotion-code"
   request-keys/add-promotion-code
   {:params {:number number
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

(defn add-to-bag [{:keys [token number variant] :as params}]
  (api-req
   POST
   "/v2/add-to-bag"
   request-keys/add-to-bag
   {:params (merge (select-keys params [:quantity :stylist-id :user-id :user-token])
                   {:variant-id (:id variant)}
                   (when (and token number) {:token token :number number}))
    :handler #(messages/handle-message events/api-success-add-to-bag
                                       {:order %
                                        :quantity (:quantity params)
                                        :variant (:variant params)})}))

(defn remove-promotion-code [{:keys [token number]} promo-code]
  (api-req
   POST
   "/v2/remove-promotion-code"
   request-keys/remove-promotion-code
   {:params {:number number :token token :code promo-code}
    :handler #(messages/handle-message events/api-success-update-order-remove-promotion-code
                                       {:order %
                                        :promo-code promo-code})}))

(defn create-shared-cart [order-number order-token]
  (api-req
   POST
   "/create-shared-cart"
   request-keys/create-shared-cart
   {:params  {:order-number order-number
              :order-token  order-token}
    :handler #(messages/handle-message events/api-success-shared-cart
                                       {:cart %})}))

(defn create-order-from-cart [shared-cart-id user-id user-token stylist-id]
  (api-req
   POST
   "/create-order-from-shared-cart"
   request-keys/create-order-from-shared-cart
   {:params {:shared-cart-id shared-cart-id
             :user-id        user-id
             :user-token     user-token
             :stylist-id     stylist-id}
    :handler #(messages/handle-message events/api-success-update-order-from-shared-cart
                                       {:order %
                                        :navigate events/navigate-cart})
    :error-handler #(do
                      ;; Order is important here, for correct display of errors
                      (default-error-handler %)
                      (messages/handle-message events/api-failure-order-not-created-from-shared-cart))}))

(defn send-referrals [referral]
  (api-req
   POST
   "/leads/referrals"
   request-keys/send-referrals
   {:params referral
    :handler #(messages/handle-message events/api-success-send-stylist-referrals
                                      {:referrals %})}))

(defn- static-content-req [method path req-key {:keys [handler params] :as request-opts}]
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

(defn telligent-sign-in [user-id token]
  (api-req
   POST
   "/v2/login/telligent"
   request-keys/login-telligent
   {:params {:token token
             :user-id user-id}
    :handler #(messages/handle-message events/api-success-telligent-login (set/rename-keys % {:max_age :max-age}))}))
