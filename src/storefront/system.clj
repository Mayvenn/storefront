(ns storefront.system
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [spice.date :as date]
            [storefront.config :as config]
            [storefront.handler :refer [create-handler]]
            [storefront.jetty :as jetty]
            [storefront.system.contentful :as contentful]
            [taoensso.timbre :as timbre]
            [tocsin.core :as tocsin]))

(defrecord AppHandler [logger exception-handler storeback-config leads-config environment client-version]
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
   :contentful  (contentful/map->ContentfulContext (merge (:contentful-config config)
                                                          (select-keys config [:environment])))
   :app-handler (map->AppHandler (select-keys config [:storeback-config
                                                      :leads-config
                                                      :environment
                                                      :client-version]))
   :embedded-server (jetty-server (merge (:server-opts config)
                                         {:configurator jetty/configurator}))
   :exception-handler (exception-handler (config :bugsnag-token) (config :environment))))

(def dependency-map
  {:app-handler     [:logger :exception-handler :contentful]
   :contentful      [:logger :exception-handler]
   :embedded-server {:app :app-handler}})

(defn create-system
  ([] (create-system {}))
  ([config-overrides]
   (let [config (config/system-config config-overrides)]
     (component/system-using
      (system-map config)
      dependency-map))))
