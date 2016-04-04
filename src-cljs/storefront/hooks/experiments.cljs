(ns storefront.hooks.experiments
  (:require [storefront.browser.tags :refer [insert-tag-with-src
                                             insert-body-bottom
                                             text-tag
                                             remove-tags-by-class]]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]))

(def experiment->buckets
  (if config/production?
    {5490150509 [[(comp odd? :stylist_id) 5467272676]
                 [(comp even? :stylist_id) 5469762231]
                 [(comp #{"shop" "store"} :store_slug) 5484490640]]}
    {5486980194 [[(comp odd? :stylist_id) 5483790215]
                 [(comp even? :stylist_id) 5485630510]
                 [(comp #{"shop" "store"} :store_slug) 5486970170]]}))

(defn bucketeer [experiment-id store]
  (when-let [buckets (experiment->buckets experiment-id)]
    (when-let [variation-id (->> buckets
                                 (filter (fn [[pred bucket-variation-id]]
                                           (when (pred store) bucket-variation-id)))
                                 vals
                                 first)]
      variation-id)))

(defn calls [experiment-id store]
  (when-let [variation-id (bucketeer experiment-id store)]
    (clj->js [["bucketVisitor" experiment-id variation-id]])))

(defn insert-optimizely [store]
  (set! (.-optimizely js/window) (calls (first (keys experiment->buckets)) store))
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

(defn frontals? [data]
  (display-variation data "frontals"))

(defn three-steps? [data]
  (display-variation data "three-steps"))

(defn activate-universal-analytics []
  (when (and (.hasOwnProperty js/window "optimizely") js/optimizely.activateUniversalAnalytics)
    (.activateUniversalAnalytics js/optimizely)))
