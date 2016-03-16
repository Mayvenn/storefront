(ns storefront.core
  (:gen-class :main true)
  (:require [com.stuartsierra.component :as component]
            [tocsin.core :as tocsin]
            [environ.core :refer [env]]
            [storefront.system :as system]))

(def the-system nil)

(.addShutdownHook (Runtime/getRuntime)
                  (Thread. (fn [] (when the-system (component/stop the-system)))))

(defn -main [& args]
  (try
    (alter-var-root #'the-system (constantly (system/create-system)))
    (component/start the-system)
    (catch Exception e
      (do (tocsin/notify e {:api-key (env :bugsnag-token)
                            :environment (env :environment)})
          (throw e)))))
