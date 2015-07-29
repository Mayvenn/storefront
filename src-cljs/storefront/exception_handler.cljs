(ns storefront.exception-handler
  (:require [storefront.browser.tags :refer [insert-tag-pair remove-tag-pair]]
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

(defn- log [msg error error-class]
  (when (and js/console js/console.error)
    (js/console.error (str msg error error-class))))

(defn report [error & [custom-class]]
  (if (and (honeybadger-enabled?) (.hasOwnProperty js/window "Honeybadger"))
    (do (js/Honeybadger.notify error custom-class)
        (log "[Exception occurred, logged to honeybadger]: " error custom-class))
    (log "[Honeybadger not loaded when exception occurred]: " error custom-class))
  (throw error))

(defn insert-handler []
  (when (honeybadger-enabled?)
    (insert-tag-pair src configuration class-name)))

(defn remove-handler []
  (when (honeybadger-enabled?)
    (remove-tag-pair class-name)))
