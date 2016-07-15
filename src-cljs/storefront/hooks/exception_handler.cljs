(ns storefront.hooks.exception-handler
  (:require [storefront.browser.tags :refer [insert-tag-pair remove-tag-pair]]
            [storefront.config :as config]
            [bugsnag]))

(defn- log [msg error custom-context]
  (when (and js/console js/console.error)
    (js/console.error msg error (clj->js custom-context))))

(defn report [error & [custom-context]]
  (if (and config/report-errors? (.hasOwnProperty js/window "Bugsnag"))
    (do (js/Bugsnag.notifyException error nil (clj->js custom-context) "error")
        (log "[Exception occurred, logged to bugsnag]: " error custom-context))
    (log "[Bugsnag not loaded when exception occurred]: " error custom-context))
  (throw error))

(defn refresh []
  (when (.hasOwnProperty js/window "Bugsnag")
    (.refresh js/Bugsnag)))
