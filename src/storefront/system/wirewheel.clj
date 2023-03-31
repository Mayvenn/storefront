(ns storefront.system.wirewheel
  (:require [tugboat.core :as tugboat]
            [storefront.system.scheduler :as scheduler]
            [com.stuartsierra.component :as component]
            [cheshire.core :as json])
  (:import java.util.Base64))

(defn auth-token-request
  [{:keys [issuer-url client-id client-secret exception-handler logger]}]
  (try
    (tugboat/request {:logger       logger
                      :method       :post
                      :url          issuer-url
                      :headers      {"Content-Type"  "application/x-www-form-urlencoded"
                                     "Authorization" (->> (str client-id ":" client-secret)
                                                          .getBytes
                                                          (.encodeToString (Base64/getEncoder))
                                                          (str "Basic "))}
                      :query-params {"grant_type" "client_credentials"
                                     "scope"      "consent-insert-api consent-get-api"}
                      :body         (json/generate-string {"access_token" nil})})
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

(defrecord WirewheelContext [logger exception-handler scheduler auth-token-timeout client-id client-secret issuer-url api-base-url]
  component/Lifecycle
  (start [c]
    (logger :event {:event {:name :cms.lifecycle/started}})
    (let [auth-token (atom nil)]
      (scheduler/every scheduler
                       auth-token-timeout
                       "poller for Wire Wheel API auth token"
                       #(reset! auth-token
                                (some-> {:issuer-url        issuer-url
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
    (when (:auth-token c)
      (reset! (:auth-token c) nil))
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
