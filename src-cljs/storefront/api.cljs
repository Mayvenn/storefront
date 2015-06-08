(ns storefront.api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [ajax.core :refer [GET POST PUT DELETE json-response-format]]
            [cljs.core.async :refer [take! chan <! mult tap]]
            [storefront.messages :refer [enqueue-message]]
            [storefront.events :as events]
            [storefront.taxons :refer [taxon-name-from]]
            [clojure.set :refer [rename-keys]]
            [storefront.config :refer [api-base-url send-sonar-base-url send-sonar-publishable-key]]))

(defn filter-nil [m]
  (into {} (filter second m)))

(defn api-req [method path params success-handler]
  (method (str api-base-url path)
          {:handler success-handler
           :error-handler #(js/console.error path (clj->js %))
           :headers {"Accepts" "application/json"}
           :format :json
           :params params
           :response-format (json-response-format {:keywords? true})}))

(defn cache-req [cache events-ch method path params cb]
  (let [key [path params]
        res (cache key)]
    (if res
      (cb res)
      (api-req method path params
               (fn [result]
                 (enqueue-message events-ch [events/api-success-cache {key result}])
                 (cb result))))))

(defn get-taxons [events-ch cache]
  (cache-req
   cache
   events-ch
   GET
   "/product-nav-taxonomy"
   {}
   #(enqueue-message events-ch [events/api-success-taxons (select-keys % [:taxons])])))

(defn get-store [events-ch cache store-slug]
  (cache-req
   cache
   events-ch
   GET
   "/store"
   {:store_slug store-slug}
   #(enqueue-message events-ch [events/api-success-store %])))

(defn get-promotions [events-ch cache]
  (cache-req
   cache
   events-ch
   GET
   "/promotions"
   {}
   #(enqueue-message events-ch [events/api-success-promotions %])))

(defn get-products [events-ch cache taxon-path]
  (cache-req
   cache
   events-ch
   GET
   "/products"
   {:taxon_name (taxon-name-from taxon-path)}
   #(enqueue-message events-ch [events/api-success-products (merge (select-keys % [:products])
                                                        {:taxon-path taxon-path})])))

(defn get-product [events-ch cache product-path]
  (cache-req
   cache
   events-ch
   GET
   (str "/products")
   {:slug product-path}
   #(enqueue-message events-ch [events/api-success-product {:product-path product-path
                                                 :product %}])))

(defn get-states [events-ch cache]
  (cache-req
   cache
   events-ch
   GET
   "/states"
   {}
   #(enqueue-message events-ch [events/api-success-states (select-keys % [:states])])))

(defn select-sign-in-keys [args]
  (select-keys args [:email :token :store_slug :id]))

(defn sign-in [events-ch email password]
  (api-req
   POST
   "/login"
   {:email email
    :password password}
   #(enqueue-message events-ch [events/api-success-sign-in (select-sign-in-keys %)])))

(defn sign-up [events-ch email password password-confirmation]
  (api-req
   POST
   "/signup"
   {:email email
    :password password
    :password_confirmation password-confirmation}
   #(enqueue-message events-ch [events/api-success-sign-up (select-sign-in-keys %)])))

(defn forgot-password [events-ch email]
  (api-req
   POST
   "/forgot_password"
   {:email email}
   #(enqueue-message events-ch [events/api-success-forgot-password])))

(defn reset-password [events-ch password password-confirmation reset-token]
  (api-req
   POST
   "/reset_password"
   {:password password
    :password_confirmation password-confirmation
    :reset_password_token reset-token}
   #(enqueue-message events-ch [events/api-success-reset-password (select-sign-in-keys %)])))

(defn select-address-keys [m]
  (let [keys [:address1 :address2 :city :country_id :firstname :lastname :id :phone :state_id :zipcode]]
    (select-keys m keys)))

(defn rename-server-address-keys [m]
  (rename-keys m {:bill_address :billing-address
                  :ship_address :shipping-address}))

(defn get-account [events-ch id token]
  (api-req
   GET
   "/users"
   {:id id
    :token token}
   #(enqueue-message events-ch [events/api-success-account (rename-server-address-keys %)])))

(defn update-account [events-ch id email password password-confirmation token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :password password
    :password_confirmation password-confirmation
    :token token}
   #(enqueue-message events-ch [events/api-success-manage-account (select-sign-in-keys %)])))

(defn update-account-address [events-ch id email billing-address shipping-address token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :bill_address (select-address-keys billing-address)
    :ship_address (select-address-keys shipping-address)
    :token token}
   #(enqueue-message events-ch [events/api-success-account (rename-server-address-keys %)])))

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
                                 :stylist (select-stylist-account-keys %)}])))

(defn update-stylist-account [events-ch user-token stylist-account]
  (api-req
   PUT
   "/stylist"
   {:user-token user-token
    :stylist stylist-account}
   #(enqueue-message events-ch [events/api-success-stylist-manage-account
                                {:updated true
                                 :stylist (select-stylist-account-keys %)}])))

(defn update-stylist-account-profile-picture [events-ch user-token stylist-account]
  (let [form-data (doto (js/FormData.)
                    (.append "file"
                             (stylist-account :profile-picture)
                             (.-name (stylist-account :profile-picture)))
                    (.append "user-token" user-token))]
    (PUT (str api-base-url "/stylist/profile-picture")
         {:handler #(enqueue-message events-ch
                                     [events/api-success-stylist-manage-account-profile-picture
                                      (select-keys % [:profile_picture_url])])
          :error-handler #(js/console.error "stylist profile picture upload error " (clj->js %))
          :params form-data
          :response-format (json-response-format {:keywords? true})
          :timeout 10000})))

(defn get-stylist-commissions [events-ch user-token]
  (api-req
   GET
   "/stylist/commissions"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-stylist-commissions
                     (select-keys % [:rate :next-amount :paid-total :new-orders :payouts])])))

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
                                     :bonuses])])))

