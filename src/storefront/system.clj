(ns storefront.system
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [spice.date :as date]
            [storefront.config :as config]
            [storefront.handler :refer [create-handler]]
            [storefront.jetty :as jetty]
            [taoensso.timbre :as timbre]
            [tocsin.core :as tocsin]))

(defrecord ContentfulContext
    [logger exception-handler cache-timeout api-key space-id endpoint]
  component/Lifecycle
  (start [c]
    (assoc c :cache (atom {:timestamp (date/add-delta
                                       (date/now)
                                       {:minutes -6})
                           :transformed-response nil})))
  (stop [c]
    (dissoc c :cache)))

(defrecord AppHandler [logger exception-handler contentful-config storeback-config leads-config environment client-version]
  component/Lifecycle
  (start [c]
    (assoc c :handler (create-handler (dissoc c :handler))))
  (stop [c] c))

(defn logger [logger-config]
  (fn [level str]
    (timbre/log logger-config level str)))

(defn exception-handler [bugsnag-token environment]
  (fn [e]
    (tocsin/notify e {:api-key bugsnag-token
                      :environment environment
                      :project-ns "storefront"})))


(defn system-map [config]
  (component/system-map
   :logger (logger (config :logging))
   :contentful  (map->ContentfulContext (:contentful-config config))
   :app-handler (map->AppHandler (select-keys config [:storeback-config
                                                      :leads-config
                                                      :environment
                                                      :client-version]))
   :embedded-server (jetty-server (merge (:server-opts config)
                                         {:configurator jetty/configurator}))
   :exception-handler (exception-handler (config :bugsnag-token) (config :environment))))

(def dependency-map
  {:app-handler [:logger :exception-handler :contentful]
   :contentful  [:logger :exception-handler]
   :embedded-server {:app :app-handler}})

(defn create-system
  ([] (create-system {}))
  ([config-overrides]
   (let [config (config/system-config config-overrides)]
     (component/system-using
      (system-map config)
      dependency-map))))
