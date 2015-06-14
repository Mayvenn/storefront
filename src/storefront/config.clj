(ns storefront.config
  (:require [environ.core :refer [env]]
            [storefront.server :as server]
            [taoensso.timbre :as timbre]))

(defn development? [env]
  (= env "development"))


  
    
    
    

(def default-config {:server-opts {:port 3006
                                   :configurator server/configurator}
                     :logging
                     (merge (timbre/get-default-config)
                            {:appenders
                             {:standard-out
                              (get-in timbre/example-config [:appenders :standard-out])}})})

(def env-config {:environment (env :environment)
                 :honeybadger-token (env :honeybadger-token)
                 :storeback {:endpoint (env :storeback-endpoint)}
                 :prerender-token (env :prerender-token)})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))
