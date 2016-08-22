(ns storefront.hooks.optimizely
  (:require [storefront.browser.tags :as tags]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(defn insert-tracking []
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely")
                                 #(m/handle-message events/inserted-optimizely))
  (js/setTimeout #(m/handle-message events/inserted-optimizely) 15000))

(defn remove-tracking []
  (tags/remove-tags-by-class "optimizely"))

(defn set-dimension [dimension-name value]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["setDimensionValue" dimension-name value]))))

(defn track-event [event-name & [opts]]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["trackEvent" event-name opts]))))

;; TODO are we using ga ecommerce and does this work without that?
(defn activate-universal-analytics []
  (when (and (.hasOwnProperty js/window "optimizely") js/optimizely.activateUniversalAnalytics)
    (.activateUniversalAnalytics js/optimizely)))
