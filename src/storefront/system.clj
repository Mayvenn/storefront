(ns storefront.system
  (:require [taoensso.timbre :as timbre]
            [com.stuartsierra.component :as component]
            [clj-honeybadger.core :as honeybadger]
            [storefront.config :as config]
            [ring.component.jetty :refer [jetty-server]]
            [storefront.handler :refer [create-handler]]))

(defrecord AppHandler [logger exception-handler storeback]
  component/Lifecycle
  (start [c]
    (let [params (merge {:storeback-config storeback}
                        (select-keys c [:logger :exception-handler]))]
      (assoc c :handler (create-handler params))))
  (stop [c] c))

(defn logger [logger-config]
  (fn [level str]
    (timbre/log logger-config level str)))

(defn exception-handler [honeybadger-token environment]
  (fn [e]
    (honeybadger/send-exception! e {:api-key honeybadger-token
                                    :env environment})))

(defn system-map [config]
  (component/system-map
   :logger (logger (config :logging))
   :app-handler (map->AppHandler (select-keys config [:storeback]))
   :embedded-server (jetty-server (config :server-opts))
   :exception-handler (exception-handler (config :honeybadger-token) (config :environment))))

(defn dependency-map []
  {:app-handler [:logger :exception-handler]
   :embedded-server {:app :app-handler}})

(defn create-system
  ([] (create-system {}))
  ([config-overrides]
   (let [config (config/system-config config-overrides)]
     (component/system-using
      (system-map config)
      (dependency-map)))))
