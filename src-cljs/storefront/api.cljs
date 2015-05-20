(ns storefront.api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [ajax.core :refer [GET POST PUT json-response-format]]
            [cljs.core.async :refer [put! take! chan <! mult tap]]
            [storefront.events :as events]
            [storefront.taxons :refer [taxon-name-from]]))

(def base-url "http://localhost:3005")
(def send-sonar-base-url "https://www.sendsonar.com/api/v1")
(def send-sonar-publishable-key "d7d8f2d0-9f91-4507-bc82-137586d41ab8")

(defn api-req [method path params success-handler]
  (method (str base-url path)
          {:handler success-handler
           :error-handler #(js/console.error (clj->js %))
           :headers {"Accepts" "application/json"}
           :format :json
           :params params
           :response-format (json-response-format {:keywords? true})}))

(defn get-taxons [events-ch]
  (api-req
   GET
   "/product-nav-taxonomy"
   {}
   #(put! events-ch [events/api-success-taxons (select-keys % [:taxons])])))

(defn get-store [events-ch store-slug]
  (api-req
   GET
   "/stylist"
   {:store_slug store-slug}
   #(put! events-ch [events/api-success-store %])))

(defn get-products [events-ch taxon-path]
  (api-req
   GET
   "/products"
   {:taxon_name (taxon-name-from taxon-path)}
   #(put! events-ch [events/api-success-products (merge (select-keys % [:products])
                                                        {:taxon-path taxon-path})])))

(defn get-product [events-ch product-path]
  (api-req
   GET
   (str "/products")
   {:slug product-path}
   #(put! events-ch [events/api-success-product {:product-path product-path
                                                 :product %}])))

(defn select-sign-in-keys [args]
  (select-keys args [:email :token :store_slug :id]))

(defn sign-in [events-ch email password]
  (api-req
   POST
   "/login"
   {:email email
    :password password}
   #(put! events-ch [events/api-success-sign-in (select-sign-in-keys %)])))

(defn sign-up [events-ch email password password-confirmation]
  (api-req
   POST
   "/signup"
   {:email email
    :password password
    :password_confirmation password-confirmation}
   #(put! events-ch [events/api-success-sign-up (select-sign-in-keys %)])))

(defn forgot-password [events-ch email]
  (api-req
   POST
   "/forgot_password"
   {:email email}
   #(put! events-ch [events/api-success-forgot-password])))

(defn reset-password [events-ch password password-confirmation reset-token]
  (api-req
   POST
   "/reset_password"
   {:password password
    :password_confirmation password-confirmation
    :reset_password_token reset-token}
   #(put! events-ch [events/api-success-reset-password (select-sign-in-keys %)])))

(defn update-account [events-ch id email password password-confirmation token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :password password
    :password_confirmation password-confirmation
    :token token}
   #(put! events-ch [events/api-success-manage-account (select-sign-in-keys %)])))

(defn update-account-address [events-ch id email billing-address shipping-address token]
  (api-req
   PUT
   "/users"
   {:id id
    :email email
    :billing_address billing-address
    :shipping_address shipping-address
    :token token}
   #(put! events-ch [events/api-success-account-addresses (select-sign-in-keys %)])))

(defn get-stylist-commissions [events-ch user-token]
  (api-req
   GET
   "/stylist/commissions"
   {:user-token user-token}
   #(put! events-ch [events/api-success-stylist-commissions
                     (select-keys % [:rate :next-amount :paid-total :new-orders :payouts])])))

(defn get-stylist-bonus-credits [events-ch user-token]
  (api-req
   GET
   "/stylist/bonus-credits"
   {:user-token user-token}
   #(put! events-ch [events/api-success-stylist-bonus-credits
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
   #(put! events-ch [events/api-success-stylist-referral-program
                    (select-keys % [:sales-rep-email :bonus-amount :earning-amount :total-amount :referrals])])))

(defn get-sms-number [events-ch]
  (letfn [(normalize-number [x] ;; smooth out send-sonar's two different number formats
            (apply str (if (= "+" (first x))
                         (drop 3 x)
                         x)))
          (callback [resp]
            (put! events-ch
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
   #(put! events-ch [events/api-success-create-order (select-keys % [:number :token])])))

(defn create-order-if-needed [events-ch order-id order-token user-token]
  (if (and order-token order-id)
    (put! events-ch [events/api-success-create-order {:number order-id :token order-token}])
    (create-order events-ch user-token)))

(defn update-order [events-ch user-token {guest-token :token :as order}]
  (api-req
   PUT
   "/orders"
   {:order (select-keys order [:number :bill_address :ship_address])
    :order_token guest-token}
   #(put! events-ch [events/api-success-update-order %])))

(defn add-line-item [events-ch variant-id variant-quantity order-number order-token]
  (api-req
   POST
   "/line-items"
   {:token order-token
    :order_id order-number
    :variant_id variant-id
    :variant_quantity variant-quantity}
   #(put! events-ch [events/api-success-add-to-bag {:variant-id variant-id
                                                    :variant-quantity variant-quantity
                                                    :order-number order-number
                                                    :order-token order-token}])))

(defn get-order [events-ch order-number order-token]
  (api-req
   GET
   "/orders"
   {:id order-number
    :token order-token}
   #(put! events-ch [events/api-success-get-order %])))

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
