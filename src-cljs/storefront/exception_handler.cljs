(ns storefront.exception-handler
  (:require [storefront.script-tags :refer [insert-tag-pair remove-tag-pair]]
            [storefront.config :as config]))

(def ^:private class-name ".honeybadger-script")
(def ^:private src "//js.honeybadger.io/v0.2/honeybadger.min.js")

(def ^:private configuration
  (str "Honeybadger.configure({api_key: '"
       config/honeybadger-api-key
       "', environment: '"
       config/environment
       "'});"))

(defn honeybadger-enabled? []
  (not config/development?))

(defn report [error & [custom-class]]
  (cond (and (honeybadger-enabled?) js/Honeybadger)
        (js/Honeybadger.notify error custom-class)

        (and js/console js/console.error)
        (js/console.error "[Honeybadger not loaded when exception occurred]: " error custom-class)

        :else ""))

(defn insert-handler []
  (when (honeybadger-enabled?)
    (insert-tag-pair src configuration class-name)))

(defn remove-handler [h]
  (when (honeybadger-enabled?)
    (remove-tag-pair class-name)))
