(ns storefront.config
  (:require [environ.core :refer [env]]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]))

(defn development? [env]
  {:pre [(#{"development" "test" "acceptance" "production"} env)]}
  (= env "development"))

(def client-version
  (try
    (slurp (io/file (io/resource "client_version.txt")))
    (catch java.lang.IllegalArgumentException e
      "unknown")))

(def default-config {:server-opts    {:port 3006}
                     :client-version client-version
                     :logging
                     (merge (timbre/get-default-config)
                            {:appenders
                             {:standard-out
                              (get-in timbre/example-config [:appenders :standard-out])}})})

(def env-config {:environment      (env :environment)
                 :bugsnag-token    (env :bugsnag-token)
                 :leads-config     {:endpoint (env :leads-endpoint)}
                 :storeback-config {:endpoint          (env :storeback-endpoint)
                                    :internal-endpoint (env :storeback-internal-endpoint)}})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))
