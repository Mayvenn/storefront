(ns storefront.system
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [spice.date :as date]
            [storefront.config :as config]
            [storefront.feature-flags :as feature-flags]
            [storefront.handler :refer [create-handler]]
            [storefront.jetty :as jetty]
            [storefront.system.contentful :as contentful]
            [storefront.system.scheduler :as scheduler]
            [storefront.system.contentful.static-page :as static-page]
            [spice.logger4j :as logger4j]
            [tocsin.core :as tocsin]
            [storefront.system.wirewheel :as wirewheel]))

(defrecord AppHandler [logger exception-handler storeback-config welcome-config environment
                       client-version static-pages-repo wirewheel-config]
  component/Lifecycle
  (start [c]
    (assoc c :handler (create-handler (dissoc c :handler))))
  (stop [c] (dissoc c :handler)))

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
   :logger (logger4j/make-logger (config :logging))
   :scheduler (scheduler/->Scheduler nil nil nil)
   :wirewheel-context (wirewheel/map->WirewheelContext (:wirewheel-config config))
   :static-pages-repo (static-page/->Repository
                       (merge (:contentful-config config)
                              (select-keys config [:environment]))
                       nil nil)
   :contentful  (contentful/map->ContentfulContext (merge (:contentful-config config)
                                                          (select-keys config [:environment])))
   :launchdarkly (feature-flags/map->LaunchDarkly (select-keys config [:launchdarkly-config]))
   :app-handler (map->AppHandler (select-keys config [:storeback-config
                                                      :welcome-config
                                                      :wirewheel-config
                                                      :environment
                                                      :client-version]))
   :sitemap-cache (->AtomCache nil)
   :embedded-server (jetty-server (merge (:server-opts config)
                                         {:configurator jetty/configurator}))
   :exception-handler (exception-handler (config :bugsnag-token) (config :environment))))

(def dependency-map
  {:app-handler       [:logger :exception-handler :contentful :wirewheel-context :launchdarkly :sitemap-cache :static-pages-repo]
   :contentful        [:logger :exception-handler :scheduler]
   :wirewheel-context [:logger :exception-handler :scheduler]
   :static-pages-repo [:scheduler :exception-handler]
   :scheduler         [:logger :exception-handler]
   :embedded-server   {:app :app-handler}})

(defn create-system
  ([] (create-system {}))
  ([config-overrides] (create-system config-overrides identity))
  ([config-overrides component-overrides-fn]
   (let [config (config/system-config config-overrides)]
     (component/system-using
      (component-overrides-fn (system-map config))
      dependency-map))))
