(ns storefront.config
  (:require [environ.core :refer [env]]
            [clojure.java.io :as io]))

(def welcome-subdomain "welcome")

(def freeinstall-subdomain "freeinstall")

(defn development? [environment]
  {:pre [(#{"development" "test" "acceptance" "production"} environment)]}
  (= environment "development"))

(defn dev-print! [environment & msg]
  (when (development? environment)
    (newline)
    (println "********************************")
    (apply println msg)
    (println "********************************")
    (newline)
    (newline)))

(def feature-block-look-ids
  ;;NOTE edit the cljs config too!
  ;;NOTE @Ryan, please only change the top map
  (if (= (env :environment) "production")
    {:left  191567494
     :right 191567299}
    {:left  144863121
     :right 144863121}))

(def client-version
  (try
    (slurp (io/resource "client_version.txt"))
    (catch java.lang.IllegalArgumentException e
      "unknown")))

(def default-config {:server-opts       {:port 3006}
                     :client-version    client-version
                     :contentful-config {:cache-timeout 120000
                                         :endpoint      "https://cdn.contentful.com"}
                     :logging           {:system-name "storefront.system"}})

(def env-config {:environment       (env :environment)
                 :bugsnag-token     (env :bugsnag-token)
                 ;;TODO Update env-vars
                 :welcome-config    {:url (env :welcome-url)}
                 :contentful-config {:api-key  (env :contentful-content-delivery-api-key)
                                     :space-id (env :contentful-space-id)}
                 :storeback-config  {:endpoint          (env :storeback-endpoint)
                                     :internal-endpoint (or
                                                         (env :storeback-v2-internal-endpoint)
                                                         (env :storeback-internal-endpoint))}})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))

(def pixlee {})
(def voucherify {})
