(ns storefront.system
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [spice.date :as date]
            [storefront.config :as config]
            [storefront.handler :refer [create-handler]]
            [storefront.jetty :as jetty]
            [storefront.system.contentful :as contentful]
            [spice.logger.core :as logger]
            [tocsin.core :as tocsin]))

(defrecord AppHandler [logger exception-handler storeback-config welcome-config environment client-version]
  component/Lifecycle
  (start [c]
    (assoc c :handler (create-handler (dissoc c :handler))))
  (stop [c] c))

(defn exception-handler [bugsnag-token environment]
  (fn [e]
    (tocsin/notify e {:api-key bugsnag-token
                      :environment environment
                      :project-ns "storefront"})))

(defrecord AtomCache [atom]
  component/Lifecycle
  (start [c] (assoc c :atom (clojure.core/atom nil)))
  (stop [c] (assoc c :atom (clojure.core/atom nil))))

(defn system-map [config]
  (component/system-map
   :logger (logger/create-logger (config :logging))
   :contentful  (contentful/map->ContentfulContext (merge (:contentful-config config)
                                                          (select-keys config [:environment])))
   :app-handler (map->AppHandler (select-keys config [:storeback-config
                                                      :welcome-config
                                                      :environment
                                                      :client-version]))
   :sitemap-cache (->AtomCache nil)
   :embedded-server (jetty-server (merge (:server-opts config)
                                         {:configurator jetty/configurator}))
   :exception-handler (exception-handler (config :bugsnag-token) (config :environment))))

(def dependency-map
  {:app-handler     [:logger :exception-handler :contentful :sitemap-cache]
   :contentful      [:logger :exception-handler]
   :embedded-server {:app :app-handler}})

(defn create-system
  ([] (create-system {}))
  ([config-overrides]
   (let [config (config/system-config config-overrides)]
     (component/system-using
      (system-map config)
      dependency-map))))
