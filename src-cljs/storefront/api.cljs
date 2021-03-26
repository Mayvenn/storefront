(ns storefront.api
  (:require [ajax.core :refer [GET POST PUT] :as ajax]
            [ajax.protocols :refer [-body]]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            [storefront.routes :as routes]
            [storefront.cache :as c]
            [storefront.config :refer [api-base-url] :as config]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [catalog.skuers :as skuers]
            [spice.maps :as maps]
            [clojure.set :as set]
            [storefront.accessors.promos :as promos]))

(defn is-rails-style? [resp]
  (or (seq (:error resp))
      (seq (:errors resp))))

(defn is-rails-exception? [resp]
  (seq (:exception resp)))

(defn convert-to-paths [errors]
  (for [[error-key error-messages] errors
        error-message error-messages]
    {:path (string/split (name error-key) #"\.")
     :long-message error-message}))

(defn rails-style->std-error [{:keys [error errors]}]
  {:error-message (or error "Something went wrong. Please refresh and try again or contact customer service.")
   :error-code (if errors "invalid-input" "generic-error")
   :field-errors (when errors (convert-to-paths (maps/kebabify errors)))})

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

(defn ^:private slow-json->clj [json]
  (js->clj (js/JSON.parse json) :keywordize-keys true))

;; (defn json-response-format-with-app-version [config]
;;   (let [default (ajax/json-response-format config)
;;         read-json (:read default)]
;;     (assoc default :read (fn [xhrio]
;;                            {:body (read-json xhrio)
;;                             :app-version (app-version xhrio)}))))

(defn json-response-format-with-app-version [config]
  (let [default   (ajax/json-response-format config)
        parser    (:parser config slow-json->clj)]
    (assoc default
           :content-type ["application/json"]
           :description "JSON"
           :read (fn [xhrio]
                   {:body        (parser (-body xhrio))
                    :app-version (app-version xhrio)}))))

(def default-req-opts {:format :json
                       :response-format (json-response-format-with-app-version {:keywords? true})})

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

(defn ^:private set->vec
  "In newer releases of cljs-ajax sets are serialized via str, which is less than ideal for us.
  We should never send sets as set semantics cannot be enforced in query params.
  So as a hack, we are simply making sets vecs.
  Using a more capable format such as transit or edn would also work."
  [params]
  (walk/postwalk #(cond (set? %) (vec %)
                        :else %)
                 params))

