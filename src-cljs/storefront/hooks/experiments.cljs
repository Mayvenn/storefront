(ns storefront.hooks.experiments
  (:require [storefront.browser.tags :as tags]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(def stylist-odd?  (comp odd? :stylist_id))
(def stylist-even? (comp even? :stylist_id))
(def no-stylist?   (comp #{"shop" "store"} :store_slug))

(def experiment->buckets
  {:experiment-id [[no-stylist? 2]
                   [stylist-odd? 0]
                   [stylist-even? 1]]})

(defn- bucket [store experiment-id]
  (some->> (get experiment->buckets experiment-id)
           (some (fn [[pred bucket-variation-idx]]
                   (when (pred store) bucket-variation-idx)))
           (conj ["bucketVisitor" experiment-id])))

(defn insert-optimizely [store]
  (set! (.-optimizely js/window) (clj->js (keep (partial bucket store) (keys experiment->buckets))))
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely")
                                 #(m/handle-message events/inserted-optimizely))
  (js/setTimeout #(m/handle-message events/inserted-optimizely) 15000))

(defn remove-optimizely []
  (tags/remove-tags-by-class "optimizely"))

(defn set-dimension [dimension-name value]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["setDimensionValue" dimension-name value]))))

(defn track-event [event-name & [opts]]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["trackEvent" event-name opts]))))

(defn display-variation [data variation]
  (contains? (get-in data keypaths/optimizely-variations)
             variation))

(defn color-option? [data]
  (display-variation data "color-option"))

(defn activate-universal-analytics []
  (when (and (.hasOwnProperty js/window "optimizely") js/optimizely.activateUniversalAnalytics)
    (.activateUniversalAnalytics js/optimizely)))