(defn get-stylist-referral-program [events-ch user-token]
  (api-req
   GET
   "/stylist/referrals"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-stylist-referral-program
                    (select-keys % [:sales-rep-email :bonus-amount :earning-amount :total-amount :referrals])])))

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

(defn create-order [events-ch user-token]
  (api-req
   POST
   "/orders"
   (if user-token {:token user-token} {})
   #(enqueue-message events-ch [events/api-success-create-order (select-keys % [:number :token])])))

(defn create-order-if-needed [events-ch order-id order-token user-token]
  (if (and order-token order-id)
    (enqueue-message events-ch [events/api-success-create-order {:number order-id :token order-token}])
    (create-order events-ch user-token)))

(defn update-cart [events-ch user-token {order-token :token :as order} extra-message-args]
  (api-req
   PUT
   "/cart"
   (filter-nil
    {:order (select-keys order [:number :line_items_attributes :coupon_code :email :user_id :state])
     :order_token order-token})
   #(enqueue-message events-ch [events/api-success-update-cart (merge {:order %} extra-message-args)])))

(defn update-order [events-ch user-token order extra-message-args]
  (api-req
   PUT
   "/orders"
   {:order (filter-nil (-> order
                           (select-keys [:number
                                         :bill_address
                                         :ship_address
                                         :shipments_attributes
                                         :payments_attributes])
                           (update-in [:bill_address] select-address-keys)
                           (update-in [:ship_address] select-address-keys)
                           (rename-keys {:bill_address :bill_address_attributes
                                         :ship_address :ship_address_attributes})))
    :state (:state order)
    :order_token (:token order)}
   #(enqueue-message events-ch [events/api-success-update-order (merge {:order %} extra-message-args)])))

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
                                                    :order-token order-token}])))

(defn get-order [events-ch order-number order-token]
  (api-req
   GET
   "/orders"
   {:id order-number
    :token order-token}
   #(enqueue-message events-ch [events/api-success-get-order %])))

(defn get-past-order [events-ch order-number user-token]
  (api-req
   GET
   "/orders"
   {:id order-number
    :token user-token}
   #(enqueue-message events-ch [events/api-success-get-past-order %])))

(defn get-my-orders [events-ch user-token]
  (api-req
   GET
   "/my_orders"
   {:user-token user-token}
   #(enqueue-message events-ch [events/api-success-my-orders %])))

(defn observe-events [f events-ch & args]
  (let [broadcast-ch (chan)
        mult-ch (mult broadcast-ch)
        result-ch (chan)]
    (tap mult-ch events-ch false)
    (tap mult-ch result-ch)
    (apply f broadcast-ch args)
    result-ch))

(defn add-to-bag [events-ch variant-id variant-quantity order-token order-id user-token]
  (go
    (let [[_ {order-id :number order-token :token}] (<! (observe-events create-order-if-needed events-ch order-id order-token user-token))]
      (<! (observe-events add-line-item events-ch variant-id variant-quantity order-id order-token))
      (get-order events-ch order-id order-token))))
