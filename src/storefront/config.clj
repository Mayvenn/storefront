(ns storefront.config
  (:require [environ.core :refer [env]]
            [taoensso.timbre :as timbre]))

(defn development? [env]
  (= env "development"))

(def default-config {:server-opts {:port 3006}
                     :logging
                     (merge (timbre/get-default-config)
                            {:appenders
                             {:standard-out
                              (get-in timbre/example-config [:appenders :standard-out])}})})

(def env-config {:environment (env :environment)
                 :bugsnag-token (env :bugsnag-token)
                 :leads {:endpoint (env :leads-endpoint)}
                 :storeback {:endpoint (env :storeback-endpoint)
                             :internal-endpoint (env :storeback-internal-endpoint)}})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))
