(ns storefront.system.wirewheel
  (:require [tugboat.core :as tugboat]
            [storefront.system.scheduler :as scheduler]
            [com.stuartsierra.component :as component]
            [cheshire.core :as json])
  (:import java.util.Base64))

(defn auth-token-request
  [{:keys [base-url path client-id client-secret exception-handler logger]}]
  (try
    (tugboat/request {:endpoint base-url
                      :logger   logger}
                     :post
                     path
                     {:socket-timeout 30000
                      :conn-timeout   30000
                      :headers        {"Content-Type"  "application/x-www-form-urlencoded"
                                       "Authorization" (->> (str client-id ":" client-secret)
                                                            .getBytes
                                                            (.encodeToString (Base64/getEncoder))
                                                            (str "Basic "))}
                      :query-params   {"grant_type" "client_credentials"
                                       "scope"      "consent-insert-api consent-get-api"}
                      :body           (json/generate-string {"access_token" nil})})
    (catch Throwable e
      (logger :error e)
      (exception-handler e)
      nil)))

(defn ^:private api-request [{:keys [base-url method auth-token path exception-handler body logger]}]
  (try
    (tugboat/request {:endpoint base-url}
                     method
                     path
                     (merge {:socket-timeout 30000
                             :conn-timeout   30000
                             :as             :json
                             :headers        {"Authorization" (str "Bearer " auth-token)
                                              "Content-Type"  "application/json"}}
                            (when body {:body (json/generate-string body)})))
    (catch Throwable e
      (logger :error e)
      (exception-handler e)
      nil)))

(defn consents-fetch-request
  [{:keys [base-url auth-token verified-id exception-handler] :as opts}]
  (api-request (merge (select-keys opts [:base-url :auth-token :exception-handler :logger])
                      {:method :get
                       :path   (str "/v2/consents/unified/" verified-id)})))

;; Consents should be a collection of maps with the following keys:
;; * target [The name of the permission]
;; * vendor [???]
;; * action [either "ACCEPT" or "REJECT"]
(defn consents-set-request
  [{:keys [verified-id consents] :as opts}]
  (api-request (merge (select-keys opts [:base-url :auth-token :exception-handler :logger])
                      {:method :post
                       :path   "/v2/consents"
                       :body   {:subject    {:verifiedId verified-id}
                                :actions    consents
                                :tags       []
                                :attributes {}}})))

(defprotocol ConsentPlatform
  (set-consents [_ _ _] "Set consent information for an individual")
  (fetch-consents [_ _] "Fetch an individual's consents"))

(defrecord WirewheelContext [logger exception-handler auth-token-timeout client-id client-secret issuer-base-url issuer-path api-base-url scheduler]
  component/Lifecycle
  (start [c]
    (logger :event {:event {:name :cms.lifecycle/started}})
    (let [auth-token (atom nil)]
      (scheduler/every scheduler
                       auth-token-timeout
                       "poller for Wire Wheel API auth token"
                       #(reset! auth-token
                                (some-> {:base-url          issuer-base-url
                                         :path              issuer-path
                                         :client-id         client-id
                                         :client-secret     client-secret
                                         :exception-handler exception-handler
                                         :logger            logger}
                                        auth-token-request
                                        :body
                                        (json/parse-string true)
                                        :access_token)))
      (assoc c :auth-token auth-token)))
  (stop [c]
    (logger :event {:event {:name :component.wirewheel.lifecycle/stopped}})
    (reset! (:auth-token c) nil)
    (dissoc c :auth-token))
  ConsentPlatform
  (set-consents [c verified-user-id consents]
    (consents-set-request
     {:base-url          api-base-url
      :auth-token        (deref (:auth-token c))
      :verified-id       verified-user-id
      :exception-handler exception-handler
      :logger            logger
      :consents          consents}))
  (fetch-consents [c verified-user-id]
    (some-> {:base-url          api-base-url
             :auth-token        (deref (:auth-token c))
             :verified-id       verified-user-id
             :exception-handler exception-handler
             :logger            logger}
            consents-fetch-request
            :body
            :unifiedConsent)))

(comment
  (let [wirewheel   (-> {:logger             (:logger dev-system/the-system)
                         :auth-token-timeout 20000 ;; 12000000 ; 20 minutes
                         :client-id          "0oa3o6qykrLjo3st4697"
                         :client-secret      "zdlfVJo7V2FN4pSDzwmXX_A09PWCT7S9elndwyHP"
                         :issuer-base-url    "https://wirewheelio.okta.com"
                         :issuer-path        "/oauth2/aus4je1g5wPYSB7dz697/v1/token"
                         :api-base-url       "https://api.upcp.wirewheel.io/"
                         :scheduler          (:scheduler dev-system/the-system)
                         :exception-handler  (:exception-handler dev-system/the-system)}
                      map->WirewheelContext
                      .start)
        verified-id (str (java.util.UUID/randomUUID))]
    (Thread/sleep 1000)
    (.set-consents wirewheel verified-id [{:target "foo-target"
                                           :vendor "bar-vendor"
                                           :action "REJECT"}])
    (.fetch-consents wirewheel verified-id)
    (.stop wirewheel))

  )
