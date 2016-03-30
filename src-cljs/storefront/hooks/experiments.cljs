(ns storefront.hooks.experiments
  (:require [storefront.browser.tags :refer [insert-tag-with-src
                                             insert-body-bottom
                                             text-tag
                                             remove-tags-by-class]]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]))

(defn insert-optimizely []
  (insert-body-bottom (text-tag "window['optimizely'] = window['optimizely'] || []" "optimizely"))
  (insert-tag-with-src (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely"))

(defn remove-optimizely []
  (remove-tags-by-class "optimizely"))

(defn set-dimension [dimension-name value]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["setDimensionValue" dimension-name value]))))

(defn track-event [event-name & [opts]]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["trackEvent" event-name opts]))))

(defn display-variation [data variation]
  (contains? (get-in data keypaths/optimizely-variations)
             variation))

(defn guest-checkout? [data]
  (display-variation data "guest-checkout"))

(defn frontals? [data]
  (display-variation data "frontals"))

(defn activate-universal-analytics []
  (when (and (.hasOwnProperty js/window "optimizely") js/optimizely.activateUniversalAnalytics)
    (.activateUniversalAnalytics js/optimizely)))
