(ns storefront.config
  (:require [environ.core :refer [env]]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]))

;; TODO: These numbers also exist in config.cljs - consider creating cljc
(def mayvenn-leads-call-number "1-866-424-7201")
(def mayvenn-leads-sms-number  "34649")

(defn development? [environment]
  {:pre [(#{"development" "test" "acceptance" "production"} environment)]}
  (= environment "development"))

(def feature-block-look-ids
  ;;NOTE edit the cljs config too!
  ;;NOTE @Ryan, please only change the top map
  (if (= (env :environment) "production")
    {:left  186605502
     :right 191946859}
    {:left  144863121
     :right 144863121}))

(def client-version
  (try
    (slurp (io/resource "client_version.txt"))
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
                 :dc-logo-config   {:endpoint (env :dc-logo-endpoint)}
                 :storeback-config {:endpoint          (env :storeback-endpoint)
                                    :internal-endpoint (env :storeback-internal-endpoint)}})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))

(def pixlee {})
