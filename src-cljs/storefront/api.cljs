(ns storefront.api
  (:require [ajax.core :refer [GET POST PUT DELETE json-response-format]]
            [storefront.events :as events]
            [storefront.taxons :refer [taxon-name-from]]
            [clojure.set :refer [rename-keys]]
            [storefront.config :refer [api-base-url send-sonar-base-url send-sonar-publishable-key]]))

(defn default-error-handler [handle-message]
  (fn [response]
    (cond
      (zero? (:status response))
      (handle-message events/api-failure-no-network-connectivity response)

      (or (seq (get-in response [:response :error]))
          (seq (get-in response [:response :errors])))
      (handle-message events/api-failure-validation-errors
                      (-> (:response response)
                          (select-keys [:error :errors])
                          (rename-keys {:errors :fields})))

      (or (seq (get-in response [:response :exception])))
      (handle-message events/api-failure-validation-errors
                      {:fields {"" [(get-in response [:response :exception])]}})

      :else
      (handle-message events/api-failure-bad-server-response response))))

(defn filter-nil [m]
  (into {} (filter second m)))

(defn api-req [method path params success-handler error-handler]
  (method (str api-base-url path)
          {:handler success-handler
           :error-handler error-handler
           :headers {"Accepts" "application/json"}
           :format :json
           :params params
           :response-format (json-response-format {:keywords? true})}))

(defn cache-req [cache handle-message method path params success-handler error-handler]
  (let [key [path params]
        res (cache key)]
    (if res
      (success-handler res)
      (api-req method path params
               (fn [result]
                 (handle-message events/api-success-cache {key result})
                 (success-handler result))
               error-handler))))

(defn get-taxons [handle-message cache]
  (cache-req
   cache
   handle-message
   GET
   "/product-nav-taxonomy"
   {}
   #(handle-message events/api-success-taxons (select-keys % [:taxons]))
   (default-error-handler handle-message)))

(defn get-store [handle-message cache store-slug]
  (cache-req
   cache
   handle-message
   GET
   "/store"
   {:store_slug store-slug}
   #(handle-message events/api-success-store %)
   (default-error-handler handle-message)))

(defn get-promotions [handle-message cache]
  (cache-req
   cache
   handle-message 
   GET
   "/promotions"
   {}
   #(handle-message events/api-success-promotions %)
   (default-error-handler handle-message)))

(defn get-products [handle-message cache taxon-path]
  (cache-req
   cache
   handle-message 
   GET
   "/products"
   {:taxon_name (taxon-name-from taxon-path)}
   #(handle-message events/api-success-products (merge (select-keys % [:products])
                                                       {:taxon-path taxon-path}))
   (default-error-handler handle-message)))

(defn get-product [handle-message product-path]
  (api-req
   GET
   (str "/products")
   {:slug product-path}
   #(handle-message events/api-success-product {:product-path product-path
                                                :product %})
   (default-error-handler handle-message)))

(defn get-states [handle-message cache]
  (cache-req
   cache
   handle-message 
   GET
   "/states"
   {}
   #(handle-message events/api-success-states (select-keys % [:states]))
   (default-error-handler handle-message)))

(defn get-payment-methods [handle-message cache]
  (cache-req
   cache
   handle-message 
   GET
   "/payment_methods"
   {}
   #(handle-message events/api-success-payment-methods (select-keys % [:payment_methods]))
   (default-error-handler handle-message)))

(defn select-sign-in-keys [args]
  (select-keys args [:email :token :store_slug :id :order-id :order-token]))

(defn sign-in [handle-message email password stylist-id order-token]
  (api-req
   POST
   "/login"
   {:email email
    :password password
    :stylist-id stylist-id
    :order-token order-token}
   #(handle-message events/api-success-sign-in (select-sign-in-keys %))
   (default-error-handler handle-message)))

(defn sign-up [handle-message email password password-confirmation stylist-id order-token]
  (api-req
   POST
   "/signup"
   {:email email
    :password password
    :password_confirmation password-confirmation
    :stylist-id stylist-id
    :order-token order-token}
   #(handle-message events/api-success-sign-up (select-sign-in-keys %))
   (default-error-handler handle-message)))

(defn forgot-password [handle-message email]
  (api-req
   POST
   "/forgot_password"
   {:email email}
   #(handle-message events/api-success-forgot-password)
   (default-error-handler handle-message)))

(defn reset-password [handle-message password password-confirmation reset-token]
  (api-req
   POST
   "/reset_password"
   {:password password
    :password_confirmation password-confirmation
    :reset_password_token reset-token}
   #(handle-message events/api-success-reset-password (select-sign-in-keys %))
   (default-error-handler handle-message)))

(defn select-address-keys [m]
  (let [keys [:address1 :address2 :city :country_id :firstname :lastname :id :phone :state_id :zipcode]]
    (select-keys m keys)))

(defn rename-server-address-keys [m]
  (rename-keys m {:bill_address :billing-address
                  :ship_address :shipping-address}))

(defn get-account [handle-message id token stylist-id]
  (api-req
   GET
   "/users"
   {:id id
    :token token
    :stylist-id stylist-id}
   #(handle-message events/api-success-account (rename-server-address-keys %))
   (default-error-handler handle-message)))

(defn update-account [handle-message id email password password-confirmation token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :password password
    :password_confirmation password-confirmation
    :token token}
   #(handle-message events/api-success-manage-account (select-sign-in-keys %))
   (default-error-handler handle-message)))

(defn update-account-address [handle-message id email billing-address shipping-address token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :bill_address (select-address-keys billing-address)
    :ship_address (select-address-keys shipping-address)
    :token token}
   #(handle-message events/api-success-address (rename-server-address-keys %))
   (default-error-handler handle-message)))

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
   GET
   "/stylist"
   {:user-token user-token}
   #(handle-message events/api-success-stylist-manage-account
                    {:updated false
                     :stylist (select-stylist-account-keys %)})
   (default-error-handler handle-message)))

