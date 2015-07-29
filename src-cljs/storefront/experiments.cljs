(ns storefront.experiments
  (:require [storefront.script-tags :refer [insert-tag-with-src
                                            remove-tags]]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]))

(defn insert-optimizely []
  (insert-tag-with-src (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely"))

(defn remove-optimizely []
  (remove-tags "optimizely"))

(defn track-event [event-name]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["trackEvent" event-name]))))

(defn display-variation [data variation]
  (contains? (get-in data keypaths/optimizely-variations)
             variation))
