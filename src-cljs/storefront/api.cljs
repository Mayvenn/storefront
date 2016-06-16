(ns storefront.api
  (:require [ajax.core :refer [GET POST PUT DELETE json-response-format]]
            [clojure.set :refer [subset?]]
            [storefront.events :as events]
            [storefront.messages :as messages]
            [storefront.accessors.states :as states]
            [storefront.accessors.orders :as orders]
            [clojure.set :refer [rename-keys]]
            [clojure.walk :refer [postwalk]]
            [storefront.config :refer [api-base-url send-sonar-base-url send-sonar-publishable-key]]
            [storefront.request-keys :as request-keys]))

(defn default-error-handler [response]
  (let [response-body (get-in response [:response :body])]
    (cond
      ;; aborted request
      (#{:aborted} (:failure response))
      (messages/handle-message events/api-abort)

      ;; connectivity
      (zero? (:status response))
      (messages/handle-message events/api-failure-no-network-connectivity response)

      ;; standard rails error response
      (or (seq (:error response-body))
          (seq (:errors response-body)))
      (messages/handle-message events/api-failure-validation-errors
                               (-> response-body
                                   (select-keys [:error :errors])
                                   (rename-keys {:error :error-message
                                                 :errors :details})))

      ;; standard rails validation errors
      (seq (:exception response-body))
      (messages/handle-message events/api-failure-validation-errors
                               {:details {"" [(:exception response-body)]}})

      ;; kill order cookies if no order is found
      (= "order-not-found" (:error-code response-body))
      (messages/handle-message events/api-handle-order-not-found response)

      ;; Standard waiter response
      (seq (:error-code response-body))
      (messages/handle-message events/api-failure-validation-errors
                               (select-keys response-body [:error-message :details]))

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

(defn filter-nil [m]
  (into {} (filter (comp not nil? val) m)))

(def default-req-opts {:format :json
                       :response-format (header-json-response-format {:keywords? true})})

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

;;  Neccessary for a frustrating bug in Clojurescript that doesn't seem to be
;;  able to hash a deep map correctly. Feel free to delete this when
;;  ClojureScript fixes this error.
(defn unique-serialize
  "Walks a collection and converts every map within into a sorted map, then
  serializes the entirity of it into a string."
  [coll]
  (let [sort-if-map
        #(if (map? %) (into (sorted-map) %) %)]
    (pr-str
     (postwalk sort-if-map coll))))

(defn cache-req
  [cache method path req-key {:keys [handler params] :as request-opts}]
  (let [key (pr-str (unique-serialize [path params]))
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

(defn get-taxons [cache]
  (cache-req
   cache
   GET
   "/bundle-builder-nav-taxonomy"
   request-keys/get-taxons
   {:handler
    #(messages/handle-message events/api-success-taxons (select-keys % [:taxons]))}))

(defn get-promotions [cache promo-code]
  (cache-req
   cache
   GET
   "/promotions"
   request-keys/get-promotions
   {:params {:additional-promo-code promo-code}
    :handler #(messages/handle-message events/api-success-promotions %)}))

(defn get-products [cache taxon-slug user-token]
  (cache-req
   cache
   GET
   "/products"
   (conj request-keys/get-products taxon-slug)
   {:params
    {:taxon-slug taxon-slug
     :user-token user-token}
    :handler
    #(messages/handle-message events/api-success-products
                              (merge (select-keys % [:products])
                                     {:taxon-slug taxon-slug}))}))

(defn get-products-by-ids [product-ids user-token]
  (api-req
   GET
   "/products"
   (conj request-keys/get-products (sorted-set product-ids))
   {:params {:ids product-ids
             :user-token user-token}
    :handler
    #(messages/handle-message events/api-success-products
                              (select-keys % [:products]))}))

(defn get-states [cache]
  (cache-req
   cache
   GET
   "/states"
   request-keys/get-states
   {:handler
    #(messages/handle-message events/api-success-states
                              (select-keys % [:states]))}))

(defn select-sign-in-keys [args]
  (select-keys args [:email :token :store_slug :id]))

(defn sign-in [email password stylist-id]
  (api-req
   POST
   "/login"
   request-keys/sign-in
   {:params
    {:email (.toLowerCase (str email))
     :password password
     :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-sign-in
                              (select-sign-in-keys %))}))

(defn facebook-sign-in [uid access-token stylist-id]
  (api-req
   POST
   "/facebook_login"
   request-keys/facebook-sign-in
   {:params
    {:uid uid
     :access-token access-token
     :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-sign-in
                              (select-sign-in-keys %))}))

(defn sign-up [email password password-confirmation stylist-id]
  (api-req
   POST
   "/signup"
   request-keys/sign-up
   {:params
    {:email email
     :password password
     :password-confirmation password-confirmation
     :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-sign-up
                              (select-sign-in-keys %))}))

(defn forgot-password [email]
  (api-req
   POST
   "/forgot_password"
   request-keys/forgot-password
   {:params
    {:email (.toLowerCase (str email))}
    :handler
    #(messages/handle-message events/api-success-forgot-password)}))

(defn reset-password [password password-confirmation reset-token]
  (api-req
   POST
   "/reset_password"
   request-keys/reset-password
   {:params
    {:password password
     :password_confirmation password-confirmation
     :reset_password_token reset-token}
    :handler
    #(messages/handle-message events/api-success-reset-password
                              (select-sign-in-keys %))}))

(defn facebook-reset-password [uid access-token reset-token]
  (api-req
   POST
   "/reset_facebook"
   request-keys/reset-facebook
   {:params
    {:uid uid
     :access-token access-token
     :reset-password-token reset-token}
    :handler
    #(messages/handle-message events/api-success-reset-password
                              (select-sign-in-keys %))}))

(defn add-user-in-order [token number user-token user-id]
  (api-req
   POST
   "/v2/add-user-to-order"
   request-keys/add-user-in-order
   {:params
    {:user-id user-id
     :user-token user-token
     :number number
     :token token}
    :handler
    #(messages/handle-message events/api-success-update-order
                              {:order %})}))

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

(defn get-messenger-token [id token]
  (api-req
   GET
   "/v2/users/tokens"
   request-keys/get-messenger-token
   {:params
    {:id id
     :token token}
    :handler
    #(messages/handle-message events/api-success-messenger-token (select-keys % [:messenger_token]))}))

(defn update-account [id email password password-confirmation token]
  (api-req
   PUT
   "/users"
   request-keys/update-account
   {:params
    {:id id
     :email email
     :password password
     :password_confirmation password-confirmation
     :token token}
    :handler
    #(messages/handle-message events/api-success-manage-account
                              (select-sign-in-keys %))}))

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
  (select-keys args [:birth_date_1i :birth_date_2i :birth_date_3i
                     :profile_picture_url
                     :chosen_payout_method
                     :venmo_payout_attributes
                     :paypal_payout_attributes
                     :instagram_account
                     :styleseat_account
                     :user
                     :address]))

(defn get-stylist-account [user-token]
  (api-req
   GET
   "/stylist"
   request-keys/get-stylist-account
   {:params
    {:user-token user-token}
    :handler
    #(messages/handle-message events/api-success-stylist-manage-account
                              {:updated false
                               :stylist (select-stylist-account-keys %)})}))

(defn get-shipping-methods []
  (api-req
   GET
   "/v2/shipping-methods"
   request-keys/get-shipping-methods
   {:handler #(messages/handle-message events/api-success-shipping-methods
                                       (update-in % [:shipping-methods] reverse))}))

(defn update-stylist-account [user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account
   {:params
    {:user-token user-token
     :stylist stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-manage-account
                              {:updated true
                               :stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-profile-picture [user-token profile-picture]
  (let [form-data (doto (js/FormData.)
                    (.append "file" profile-picture (.-name profile-picture))
                    (.append "user-token" user-token))]
    (PUT (str api-base-url "/stylist/profile-picture")
         {:handler #(messages/handle-message events/api-success-stylist-manage-account-profile-picture
                                             (merge {:updated true}
                                                    {:stylist (select-keys % [:profile_picture_url])}))
          :error-handler default-error-handler
          :params form-data
          :response-format (json-response-format {:keywords? true})
          :timeout 10000})))

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

(defn place-order [order]
  (api-req
   POST
   "/v2/place-order"
   request-keys/place-order
   {:params (select-keys order [:number :token :session-id])
    :handler #(messages/handle-message events/api-success-update-order-place-order
                                       {:order %
                                        :navigate events/navigate-order-complete})}))

(defn inc-line-item [order {:keys [variant-id] :as params}]
  (api-req
   POST
   "/v2/add-to-bag"
   (conj request-keys/increment-line-item variant-id)
   {:params (merge (select-keys order [:number :token])
                   {:variant-id variant-id
                    :quantity 1})
    :handler #(messages/handle-message events/api-success-add-to-bag
                                       {:order %
                                        :requested-quantity 1})}))

(defn dec-line-item [order {:keys [variant-id]}]
  (api-req
   POST
   "/v2/remove-from-bag"
   (conj request-keys/decrement-line-item variant-id)
   {:params (merge (select-keys order [:number :token])
                   {:variant-id variant-id
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
                                        :allow-dormant? allow-dormant?})
    :error-handler #(if allow-dormant?
                      (messages/handle-message events/api-failure-pending-promo-code %)
                      (default-error-handler %))}))

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
                                        :requested (select-keys params [:quantity :product :variant])})}))

(defn remove-promotion-code [{:keys [token number]} promo-code]
  (api-req
   POST
   "/v2/remove-promotion-code"
   request-keys/remove-promotion-code
   {:params {:number number :token token :code promo-code}
    :handler #(messages/handle-message events/api-success-update-order-remove-promotion-code
                                       {:order %})}))

(defn create-shared-cart [order-number order-token]
  (api-req
   POST
   "/create-shared-cart"
   request-keys/create-shared-cart
   {:params  {:order-number order-number
              :order-token  order-token}
    :handler #(messages/handle-message events/api-success-shared-cart
                                       {:cart %})}))
