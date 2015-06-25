(ns storefront.api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [ajax.core :refer [GET POST PUT DELETE json-response-format]]
            [cljs.core.async :refer [take! chan <! mult tap]]
            [storefront.messages :refer [enqueue-message]]
            [storefront.events :as events]
            [storefront.taxons :refer [taxon-name-from]]
            [clojure.set :refer [rename-keys]]
            [storefront.config :refer [api-base-url send-sonar-base-url send-sonar-publishable-key]]))

(defn default-error-handler [events-ch]
  (fn [response]
    (cond
      (zero? (:status response))
      (enqueue-message events-ch [events/api-failure-no-network-connectivity response])

      (or (seq (get-in response [:response :error]))
          (seq (get-in response [:response :errors])))
      (enqueue-message events-ch [events/api-failure-validation-errors
                                  (-> (:response response)
                                      (select-keys [:error :errors])
                                      (rename-keys {:errors :fields}))])

      (or (seq (get-in response [:response :exception])))
      (enqueue-message events-ch [events/api-failure-validation-errors
                                  {:fields {"" [(get-in response [:response :exception])]}}])

      :else
      (enqueue-message events-ch [events/api-failure-bad-server-response response]))))

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

(defn cache-req [cache events-ch method path params success-handler error-handler]
  (let [key [path params]
        res (cache key)]
    (if res
      (success-handler res)
      (api-req method path params
               (fn [result]
                 (enqueue-message events-ch [events/api-success-cache {key result}])
                 (success-handler result))
               error-handler))))

(defn get-taxons [events-ch cache]
  (cache-req
   cache
   events-ch
   GET
   "/product-nav-taxonomy"
   {}
   #(enqueue-message events-ch [events/api-success-taxons (select-keys % [:taxons])])
   (default-error-handler events-ch)))

(defn get-store [events-ch cache store-slug]
  (cache-req
   cache
   events-ch
   GET
   "/store"
   {:store_slug store-slug}
   #(enqueue-message events-ch [events/api-success-store %])
   (default-error-handler events-ch)))

(defn get-promotions [events-ch cache]
  (cache-req
   cache
   events-ch
   GET
   "/promotions"
   {}
   #(enqueue-message events-ch [events/api-success-promotions %])
   (default-error-handler events-ch)))

(defn get-products [events-ch cache taxon-path]
  (cache-req
   cache
   events-ch
   GET
   "/products"
   {:taxon_name (taxon-name-from taxon-path)}
   #(enqueue-message events-ch [events/api-success-products (merge (select-keys % [:products])
                                                                   {:taxon-path taxon-path})])
   (default-error-handler events-ch)))

(defn get-product [events-ch product-path]
  (api-req
   GET
   (str "/products")
   {:slug product-path}
   #(enqueue-message events-ch [events/api-success-product {:product-path product-path
                                                            :product %}])
   (default-error-handler events-ch)))

(defn get-states [events-ch cache]
  (cache-req
   cache
   events-ch
   GET
   "/states"
   {}
   #(enqueue-message events-ch [events/api-success-states (select-keys % [:states])])
   (default-error-handler events-ch)))

(defn get-payment-methods [events-ch cache]
  (cache-req
   cache
   events-ch
   GET
   "/payment_methods"
   {}
   #(enqueue-message events-ch [events/api-success-payment-methods (select-keys % [:payment_methods])])
   (default-error-handler events-ch)))

(defn select-sign-in-keys [args]
  (select-keys args [:email :token :store_slug :id :order-id :order-token]))

(defn sign-in [events-ch email password stylist-id order-token]
  (api-req
   POST
   "/login"
   {:email email
    :password password
    :stylist-id stylist-id
    :order-token order-token}
   #(enqueue-message events-ch [events/api-success-sign-in (select-sign-in-keys %)])
   (default-error-handler events-ch)))

