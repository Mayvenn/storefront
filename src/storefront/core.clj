(ns storefront.core
  (:gen-class :main true)
  (:require [com.stuartsierra.component :as component]
            [tocsin.core :as tocsin]
            [environ.core :refer [env]]
            [storefront.jetty :as jetty]
            [storefront.system :as system]
            [mayvenn.tracer :as tracer]))

(def the-system nil)

(.addShutdownHook (Runtime/getRuntime)
                  (Thread. (fn []
                             (when the-system
                               ;; Ensure de-registered from service registry before stopping
                               ;; the system (which causes Jetty stopping to accept new requests)
                               (Thread/sleep jetty/stale-reads-timeout)
                               (component/stop the-system)))))

(defn -main [& args]
  (try
    (when-not (= (env :environment) "development")
      (tracer/configure-xray!))
    (alter-var-root #'the-system (constantly (system/create-system)))
    (alter-var-root #'the-system component/start)
    (when (= (env :environment) "development")
      (println "Ready to serve.")) ; So devs can watch the logs instead of constantly hitting F5
    (catch Exception e
      (do (tocsin/notify e {:api-key (env :bugsnag-token)
                            :environment (env :environment)
                            :project-ns "storefront"})
          (throw e)))))
