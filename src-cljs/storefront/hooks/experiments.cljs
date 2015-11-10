(ns storefront.hooks.experiments
  (:require [storefront.browser.tags :refer [insert-tag-with-src
                                             remove-tags]]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]))

(defn insert-optimizely []
  (insert-tag-with-src (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely"))

(defn remove-optimizely []
  (remove-tags "optimizely"))

(defn set-dimension [dimension-name value]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["setDimensionValue" dimension-name value]))))

(defn track-event [event-name & [opts]]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["trackEvent" event-name opts]))))

(defn display-variation [data variation]
  (contains? (get-in data keypaths/optimizely-variations)
             variation))

(defn bundle-builder? [data]
  (display-variation data "bundle-builder"))

(defn- bundle-builder-included-stylist-only [data stylist-only-map]
  (and (bundle-builder? data)
       (not (:stylist_only? stylist-only-map))))

(def bundle-builder-included-product? bundle-builder-included-stylist-only)

(def bundle-builder-included-taxon? bundle-builder-included-stylist-only)

(defn paypal? [data]
  (display-variation data "paypal"))

(defn activate-universal-analytics []
  (when (.hasOwnProperty js/window "optimizely")
    (.activateUniversalAnalytics js/optimizely)))
