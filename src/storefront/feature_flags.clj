(ns storefront.feature-flags
  (:require [com.stuartsierra.component :as component])
  (:import [com.launchdarkly.client LDClient LDUser$Builder]))

(defprotocol IFeatureFlags
  (retrieve-flag [_ flag-key default]
    "Returns the boolean value of the feature flag or default if unavailable"))

(defrecord LaunchDarkly [launchdarkly-config]
  component/Lifecycle
  (start [c]
    (assoc c
           :client (LDClient. (:sdk-key launchdarkly-config))
           :user (.build (doto (LDUser$Builder. "mayvenn")
                           (.anonymous true)))))
  (stop [c]
    (when-let [client (:client c)]
      (.close client))
    (dissoc c :client :user))

  IFeatureFlags
  (retrieve-flag [{:keys [client user]} flag-key default]
    (.boolVariation client flag-key user (boolean default))))

(defrecord TestFeatureFlags [flags]
  component/Lifecycle
  (start [c]
    (assoc c :flags flags))
  (stop [c]
    (dissoc c :flags))

  IFeatureFlags
  (retrieve-flag [{:keys [flags]} flag-key default]
    (get flags flag-key default)))