(defn api-request [method url req-key request-opts]
  (let [request-opts (-> request-opts
                         (update-in [:params] @#'maps/remove-nils)
                         (update-in [:params] set->vec))
        req-id       (str (random-uuid))
        request      (method url (merge-req-opts req-key req-id request-opts))]
    (messages/handle-message events/api-start {:xhr         request
                                               :request-key req-key
                                               :request-id  req-id})))

(defn storeback-api-req [method path req-key request-opts]
  (api-request method (str api-base-url path) req-key request-opts))

(defn fetch-cms-keypath
  [keypath handler]
  (let [uri-path (str "/cms/" (string/join "/" (map name keypath)))]
    (api-request GET uri-path
                 request-keys/fetch-cms-keypath
                 {:handler (fn [result]
                             (messages/handle-message events/api-success-fetch-cms-keypath result)
                             (handler result))})))

(defn cache-req
  [cache method path req-key {:keys [handler params cache/bypass?] :as request-opts}]
  (let [key (c/cache-key [path params])
        res (cache key)]
    (if (and res (not bypass?))
      (handler res)
      (storeback-api-req method
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
  (into {}
        (map (fn [[k v]]
               [(if-let [ns (namespace k)]
                  (str ns "/" (name k))
                  (name k))
                v]))
        criteria))

(defn get-products [cache criteria-or-id handler]
  (cache-req
   cache
   GET
   "/v3/products"
   (conj request-keys/get-products criteria-or-id)
   {:params (if (map? criteria-or-id)
              (criteria->query-params criteria-or-id)
              {:id criteria-or-id})
    :handler #(handler (-> %
                           (update :skus (fn [skus]
                                           (into {}
                                                 (map (fn [[k v]]
                                                        [(name k) (skuers/->skuer v)]))
                                                 skus)))
                           (update :images (fn [images]
                                             (into {}
                                                   (map (fn [[k v]]
                                                          [(name k) v]))
                                                   images)))))}))

(defn get-skus [cache criteria-or-id handler]
  (cache-req
   cache
   GET
   "/v2/skus"
   (conj request-keys/get-skus criteria-or-id)
   {:params (criteria->query-params criteria-or-id)
    :handler handler}))

(defn get-saved-cards [user-id user-token]
  (storeback-api-req
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
  (select-keys user
               [:email
                :token
                :store-slug
                :id
                :is-new-user
                :must-set-password
                :store-id
                :stylist-experience]))

(defn select-auth-keys [args]
  (-> args
      (update :user select-user-keys)
      (select-keys [:user :order])))

(defn sign-out [session-id browser-id user-id user-token]
  (storeback-api-req
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
  (storeback-api-req
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
                              (-> (update % :order orders/TEMP-pretend-service-items-do-not-exist)
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn facebook-sign-in [session-id browser-id uid access-token stylist-id order-number order-token]
  (storeback-api-req
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
      (let [auth-keys (-> response
                          (update :order orders/TEMP-pretend-service-items-do-not-exist)
                          select-auth-keys
                          (assoc :flow "facebook"))
            ;; Since we use facebook sign-in for both sign-in and sign-up, we
            ;; need to trigger the appropriate event. Diva tells us when this
            ;; flow has created a new user.
            new-user? (get-in auth-keys [:user :is-new-user])
            success-event (if new-user? events/api-success-auth-sign-up events/api-success-auth-sign-in)]
        (messages/handle-message success-event auth-keys)))}))

(defn sign-up [session-id browser-id email password stylist-id order-number order-token]
  (storeback-api-req
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
                                  (update :order orders/TEMP-pretend-service-items-do-not-exist)
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn reset-password [session-id browser-id password reset-token order-number order-token stylist-id]
  (storeback-api-req
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
                                  (update :order orders/TEMP-pretend-service-items-do-not-exist)
                                  select-auth-keys
                                  (assoc :flow "email-password")))}))

(defn facebook-reset-password [session-id browser-id uid access-token reset-token order-number order-token stylist-id]
  (storeback-api-req
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
                                  (update :order orders/TEMP-pretend-service-items-do-not-exist)
                                  select-auth-keys
                                  (assoc :flow "facebook")))}))

(defn forgot-password [session-id email]
  (storeback-api-req
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
      (dissoc :country-id)
      (set/rename-keys {:firstname :first-name
                        :lastname  :last-name
                        :address-1 :address1
                        :address-2 :address2})
      (update-in [:state] :abbr)
      (select-keys [:address1 :address2 :city :first-name :last-name :phone :state :zipcode])))

(defn diva->mayvenn-addresses [contains-addresses]
  (-> contains-addresses
      (set/rename-keys {:bill-address                 :billing-address
                        :ship-address                 :shipping-address
                        :total_available_store_credit :total-available-store-credit})
      (update-in [:billing-address] diva->mayvenn-address)
      (update-in [:shipping-address] diva->mayvenn-address)))

(defn get-account [id token]
  (storeback-api-req
   GET
   "/users"
   request-keys/get-account
   {:params
    {:id    id
     :token token}
    :handler
    #(messages/handle-message events/api-success-account
                              (diva->mayvenn-addresses %))}))

