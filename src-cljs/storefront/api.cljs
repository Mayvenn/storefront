(ns storefront.api
  (:require [ajax.core :refer [GET POST PUT DELETE json-response-format]]
            [clojure.set :refer [subset?]]
            [storefront.events :as events]
            [storefront.accessors.states :as states]
            [storefront.accessors.taxons :refer [taxon-name-from]]
            [storefront.accessors.orders :as orders]
            [clojure.set :refer [rename-keys]]
            [clojure.walk :refer [postwalk]]
            [storefront.config :refer [api-base-url send-sonar-base-url send-sonar-publishable-key]]
            [storefront.request-keys :as request-keys]))

(defn default-error-handler [handle-message response]
  (cond
    ;; aborted request
    (#{:aborted} (:failure response))
    (handle-message events/api-abort)

    ;; connectivity
    (zero? (:status response))
    (handle-message events/api-failure-no-network-connectivity response)

    ;; standard rails error response
    (or (seq (get-in response [:response :error]))
        (seq (get-in response [:response :errors])))
    (handle-message events/api-failure-validation-errors
                    (-> (:response response)
                        (select-keys [:error :errors])
                        (rename-keys {:error :error-message
                                      :errors :details})))

    ;; standard rails validation errors
    (seq (get-in response [:response :exception]))
    (handle-message events/api-failure-validation-errors
                    {:details {"" [(get-in response [:response :exception])]}})

    ;; kill order cookies if no order is found
    (= "order-not-found" (-> response :response :error-code))
    (handle-message events/api-handle-order-not-found response)

    ;; Standard waiter response
    (seq (get-in response [:response :error-code]))
    (handle-message events/api-failure-validation-errors
                    (select-keys (:response response) [:error-message :details]))

    :else
    (handle-message events/api-failure-bad-server-response response)))

(defn filter-nil [m]
  (into {} (filter (comp not nil? val) m)))

(def default-req-opts {:headers {"Accepts" "application/json"}
                       :format :json
                       :response-format (json-response-format {:keywords? true})})

(defn merge-req-opts [handle-message req-key req-id {:keys [handler error-handler] :as request-opts}]
  (merge default-req-opts
         request-opts
         {:handler (fn [res]
                     (handle-message events/api-end {:request-key req-key
                                                     :request-id req-id})
                     (handler res))
          :error-handler (fn [response]
                           (handle-message events/api-end {:request-id req-id
                                                           :request-key req-key})
                           ((or error-handler (partial default-error-handler handle-message))
                            response))}))

(defn api-req
  [handle-message method path req-key request-opts]
  (let [request-opts (update-in request-opts [:params] filter-nil)
        req-id (str (random-uuid))
        request
        (method (str api-base-url path)
                (merge-req-opts handle-message req-key req-id request-opts))]
    (handle-message events/api-start {:xhr request
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
  [cache handle-message method path req-key {:keys [handler params] :as request-opts}]
  (let [key (pr-str (unique-serialize [path params]))
        res (cache key)]
    (if res
      (handler res)
      (api-req handle-message
               method
               path
               req-key
               (merge request-opts
                      {:handler
                       (fn [result]
                         (handle-message events/api-success-cache {key result})
                         (handler result))})))))

(defn get-taxons [handle-message cache]
  (cache-req
   cache
   handle-message
   GET
   "/bundle-builder-nav-taxonomy"
   request-keys/get-taxons
   {:handler
    #(handle-message events/api-success-taxons (select-keys % [:taxons]))}))

(defn get-store [handle-message cache store-slug]
  (cache-req
   cache
   handle-message
   GET
   "/store"
   request-keys/get-store
   {:params
    {:store_slug store-slug}
    :handler
    #(handle-message events/api-success-store %)}))

(defn get-promotions [handle-message cache promo-code]
  (cache-req
   cache
   handle-message
   GET
   "/promotions"
   request-keys/get-promotions
   {:params {:promo-code promo-code}
    :handler #(handle-message events/api-success-promotions %)}))

(defn get-products [handle-message cache taxon-path user-token]
  (cache-req
   cache
   handle-message
   GET
   "/products"
   (conj request-keys/get-products taxon-path)
   {:params
    {:taxon_name (taxon-name-from taxon-path)
     :taxonomy "bundle-builder"
     :user-token user-token}
    :handler
    #(handle-message events/api-success-taxon-products (merge (select-keys % [:products])
                                                              {:taxon-path taxon-path}))}))

(defn get-products-by-ids [handle-message product-ids]
  (api-req
   handle-message
   GET
   "/products"
   request-keys/get-product
   {:params {:ids product-ids}
    :handler
    #(handle-message events/api-success-order-products (select-keys % [:products]))}))

(defn get-product-by-id [handle-message product-id]
  (api-req
   handle-message
   GET
   "/products"
   request-keys/get-product
   {:params {:id product-id}
    :handler
    #(handle-message events/api-success-product {:product %})}))

(defn get-product [handle-message product-path]
  (api-req
   handle-message
   GET
   "/products"
   request-keys/get-product
   {:params {:slug product-path}
    :handler
    #(handle-message events/api-success-product {:product-path product-path
                                                 :product %})}))

(defn get-states [handle-message cache]
  (cache-req
   cache
   handle-message
   GET
   "/states"
   request-keys/get-states
   {:handler
    #(handle-message events/api-success-states (select-keys % [:states]))}))

(defn select-sign-in-keys [args]
  (select-keys args [:email :token :store_slug :id]))

(defn sign-in [handle-message email password stylist-id]
  (api-req
   handle-message
   POST
   "/login"
   request-keys/sign-in
   {:params
    {:email (.toLowerCase (str email))
     :password password
     :stylist-id stylist-id}
    :handler
    #(handle-message events/api-success-sign-in (select-sign-in-keys %))}))

(defn facebook-sign-in [handle-message uid access-token stylist-id]
  (api-req
   handle-message
   POST
   "/facebook_login"
   request-keys/facebook-sign-in
   {:params
    {:uid uid
     :access-token access-token
     :stylist-id stylist-id}
    :handler
    #(handle-message events/api-success-sign-in (select-sign-in-keys %))}))

(defn sign-up [handle-message email password password-confirmation stylist-id]
  (api-req
   handle-message
   POST
   "/signup"
   request-keys/sign-up
   {:params
    {:email email
     :password password
     :password-confirmation password-confirmation
     :stylist-id stylist-id}
    :handler
    #(handle-message events/api-success-sign-up (select-sign-in-keys %))}))

(defn forgot-password [handle-message email]
  (api-req
   handle-message
   POST
   "/forgot_password"
   request-keys/forgot-password
   {:params
    {:email (.toLowerCase (str email))}
    :handler
    #(handle-message events/api-success-forgot-password)}))

(defn reset-password [handle-message password password-confirmation reset-token]
  (api-req
   handle-message
   POST
   "/reset_password"
   request-keys/reset-password
   {:params
    {:password password
     :password_confirmation password-confirmation
     :reset_password_token reset-token}
    :handler
    #(handle-message events/api-success-reset-password (select-sign-in-keys %))}))

(defn facebook-reset-password [handle-message uid access-token reset-token]
  (api-req
   handle-message
   POST
   "/reset_facebook"
   request-keys/reset-facebook
   {:params
    {:uid uid
     :access-token access-token
     :reset-password-token reset-token}
    :handler
    #(handle-message events/api-success-reset-password (select-sign-in-keys %))}))

(defn add-user-in-order [handle-message token number user-token user-id]
  (api-req
   handle-message
   POST
   "/v2/add-user-to-order"
   request-keys/add-user-in-order
   {:params
    {:user-id user-id
     :user-token user-token
     :number number
     :token token}
    :handler
    #(handle-message events/api-success-update-order {:order %})}))

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

(defn get-account [handle-message id token stylist-id]
  (api-req
   handle-message
   GET
   "/users"
   request-keys/get-account
   {:params
    {:id id
     :token token
     :stylist-id stylist-id}
    :handler
    #(handle-message events/api-success-account (spree->mayvenn-addresses %))}))

(defn update-account [handle-message id email password password-confirmation token]
  (api-req
   handle-message
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
    #(handle-message events/api-success-manage-account (select-sign-in-keys %))}))

(defn update-account-address [handle-message states {:keys [id email user-token]} billing-address shipping-address]
  (api-req
   handle-message
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

(defn get-stylist-account [handle-message user-token]
  (api-req
   handle-message
   GET
   "/stylist"
   request-keys/get-stylist-account
   {:params
    {:user-token user-token}
    :handler
    #(handle-message events/api-success-stylist-manage-account
                     {:updated false
                      :stylist (select-stylist-account-keys %)})}))

(defn get-shipping-methods [handle-message]
  (api-req
   handle-message
   GET
   "/v2/shipping-methods"
   request-keys/get-shipping-methods
   {:handler #(handle-message events/api-success-shipping-methods (update-in % [:shipping-methods] reverse))}))

(defn update-stylist-account [handle-message user-token stylist-account]
  (api-req
   handle-message
   PUT
   "/stylist"
   request-keys/update-stylist-account
   {:params
    {:user-token user-token
     :stylist stylist-account}
    :handler
    #(handle-message events/api-success-stylist-manage-account
                     {:updated true
                      :stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-profile-picture [handle-message user-token profile-picture]
  (let [form-data (doto (js/FormData.)
                    (.append "file" profile-picture (.-name profile-picture))
                    (.append "user-token" user-token))]
    (PUT (str api-base-url "/stylist/profile-picture")
      {:handler #(handle-message events/api-success-stylist-manage-account-profile-picture
                                 (merge {:updated true}
                                        {:stylist (select-keys % [:profile_picture_url])}))
       :error-handler (partial default-error-handler handle-message)
       :params form-data
       :response-format (json-response-format {:keywords? true})
       :timeout 10000})))

(defn get-stylist-commissions [handle-message user-id user-token]
  (api-req
   handle-message
   GET
   "/v2/stylist/commissions"
   request-keys/get-stylist-commissions
   {:params
    {:user-id user-id :user-token user-token}
    :handler
    #(handle-message events/api-success-stylist-commissions
                     (select-keys % [:rate :next-amount :paid-total :new-orders :payouts]))}))

(defn get-stylist-bonus-credits [handle-message user-token]
  (api-req
   handle-message
   GET
   "/stylist/bonus-credits"
   request-keys/get-stylist-bonus-credits
   {:params
    {:user-token user-token}
    :handler
    #(handle-message events/api-success-stylist-bonus-credits
                     (select-keys % [:bonus-amount
                                     :earning-amount
                                     :commissioned-revenue
                                     :total-credit
                                     :available-credit
                                     :bonuses]))}))

(defn get-stylist-referral-program [handle-message user-token]
  (api-req
   handle-message
   GET
   "/stylist/referrals"
   request-keys/get-stylist-referral-program
   {:params
    {:user-token user-token}
    :handler
    #(handle-message events/api-success-stylist-referral-program
                     (select-keys % [:sales-rep-email :bonus-amount :earning-amount :total-amount :referrals]))}))

(defn get-sms-number [handle-message]
  (letfn [(normalize-number [x] ;; smooth out send-sonar's two different number formats
            (apply str (if (= "+" (first x))
                         (drop 3 x)
                         x)))
          (callback [resp]
            (handle-message events/api-success-sms-number
                            {:number (-> resp :available_number normalize-number)}))]
    (GET (str send-sonar-base-url "/phone_numbers/available")
      {:handler callback
       :headers {"Accepts" "application/json"
                 "X-Publishable-Key" send-sonar-publishable-key}
       :format :json
       :response-format (json-response-format {:keywords? true})})))

(defn place-order [handle-message order]
  (api-req
   handle-message
   POST
   "/v2/place-order"
   request-keys/place-order
   {:params (select-keys order [:number :token :session-id])
    :handler #(handle-message events/api-success-update-order-place-order
                              {:order %
                               :navigate events/navigate-order-complete})}))

(defn inc-line-item [handle-message order {:keys [variant-id] :as params}]
  (api-req
   handle-message
   POST
   "/v2/add-to-bag"
   (conj request-keys/increment-line-item variant-id)
   {:params (merge (select-keys order [:number :token])
                   {:variant-id variant-id
                    :quantity 1})
    :handler #(handle-message events/api-success-add-to-bag {:order %
                                                             :requested-quantity 1})}))

(defn dec-line-item [handle-message order {:keys [variant-id]}]
  (api-req
   handle-message
   POST
   "/v2/remove-from-bag"
   (conj request-keys/decrement-line-item variant-id)
   {:params (merge (select-keys order [:number :token])
                   {:variant-id variant-id
                    :quantity 1})
    :handler #(handle-message events/api-success-add-to-bag {:order %})}))

(defn delete-line-item [handle-message order variant-id]
  (api-req
   handle-message
   POST
   "/v2/remove-from-bag"
   (conj request-keys/delete-line-item variant-id)
   {:params (merge (select-keys order [:number :token])
                   {:variant-id variant-id
                    :quantity (->> order
                                   orders/product-items
                                   (orders/line-item-by-id variant-id)
                                   :quantity)})
    :handler #(handle-message events/api-success-remove-from-bag {:order %})}))

(defn update-addresses [handle-message order]
  (api-req
   handle-message
   POST
   "/v2/update-addresses"
   request-keys/update-addresses
   {:params (select-keys order [:number :token :billing-address :shipping-address])
    :handler #(handle-message events/api-success-update-order-update-address
                              {:order %
                               :navigate events/navigate-checkout-delivery})}))

(defn update-shipping-method [handle-message order]
  (api-req
   handle-message
   POST
   "/v2/update-shipping-method"
   request-keys/update-shipping-method
   {:params (select-keys order [:number :token :shipping-method-sku])
    :handler #(handle-message events/api-success-update-order-update-shipping-method
                              {:order %
                               :navigate events/navigate-checkout-payment})}))

(defn update-cart-payments [handle-message {:keys [order] :as args}]
  (api-req
   handle-message
   POST
   "/v2/update-cart-payments"
   request-keys/update-cart-payments
   {:params (select-keys order [:number :token :cart-payments])
    :handler #(handle-message events/api-success-update-order-update-cart-payments
                              (merge args {:order %}))}))

(defn get-order [handle-message number token]
  (api-req
   handle-message
   GET
   (str "/v2/orders/" number)
   request-keys/get-order
   {:params
    {:token token}
    :handler
    #(handle-message events/api-success-get-order %)}))

(defn get-current-order [handle-message user-id user-token store-stylist-id]
  (api-req
   handle-message
   GET
   "/v2/current-order-for-user"
   request-keys/get-order
   {:params
    {:user-id user-id
     :user-token user-token
     :store-stylist-id store-stylist-id}
    :handler
    #(handle-message events/api-success-get-order %)
    :error-handler (constantly nil)}))

(defn get-past-order
  [handle-message order-number user-token user-id]
  (api-req
   handle-message
   GET
   (str "/v2/orders/" order-number)
   request-keys/get-past-order
   {:params
    {:user-id user-id
     :user-token user-token}
    :handler
    #(handle-message events/api-success-get-past-order %)}))

(defn get-my-orders [handle-message user-token]
  (api-req
   handle-message
   GET
   "/my_orders"
   request-keys/get-my-orders
   {:params
    {:user-token user-token}
    :handler
    #(handle-message events/api-success-my-orders %)}))

(defn api-failure? [event]
  (= events/api-failure (subvec event 0 2)))

(defn add-promotion-code [handle-message number token promo-code allow-dormant?]
  (api-req
   handle-message
   POST
   "/v2/add-promotion-code"
   request-keys/add-promotion-code
   {:params {:number number
             :token token
             :code promo-code
             :allow-dormant allow-dormant?}
    :handler #(handle-message events/api-success-update-order-add-promotion-code
                              {:order %
                               :allow-dormant? allow-dormant?})
    :error-handler #(if allow-dormant?
                      (handle-message events/api-failure-pending-promo-code %)
                      (default-error-handler handle-message %))}))

(defn add-to-bag [handle-message {:keys [token number variant] :as params}]
  (api-req
   handle-message
   POST
   "/v2/add-to-bag"
   request-keys/add-to-bag
   {:params (merge (select-keys params [:quantity :stylist-id :user-id :user-token])
                   {:variant-id (:id variant)}
                   (when (and token number) {:token token :number number}))
    :handler #(handle-message events/api-success-add-to-bag
                              {:order %
                               :requested (select-keys params [:quantity :product :variant])})}))