(defn sign-up [events-ch email password password-confirmation stylist-id order-token]
  (api-req
   POST
   "/signup"
   {:email email
    :password password
    :password_confirmation password-confirmation
    :stylist-id stylist-id
    :order-token order-token}
   #(enqueue-message events-ch [events/api-success-sign-up (select-sign-in-keys %)])
   (default-error-handler events-ch)))

(defn forgot-password [events-ch email]
  (api-req
   POST
   "/forgot_password"
   {:email email}
   #(enqueue-message events-ch [events/api-success-forgot-password])
   (default-error-handler events-ch)))

(defn reset-password [events-ch password password-confirmation reset-token]
  (api-req
   POST
   "/reset_password"
   {:password password
    :password_confirmation password-confirmation
    :reset_password_token reset-token}
   #(enqueue-message events-ch [events/api-success-reset-password (select-sign-in-keys %)])
   (default-error-handler events-ch)))

(defn select-address-keys [m]
  (let [keys [:address1 :address2 :city :country_id :firstname :lastname :id :phone :state_id :zipcode]]
    (select-keys m keys)))

(defn rename-server-address-keys [m]
  (rename-keys m {:bill_address :billing-address
                  :ship_address :shipping-address}))

(defn get-account [events-ch id token stylist-id]
  (api-req
   GET
   "/users"
   {:id id
    :token token
    :stylist-id stylist-id}
   #(enqueue-message events-ch [events/api-success-account (rename-server-address-keys %)])
   (default-error-handler events-ch)))

(defn update-account [events-ch id email password password-confirmation token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :password password
    :password_confirmation password-confirmation
    :token token}
   #(enqueue-message events-ch [events/api-success-manage-account (select-sign-in-keys %)])
   (default-error-handler events-ch)))

(defn update-account-address [events-ch id email billing-address shipping-address token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :bill_address (select-address-keys billing-address)
    :ship_address (select-address-keys shipping-address)
    :token token}
   #(enqueue-message events-ch [events/api-success-address (rename-server-address-keys %)])
   (default-error-handler events-ch)))

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

(defn get-stylist-account [events-ch user-token]
  (api-req
   GET
   "/stylist"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-stylist-manage-account
                                {:updated false
                                 :stylist (select-stylist-account-keys %)}])
   (default-error-handler events-ch)))

(defn update-stylist-account [events-ch user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   {:user-token user-token
    :stylist stylist-account}
   #(enqueue-message events-ch [events/api-success-stylist-manage-account
                                {:updated true
                                 :stylist (select-stylist-account-keys %)}])
   (default-error-handler events-ch)))

(defn update-stylist-account-profile-picture [events-ch user-token profile-picture]
  (let [form-data (doto (js/FormData.)
                    (.append "file" profile-picture (.-name profile-picture))
                    (.append "user-token" user-token))]
    (PUT (str api-base-url "/stylist/profile-picture")
         {:handler #(enqueue-message events-ch
                                     [events/api-success-stylist-manage-account-profile-picture
                                      (merge {:updated true}
                                             {:stylist (select-keys % [:profile_picture_url])})])
          :error-handler (default-error-handler events-ch)
          :params form-data
          :response-format (json-response-format {:keywords? true})
          :timeout 10000})))

(defn get-stylist-commissions [events-ch user-token]
  (api-req
   GET
   "/stylist/commissions"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-stylist-commissions
                                (select-keys % [:rate :next-amount :paid-total :new-orders :payouts])])
   (default-error-handler events-ch)))

(defn get-stylist-bonus-credits [events-ch user-token]
  (api-req
   GET
   "/stylist/bonus-credits"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-stylist-bonus-credits
                     (select-keys % [:bonus-amount
                                     :earning-amount
                                     :commissioned-revenue
                                     :total-credit
                                     :available-credit
                                     :bonuses])])
   (default-error-handler events-ch)))

