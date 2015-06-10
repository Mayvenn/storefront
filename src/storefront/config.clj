(ns storefront.config
  (:require [environ.core :refer [env]]
            [taoensso.timbre :as timbre]))

(defn development? [env]
  (= env "development"))

(defn store-location [environment]
  
    "production" "https://store.mayvenn.com"
    "acceptance" "https://store.diva-acceptance.com"
    "http://store.mayvenn-dev.com:3000"))


  
    
    
    

(def default-config {:server-opts {:port 3006}
                     :logging
                     (merge (timbre/get-default-config)
                            {:appenders
                             {:standard-out
                              (get-in timbre/example-config [:appenders :standard-out])}})})

(def env-config {:environment (env :environment)
                 :honeybadger-token (env :honeybadger-token)
                 :spree {:token (env :spree-token)
                         :endpoint (str (store-location (env :environment)) "/api")}
                 :storeback {:endpoint (env :storeback-endpoint)}
                 :prerender-token (env :prerender-token)})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))
