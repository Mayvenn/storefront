(ns storefront.hooks.experiments
  (:require [storefront.browser.tags :as tags]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :as m]
            [storefront.config :as config]))

(def experiment->buckets
  (if config/production?
    {5490150509 [[(comp #{"shop" "store"} :store_slug) 5484490640]
                 [(comp odd? :stylist_id) 5467272676]
                 [(comp even? :stylist_id) 5469762231]]}
    {5486980194 [[(comp #{"shop" "store"} :store_slug) 5486970170]
                 [(comp odd? :stylist_id) 5483790215]
                 [(comp even? :stylist_id) 5485630510]]}))

(def variation-id->name
  {"5469762231" "frontals"
   "5485630510" "frontals"})

(defn- bucketeer [experiment-id store]
  (when-let [buckets (experiment->buckets experiment-id)]
    (->> buckets
         (filter (fn [[pred bucket-variation-id]]
                   (when (pred store) bucket-variation-id)))
         first)))

(defn- calls [store experiment-id]
  (when-let [[_ variation-id] (bucketeer experiment-id store)]
    [["bucketVisitor" experiment-id variation-id]]))

(defn insert-optimizely [store]
  (set! (.-optimizely js/window) (clj->js (reduce concat [] (map (partial calls store) (keys experiment->buckets)))))
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely")
                                 (fn []
                                   (doseq [variation-id (flatten (vals (js->clj js/optimizely.data.state.variationIdsMap)))]
                                     (when-let [variation-name (variation-id->name variation-id)]
                                       (m/handle-message events/optimizely {:variation variation-name})))
                                   (m/handle-message events/inserted-optimizely)))
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

(defn frontals? [data]
  (display-variation data "frontals"))

(defn predict-frontals? [data]
  (if (get-in data keypaths/loaded-optimizely)
    (frontals? data)
    (and (-> data (get-in keypaths/store) :stylist_id odd?)
         (not (-> data (get-in keypaths/store) :store_slug #{"shop" "store"})))))

(defn three-steps? [data]
  (display-variation data "three-steps"))

(defn activate-universal-analytics []
  (when (and (.hasOwnProperty js/window "optimizely") js/optimizely.activateUniversalAnalytics)
    (.activateUniversalAnalytics js/optimizely)))