(defn get-stylist-referral-program [events-ch user-token]
  (api-req
   GET
   "/stylist/referrals"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-stylist-referral-program
                                (select-keys % [:sales-rep-email :bonus-amount :earning-amount :total-amount :referrals])])
   (default-error-handler events-ch)))

(defn get-sms-number [events-ch]
  (letfn [(normalize-number [x] ;; smooth out send-sonar's two different number formats
            (apply str (if (= "+" (first x))
                         (drop 3 x)
                         x)))
          (callback [resp]
            (enqueue-message events-ch
                  [events/api-success-sms-number
                   {:number (-> resp :available_number normalize-number)}]))]
    (GET (str send-sonar-base-url "/phone_numbers/available")
        {:handler callback
         :headers {"Accepts" "application/json"
                   "X-Publishable-Key" send-sonar-publishable-key}
         :format :json
         :response-format (json-response-format {:keywords? true})})))

(defn create-order [events-ch stylist-id user-token]
  (api-req
   POST
   "/orders"
   (merge {:stylist-id stylist-id}
          (if user-token {:token user-token} {}))
   #(enqueue-message events-ch [events/api-success-create-order (select-keys % [:number :token])])
   (default-error-handler events-ch)))

(defn create-order-if-needed [events-ch stylist-id order-id order-token user-token]
  (if (and order-token order-id)
    (enqueue-message events-ch [events/api-success-create-order {:number order-id :token order-token}])
    (create-order events-ch stylist-id user-token)))

(defn update-cart [events-ch user-token {order-token :guest-token :as order} extra-message-args]
  (api-req
   PUT
   "/cart"
   (filter-nil
    {:order (select-keys order [:number :line_items_attributes :coupon_code :email :user_id :state])
     :order_token order-token})
   #(enqueue-message events-ch [events/api-success-update-cart
                                (merge {:order (rename-keys % {:token :guest-token})}
                                       extra-message-args)])
   (default-error-handler events-ch)))

(defn update-order [events-ch user-token order extra-message-args]
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
   #(enqueue-message events-ch [events/api-success-update-order
                                (merge {:order (rename-keys % {:token :guest-token})}
                                       extra-message-args)])
   (default-error-handler events-ch)))

(defn add-line-item [events-ch variant-id variant-quantity order-number order-token]
  (api-req
   POST
   "/line-items"
   {:token order-token
    :order_id order-number
    :variant_id variant-id
    :variant_quantity variant-quantity}
   #(enqueue-message events-ch [events/api-success-add-to-bag {:variant-id variant-id
                                                    :variant-quantity variant-quantity
                                                    :order-number order-number
                                                               :order-token order-token}])
   (default-error-handler events-ch)))

(defn get-order [events-ch order-number order-token]
  (api-req
   GET
   "/orders"
   {:id order-number
    :token order-token}
   #(enqueue-message events-ch [events/api-success-get-order (rename-keys % {:token :guest-token})])
   (default-error-handler events-ch)))

(defn get-past-order [events-ch order-number user-token]
  (api-req
   GET
   "/orders"
   {:id order-number
    :token user-token}
   #(enqueue-message events-ch [events/api-success-get-past-order %])
   (default-error-handler events-ch)))

(defn get-my-orders [events-ch user-token]
  (api-req
   GET
   "/my_orders"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-my-orders %])
   (default-error-handler events-ch)))

(defn observe-events [f events-ch & args]
  (let [broadcast-ch (chan)
        mult-ch (mult broadcast-ch)
        result-ch (chan)]
    (tap mult-ch events-ch false)
    (tap mult-ch result-ch)
    (apply f broadcast-ch args)
    result-ch))

(defn add-to-bag [events-ch variant-id variant-quantity stylist-id order-token order-id user-token]
  (go
    (let [[_ {order-id :number order-token :token}] (<! (observe-events create-order-if-needed events-ch stylist-id order-id order-token user-token))]
      (<! (observe-events add-line-item events-ch variant-id variant-quantity order-id order-token))
      (get-order events-ch order-id order-token))))