(defn update-stylist-account [handle-message user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   {:user-token user-token
    :stylist stylist-account}
   #(handle-message events/api-success-stylist-manage-account
                    {:updated true
                     :stylist (select-stylist-account-keys %)})
   (default-error-handler handle-message)))

(defn update-stylist-account-profile-picture [handle-message user-token profile-picture]
  (let [form-data (doto (js/FormData.)
                    (.append "file" profile-picture (.-name profile-picture))
                    (.append "user-token" user-token))]
    (PUT (str api-base-url "/stylist/profile-picture")
      {:handler #(handle-message events/api-success-stylist-manage-account-profile-picture
                                 (merge {:updated true}
                                        {:stylist (select-keys % [:profile_picture_url])}))
       :error-handler (default-error-handler handle-message)
       :params form-data
       :response-format (json-response-format {:keywords? true})
       :timeout 10000})))

(defn get-stylist-commissions [handle-message user-token]
  (api-req
   GET
   "/stylist/commissions"
   {:user-token user-token}
   #(handle-message events/api-success-stylist-commissions
                    (select-keys % [:rate :next-amount :paid-total :new-orders :payouts]))
   (default-error-handler handle-message)))

(defn get-stylist-bonus-credits [handle-message user-token]
  (api-req
   GET
   "/stylist/bonus-credits"
   {:user-token user-token}
   #(handle-message events/api-success-stylist-bonus-credits
                    (select-keys % [:bonus-amount
                                    :earning-amount
                                    :commissioned-revenue
                                    :total-credit
                                    :available-credit
                                    :bonuses]))
   (default-error-handler handle-message)))

(defn get-stylist-referral-program [handle-message user-token]
  (api-req
   GET
   "/stylist/referrals"
   {:user-token user-token}
   #(handle-message events/api-success-stylist-referral-program
                    (select-keys % [:sales-rep-email :bonus-amount :earning-amount :total-amount :referrals]))
   (default-error-handler handle-message)))

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

(defn create-order [handle-message stylist-id user-token]
  (api-req
   POST
   "/orders"
   (merge {:stylist-id stylist-id}
          (if user-token {:token user-token} {}))
   #(handle-message events/api-success-create-order (select-keys % [:number :token]))
   (default-error-handler handle-message)))

(defn create-order-if-needed [handle-message stylist-id order-id order-token user-token]
  (if (and order-token order-id)
    (handle-message events/api-success-create-order {:number order-id :token order-token})
    (create-order handle-message stylist-id user-token)))

(defn update-cart [handle-message user-token {order-token :guest-token :as order} extra-message-args]
  (api-req
   PUT
   "/cart"
   (filter-nil
    {:order (select-keys order [:number :line_items_attributes :coupon_code :email :user_id :state])
     :order_token order-token})
   #(handle-message events/api-success-update-cart
                    (merge {:order (rename-keys % {:token :guest-token})}
                           extra-message-args))
   (default-error-handler handle-message)))

(defn update-order [handle-message user-token order extra-message-args]
  (api-req
   PUT
   "/orders"
   {:order (filter-nil (-> order
                           (select-keys [:number
                                         :bill_address
                                         :ship_address
                                         :shipments_attributes
                                         :payments_attributes
                                         :session_id
                                         :email])
                           (update-in [:bill_address] select-address-keys)
                           (update-in [:ship_address] select-address-keys)
                           (rename-keys {:guest-token :token
                                         :bill_address :bill_address_attributes
                                         :ship_address :ship_address_attributes})))
    :use_store_credits (:use-store-credits order)
    :state (:state order)
    :order_token (:guest-token order)}
   #(handle-message events/api-success-update-order
                    (merge {:order (rename-keys % {:token :guest-token})}
                           extra-message-args))
   (default-error-handler handle-message)))

(defn add-line-item [handle-message variant-id variant-quantity order-number order-token]
  (api-req
   POST
   "/line-items"
   {:token order-token
    :order_id order-number
    :variant_id variant-id
    :variant_quantity variant-quantity}
   #(handle-message events/api-success-add-to-bag {:variant-id variant-id
                                                   :variant-quantity variant-quantity
                                                   :order-number order-number
                                                   :order-token order-token})
   (default-error-handler handle-message)))

(defn get-order [handle-message order-number order-token]
  (api-req
   GET
   "/orders"
   {:id order-number
    :token order-token}
   #(handle-message events/api-success-get-order (rename-keys % {:token :guest-token}))
   (default-error-handler handle-message)))

(defn get-past-order [handle-message order-number user-token]
  (api-req
   GET
   "/orders"
   {:id order-number
    :token user-token}
   #(handle-message events/api-success-get-past-order %)
   (default-error-handler handle-message)))

(defn get-my-orders [handle-message user-token]
  (api-req
   GET
   "/my_orders"
   {:user-token user-token}
   #(handle-message events/api-success-my-orders %)
   (default-error-handler handle-message)))

(defn add-to-bag [handle-message variant-id variant-quantity stylist-id order-token order-id user-token]
  (letfn [(refetch-order [order-id order-token]
            (get-order handle-message order-id order-token))
          (add-line-item-cb [order-id order-token]
            (add-line-item (fn [event args]
                             (refetch-order order-id order-token)
                             (handle-message event args))
                           variant-id variant-quantity order-id order-token))
          (created-order-cb [event {:keys [number token] :as args}]
            (add-line-item-cb number token)
            (handle-message event args))]
    (create-order-if-needed created-order-cb stylist-id order-id order-token user-token))) 