(defn update-account [session-id id email password token]
  (storeback-api-req
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

(defn force-set-password
  [{:as params :keys [session-id id password token]} handler]
  (storeback-api-req
   PUT "/users"
   request-keys/update-account
   {:params params :handler handler}))

(defn select-stylist-account-keys [args]
  (let [args    (get args :stylist args)
        stylist (select-keys args
                             [:portrait
                              :chosen-payout-method
                              :venmo-payout-attributes
                              :paypal-payout-attributes
                              :green-dot-payout-attributes
                              :instagram-account
                              :styleseat-account
                              :user
                              :address])]
    (assoc stylist :original-payout-method (:chosen-payout-method stylist))))

(defn get-stylist-account [user-id user-token stylist-id]
  (storeback-api-req
   GET
   "/v2/stylist"
   request-keys/get-stylist-account
   {:params {:user-id    user-id
             :user-token user-token
             :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-stylist-account
                              {:stylist (select-stylist-account-keys %)})}))

(defn get-shipping-methods []
  (storeback-api-req
   GET
   "/v2/shipping-methods"
   request-keys/get-shipping-methods
   {:handler #(messages/handle-message events/api-success-shipping-methods
                                       (update-in % [:shipping-methods] reverse))}))

(defn update-stylist-account [session-id user-id user-token stylist-id stylist-account success-event]
  (storeback-api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account
   {:params {:session-id session-id
             :stylist-id stylist-id
             :user-id    user-id
             :user-token user-token
             :stylist    stylist-account}
    :handler
    #(messages/handle-message success-event {:stylist (select-stylist-account-keys %)})}))

(defn update-stylist-account-portrait [session-id user-id user-token stylist-id stylist-account]
  (storeback-api-req
   PUT
   "/stylist"
   request-keys/update-stylist-account-portrait
   {:params {:session-id session-id
             :stylist-id stylist-id
             :user-id    user-id
             :user-token user-token
             :stylist    stylist-account}
    :handler
    #(messages/handle-message events/api-success-stylist-account-portrait
                              {:stylist  (select-keys % [:portrait])
                               :updated? true})}))

(defn refresh-stylist-portrait [user-id user-token stylist-id]
  (storeback-api-req
   GET
   "/v2/stylist"
   request-keys/refresh-stylist-portrait
   {:params
    {:user-id    user-id
     :user-token user-token
     :stylist-id stylist-id}
    :handler
    #(messages/handle-message events/api-success-stylist-account-portrait
                              {:stylist (select-keys % [:portrait])})}))

(defn append-stylist-gallery [user-id user-token {:keys [gallery-urls]}]
  (storeback-api-req
   POST
   "/gallery"
   request-keys/append-gallery
   {:params {:user-id    user-id
             :user-token user-token
             :urls       gallery-urls}
    :handler
    #(messages/handle-message events/api-success-stylist-gallery-append %)}))

(defn get-stylist-gallery [params]
  (storeback-api-req
   GET
   "/gallery"
   request-keys/get-stylist-gallery
   {:params (select-keys params [:user-id :user-token])
    :handler
    #(messages/handle-message events/api-success-stylist-gallery-fetch %)}))

(defn get-store-gallery [params]
  (storeback-api-req
   GET
   "/gallery"
   request-keys/get-store-gallery
   {:params
    (select-keys params [:stylist-id])
    :handler
    #(messages/handle-message events/api-success-store-gallery-fetch %)}))

(defn delete-gallery-image [user-id user-token image-url]
  (storeback-api-req
   POST
   "/gallery/images/delete"
   request-keys/delete-gallery-image
   {:params
    {:user-id    user-id
     :user-token user-token
     :image-url  image-url}
    :handler
    #(messages/handle-message events/api-success-stylist-gallery-delete %)}))

(defn get-stylist-balance-transfer [user-id user-token balance-transfer-id]
  (storeback-api-req
   GET
   (str "/v1/stylist/balance-transfers/" balance-transfer-id)
   request-keys/get-stylist-balance-transfer
   {:params
    {:user-id user-id :user-token user-token}
    :handler
    (partial messages/handle-message events/api-success-stylist-balance-transfer-details)}))

(defn get-stylist-payout-stats
  [event stylist-id user-id user-token]
  (storeback-api-req
   GET
   "/v1/stylist/payout-stats"
   request-keys/get-stylist-payout-stats
   {:params  {:stylist-id stylist-id
              :user-id    user-id
              :user-token user-token}
    :handler #(messages/handle-message event
                                       (select-keys % [:lifetime-stats :next-payout :previous-payout :initiated-payout]))}))

(defn get-stylist-dashboard-stats
  [event stylist-id user-id user-token]
  (storeback-api-req
   GET
   "/v2/stylist/stats"
   request-keys/get-stylist-dashboard-stats
   {:params {:stylist-id stylist-id
             :user-id    user-id
             :user-token user-token}
    :handler #(messages/handle-message event
                                       (select-keys % [:earnings :services :store-credit-balance :bonuses]))}))

(defn get-stylist-dashboard-balance-transfers
  [stylist-id user-id user-token {:keys [page per]} handler]
  (storeback-api-req
   GET
   "/v2/stylist/balance-transfers"
   request-keys/get-stylist-dashboard-balance-transfers
   {:params {:stylist-id stylist-id
             :user-id    user-id
             :user-token user-token
             :page       page
             :per        per}
    :handler handler}))

(defn get-stylist-dashboard-sales
  [stylist-id user-id user-token {:keys [page per]} handler]
  (storeback-api-req
   GET
   "/v2/stylist/sales"
   request-keys/get-stylist-dashboard-sales
   {:params {:stylist-id stylist-id
             :user-id    user-id
             :user-token user-token
             :page       page
             :per        per}
    :handler handler}))

(defn get-stylist-dashboard-sale
  [{:keys [stylist-id user-id user-token order-number handler]}]
  (storeback-api-req
   GET
   "/v2/stylist/sale"
   request-keys/get-stylist-dashboard-sale
   {:params {:stylist-id stylist-id
             :user-id    user-id
             :user-token user-token
             :order-number order-number}
    :handler handler}))

(defn cash-out-commit
  [user-id user-token stylist-id]
  (storeback-api-req
   POST
   "/v1/stylist/cash-out"
   request-keys/cash-out-commit
   {:params  {:user-id    user-id
              :user-token user-token
              :stylist-id stylist-id}
    :handler #(messages/handle-message events/api-success-cash-out-commit
                                       (select-keys % [:status-id :balance-transfer-id :amount :payout-method]))}))

(defn cash-out-status
  [user-id user-token status-id stylist-id]
  (storeback-api-req
   GET
   "/v1/stylist/cash-out"
   request-keys/cash-out-status
   {:params  {:user-id    user-id
              :user-token user-token
              :status-id  status-id
              :stylist-id stylist-id}
    :handler #(messages/handle-message events/api-success-cash-out-status
                                       (select-keys % [:status :status-id :balance-transfer-id :amount :payout-method]))}))

(defn place-order
  ([session-id order utm-params affiliate-stylist-id]
   (place-order session-id order utm-params affiliate-stylist-id nil))
  ([session-id order utm-params affiliate-stylist-id {:as handlers :keys [error-handler success-handler]}]
   (let [default-success-handler #(messages/handle-message events/api-success-update-order-place-order
                                                           {:order %})]
     (storeback-api-req
      POST
      "/v2/place-order"
      request-keys/place-order
      {:params        (merge (select-keys order [:number :token])
                             {:session-id session-id
                              :utm-params utm-params}
                             (when affiliate-stylist-id
                               {:stylist-id affiliate-stylist-id}))
       :handler       (comp (or success-handler default-success-handler) orders/TEMP-pretend-service-items-do-not-exist)
       :error-handler (or error-handler default-error-handler)}))))

(defn ^:private remove-from-bag [request-key session-id {:keys [variant-id number token quantity]} handler]
  (storeback-api-req
   POST
   "/v2/remove-from-bag"
   request-key
   {:params {:session-id session-id
             :quantity quantity
             :variant-id variant-id
             :number number
             :token token}
    :handler (comp handler orders/TEMP-pretend-service-items-do-not-exist)}))

(defn remove-line-item [session-id {:keys [variant-id number token sku-code]} handler]
  (remove-from-bag
   (conj request-keys/update-line-item sku-code)
   session-id {:variant-id variant-id
               :number number
               :token token
               :quantity 1}
   handler))

(defn remove-freeinstall-line-item
  ([session-id order]
   (remove-freeinstall-line-item session-id order
                                 #(messages/handle-message events/api-success-remove-from-bag {:order %})))
  ([session-id {:keys [number token] :as order} success-handler]
   ;; TODO(corey) this is domain logic inside the api effect
   (let [mayvenn-install-line-item-variant-id (->> order
                                                   orders/service-line-items
                                                   (filter line-items/mayvenn-install-service?)
                                                   first
                                                   :id)]
     (remove-from-bag
      request-keys/remove-freeinstall-line-item
      session-id {:variant-id mayvenn-install-line-item-variant-id
                  :number     number
                  :token      token
                  :quantity   1}
      success-handler))))

(defn delete-line-item [session-id order variant-id]
  (remove-from-bag
   (conj request-keys/delete-line-item variant-id)
   session-id
   (merge (select-keys order [:number :token])
          {:session-id session-id
           :variant-id variant-id
           :quantity (->> order
                          orders/product-and-service-items
                          (orders/line-item-by-id variant-id)
                          :quantity)})
   #(messages/handle-message events/api-success-remove-from-bag {:order %})))

(defn update-addresses [session-id order]
  (storeback-api-req
   POST
   "/v2/update-addresses"
   request-keys/update-addresses
   {:params (-> order
                (select-keys [:number :token :billing-address :shipping-address :phone-marketing-opt-in])
                (update :shipping-address dissoc :latitude :longitude)
                (update :billing-address dissoc :latitude :longitude)
                (assoc :session-id session-id))
    :handler #(messages/handle-message events/api-success-update-order-update-address
                                       {:order (orders/TEMP-pretend-service-items-do-not-exist %)
                                        :navigate events/navigate-checkout-payment})}))

(defn guest-update-addresses [session-id order]
  (storeback-api-req
   POST
   "/v2/guest-update-addresses"
   request-keys/update-addresses
   {:params (-> order
                (select-keys [:number :token :email :billing-address :shipping-address :phone-marketing-opt-in])
                (update :shipping-address dissoc :latitude :longitude)
                (update :billing-address dissoc :latitude :longitude)
                (assoc :session-id session-id))
    :handler #(messages/handle-message events/api-success-update-order-update-guest-address
                                       {:order (orders/TEMP-pretend-service-items-do-not-exist %)
                                        :navigate events/navigate-checkout-payment})}))

(defn browser-pay-estimate [params successful-estimate failed-to-estimate]
  (POST
    (str api-base-url "/apple-pay-estimate")
    (merge default-req-opts
           {:params  params
            :handler (comp successful-estimate orders/TEMP-pretend-service-items-do-not-exist :body)
            :error-handler failed-to-estimate})))

(defn checkout [params successful-checkout failed-checkout]
  (storeback-api-req
   POST
   "/checkout"
   request-keys/checkout
   {:params  params
    :handler (juxt successful-checkout #(messages/handle-message events/api-success-update-order-place-order
                                                                 {:order (orders/TEMP-pretend-service-items-do-not-exist %)}))
    :error-handler (juxt failed-checkout default-error-handler)}))

(defn update-shipping-method [session-id order]
  (storeback-api-req
   POST
   "/v2/update-shipping-method"
   request-keys/update-shipping-method
   {:params (-> order
                (select-keys [:number :token :shipping-method-sku])
                (assoc :session-id session-id))
    :handler #(messages/handle-message events/api-success-update-order-update-shipping-method
                                       {:order (orders/TEMP-pretend-service-items-do-not-exist %)})}))

;;TODO This needs some reworking, it has an awkward api
(defn update-cart-payments
  ([session-id {:keys [order] :as args}]
   (update-cart-payments session-id args #(messages/handle-message events/api-success-update-order-update-cart-payments
                                                                   (merge args {:order %}))))
  ([session-id {:keys [order] :as args} success-handler]
   (storeback-api-req
    POST
    "/v2/update-cart-payments"
    request-keys/update-cart-payments
    {:params (-> order
                 (select-keys [:number :token :cart-payments])
                 (assoc :session-id session-id))
     :handler (comp success-handler orders/TEMP-pretend-service-items-do-not-exist)})))

(defn get-order [number token]
  (storeback-api-req
   GET
   (str "/v2/orders/" number)
   request-keys/get-order
   {:params
    {:token token}
    :handler
    #(messages/handle-message events/api-success-get-order (orders/TEMP-pretend-service-items-do-not-exist %))}))

(defn confirm-order-was-placed
  [session-id order utm-params success-handler error-handler]
  (storeback-api-req
   POST
   "/v2/quadpay-confirm-charge"
   request-keys/place-order
   {:params        (merge (select-keys order [:number :token])
                          {:session-id session-id
                           :utm-params utm-params})
    :handler       (comp success-handler orders/TEMP-pretend-service-items-do-not-exist)
    :error-handler (or error-handler default-error-handler)}))

(defn poll-order
  [number token handler]
  (storeback-api-req GET (str "/v2/orders/" number)
                     request-keys/get-order
                     {:params  {:token token}
                      :handler (comp handler orders/TEMP-pretend-service-items-do-not-exist)}))

(defn get-completed-order [number token]
  (storeback-api-req
   GET
   (str "/v2/orders/" number)
   request-keys/get-order
   {:params
    {:token token}
    :handler
    #(messages/handle-message events/api-success-get-completed-order
                              (orders/TEMP-pretend-service-items-do-not-exist %))}))

(defn get-current-order [user-id user-token store-stylist-id]
  (storeback-api-req
   GET
   "/v2/current-order-for-user"
   request-keys/get-order
   {:params
    {:user-id user-id
     :user-token user-token
     :store-stylist-id store-stylist-id}
    :handler #(messages/handle-message events/api-success-get-order (orders/TEMP-pretend-service-items-do-not-exist %))
    :error-handler (constantly nil)}))

(defn add-promotion-code
  [{:keys [session-id number token promo-code allow-dormant?]}]
  (storeback-api-req
   POST
   "/v2/add-promotion-code"
   request-keys/add-promotion-code
   {:params        {:session-id    session-id
                    :number        number
                    :token         token
                    :code          promo-code
                    :allow-dormant allow-dormant?}
    :handler       #(messages/handle-message events/api-success-update-order-add-promotion-code
                                             {:order          (orders/TEMP-pretend-service-items-do-not-exist %)
                                              :promo-code     promo-code
                                              :allow-dormant? allow-dormant?})
    :error-handler #(if allow-dormant?
                      (messages/handle-message events/api-failure-pending-promo-code %)
                      (let [{:keys [error-code] :as response-body} (get-in % [:response :body])]
                        (when (and (waiter-style? response-body)
                                   (#{"ineligible-with-free-install-promotion"
                                      "promotion-not-found"
                                      "stylist-wrong-store-promotion"
                                      "stylist-only-promotion"} error-code))
                          (messages/handle-message events/api-failure-errors-invalid-promo-code
                                                   (assoc (waiter-style->std-error response-body) :promo-code promo-code)))))}))

(defn add-sku-to-bag [session-id {:keys [token number sku heat-feature-flags] :as params} handler]
  (storeback-api-req
   POST
   "/v2/add-to-bag"
   (conj request-keys/add-to-bag (:catalog/sku-id sku))
   {:params (merge (select-keys params [:quantity :stylist-id :user-id :user-token])
                   {:session-id session-id
                    :sku (:catalog/sku-id sku)}
                   (when heat-feature-flags {:heat-feature-flags heat-feature-flags})
                   (when (and token number) {:token token :number number}))
    :handler (comp handler orders/TEMP-pretend-service-items-do-not-exist)}))

(defn add-skus-to-bag [session-id {:keys [token number sku-id->quantity heat-feature-flags] :as params} handler]
  (storeback-api-req
   POST
   "/v2/bulk-add-to-bag"
   (conj request-keys/add-to-bag (set (keys sku-id->quantity)))
   {:params  (merge (select-keys params [:stylist-id])
                    {:session-id       session-id
                     :sku-id->quantity sku-id->quantity}
                    (when heat-feature-flags {:heat-feature-flags heat-feature-flags})
                    (when (and token number) {:token token :number number}))
    :handler (fn [order]
               (handler {:order            (orders/TEMP-pretend-service-items-do-not-exist order)
                         :sku-id->quantity sku-id->quantity}))}))

(defn remove-promotion-code [session-id {:keys [token number]} promo-code handler]
  (storeback-api-req
   POST
   "/v2/remove-promotion-code"
   request-keys/remove-promotion-code
   {:params {:session-id session-id
             :number number :token token :code promo-code}
    :handler (comp handler orders/TEMP-pretend-service-items-do-not-exist)}))

(defn create-shared-cart [session-id order-number order-token]
  (storeback-api-req
   POST
   "/v2/create-shared-cart"
   request-keys/create-shared-cart
   {:params  {:session-id session-id
              :order-number order-number
              :order-token  order-token}
    :handler #(messages/handle-message events/api-success-shared-cart-create
                                       {:cart %})}))

(defn fetch-shared-cart [shared-cart-id]
  (storeback-api-req
   GET
   "/fetch-shared-cart"
   request-keys/fetch-shared-cart
   {:params  {:shared-cart-id shared-cart-id}
    :handler #(messages/handle-message events/api-success-shared-cart-fetch %)}))

(defn fetch-shared-carts [cache cart-ids]
  (cache-req
   cache
   GET
   "/fetch-shared-carts"
   request-keys/fetch-shared-carts
   {:params  {:cart-ids (string/join "," cart-ids)}
    :handler #(messages/handle-message events/api-success-shared-carts-fetch %)}))

(defn create-order-from-shared-cart
  [params success-handler error-handler]
  (storeback-api-req
   POST
   "/create-order-from-shared-cart"
   request-keys/create-order-from-shared-cart
   {:params        params
    :handler       success-handler
    :error-handler error-handler}))

(defn create-order-from-look
  [session-id shared-cart-id look-id user-id user-token stylist-id servicing-stylist-id cart-interstitial?]
  (create-order-from-shared-cart
   {:session-id           session-id
    :shared-cart-id       shared-cart-id
    :user-id              user-id
    :user-token           user-token
    :stylist-id           stylist-id
    :servicing-stylist-id servicing-stylist-id}
   #(messages/handle-message events/api-success-update-order-from-shared-cart
                             {:order          (orders/TEMP-pretend-service-items-do-not-exist %)
                              :look-id        look-id
                              :shared-cart-id shared-cart-id
                              :navigate       (if cart-interstitial?
                                                events/navigate-added-to-cart
                                                events/navigate-cart)})
   #(do
      ;; NOTE: Order is important here, for correct display of errors
      (default-error-handler %)
      (messages/handle-message events/api-failure-order-not-created-from-shared-cart))))

(defn assign-servicing-stylist
  "Assigns a servicing stylist to an order, or creates such an order if no order number given"
  [servicing-stylist-id stylist-id number token heat-feature-flags handler]
  (storeback-api-req
   POST
   "/v2/assign-servicing-stylist"
   request-keys/assign-servicing-stylist
   {:params  (merge {:stylist-id           stylist-id
                     :servicing-stylist-id servicing-stylist-id
                     :number               number
                     :token                token}
                    (when heat-feature-flags {:heat-feature-flags heat-feature-flags}))
    :handler (comp handler orders/TEMP-pretend-service-items-do-not-exist)}))

(defn remove-servicing-stylist
  [servicing-stylist-id number token]
  (storeback-api-req
   POST
   "/v2/remove-servicing-stylist"
   request-keys/remove-servicing-stylist
   {:params {:servicing-stylist-id servicing-stylist-id
             :number               number
             :token                token}
    :handler  #(messages/handle-message
                events/api-success-update-order-remove-servicing-stylist
                {:order (orders/TEMP-pretend-service-items-do-not-exist %)})}))

(defn- static-content-req [method path req-key {:keys [handler] :as request-opts}]
  (let [req-id       (str (random-uuid))
        content-opts {:format          :raw
                      :handler         (wrap-api-end req-key req-id handler)
                      :response-format (ajax/raw-response-format)}
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

(defn voucher-redemption [voucher-code stylist-id]
  (storeback-api-req
   POST
   "/redeem-voucher"
   request-keys/voucher-redemption
   {:params  {:voucher-code voucher-code
              :stylist-id   stylist-id}
    :handler         #(messages/handle-message events/api-success-voucher-redemption %)
    :error-handler   #(messages/handle-message events/api-failure-voucher-redemption %)}))

(defn fetch-user-stylist-service-menu [cache {:as params :keys [user-id user-token stylist-id]}]
  (cache-req
   cache
   GET
   "/v1/stylist/service-menu"
   request-keys/fetch-user-stylist-service-menu
   {:params  {:stylist-id stylist-id
              :user-id    user-id
              :user-token user-token}
    :handler #(messages/handle-message events/api-success-user-stylist-service-menu-fetch %)}))

(defn presearch-name
  [params handler]
  (storeback-api-req GET "/v1/stylist/presearch"
                     request-keys/presearch-name
                     {:params  params
                      :handler handler}))

(defn fetch-stylists-matching-filters [params handler]
  (storeback-api-req
   GET
   "/v2/stylist/within-radius"
   request-keys/fetch-stylists-matching-filters
   {:params  params
    :handler (or handler
                 #(messages/handle-message events/api-success-fetch-stylists-matching-filters
                                           %))}))

(defn fetch-matched-stylist
  ([cache stylist-id]
   (fetch-matched-stylist cache stylist-id nil))
  ([cache stylist-id {:keys [error-handler cache/bypass? success-handler]}]
   (cache-req
    cache
    GET
    "/v1/stylist/matched-by-id"
    request-keys/fetch-matched-stylist
    {:params        {:stylist-id stylist-id}
     :handler       success-handler
     :error-handler error-handler
     :cache/bypass? bypass?})))

(defn fetch-matched-stylists [cache stylist-ids handler]
  (cache-req
   cache
   GET
   "/v1/stylist/matched-by-ids"
   request-keys/fetch-matched-stylist
   {:params  {:stylist-ids stylist-ids}
    :handler handler}))

(defn fetch-stylist-reviews
  [cache {:as params :keys [stylist-id page]}]
  (cache-req
   cache
   GET
   "/v1/stylist/reviews"
   request-keys/fetch-stylist-reviews
   {:params        {:stylist-id stylist-id
                    :per-page   5
                    :page       page}
    :handler       #(messages/handle-message events/api-success-fetch-stylist-reviews %)
    :error-handler #(messages/handle-message events/api-failure-fetch-stylist-reviews %)}))

(defn add-servicing-stylist-and-sku
  [session-id {:as params :keys [token number sku servicing-stylist heat-feature-flags]} handler]
  (storeback-api-req
   POST "/add-servicing-stylist-and-sku"
   (conj request-keys/add-to-bag (:catalog/sku-id sku))
   {:params  (merge (select-keys params [:quantity :stylist-id :user-id :user-token])
                    {:session-id           session-id
                     :sku                  (:catalog/sku-id sku)
                     :servicing-stylist-id (:stylist/id servicing-stylist)}
                    (when heat-feature-flags {:heat-feature-flags heat-feature-flags})
                    (when (and token number) {:token token :number number}))
    :handler (comp handler
                   orders/TEMP-pretend-service-items-do-not-exist)}))
