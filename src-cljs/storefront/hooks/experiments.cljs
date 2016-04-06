(ns storefront.hooks.experiments
  (:require [storefront.browser.tags :as tags]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.messages :as m]
            [storefront.config :as config]))


(def stylist-odd?  (comp odd? :stylist_id))
(def stylist-even? (comp even? :stylist_id))
(def no-stylist?   (comp #{"shop" "store"} :store_slug))

(def frontals-experiment (if config/production? 5490150509 5486980194))
(def frontals-original   (if config/production? 5467272676 5483790215))
(def frontals-variation  (if config/production? 5469762231 5485630510))
(def frontals-exclude    (if config/production? 5484490640 5486970170))

(def experiment->buckets
  {frontals-experiment [[no-stylist? frontals-exclude]
                        [stylist-odd? frontals-original]
                        [stylist-even? frontals-variation]]})

;; optimizely.data.experiments = {experiment-id -> {name -> String, variation_ids -> [Int], ...}}
(defn- variation-id->name [variation-id]
  (let [experiment (->> js/optimizely.data.experiments
                        js->clj
                        vals
                        (filter (fn [experiment] (-> experiment
                                                    (get "variation_ids")
                                                    set
                                                    (get (str variation-id)))))
                        first)]
    (str (experiment "name") "/" (get-in (js->clj js/optimizely.data.variations) [(str variation-id) "name"]))))

(defn- bucketeer [experiment-id store]
  (when-let [buckets (experiment->buckets experiment-id)]
    (->> buckets
         (filter (fn [[pred bucket-variation-id]]
                   (when (pred store) bucket-variation-id)))
         first)))

(defn- calls [store experiment-id]
  (if-let [[_ variation-id] (bucketeer experiment-id store)]
    [["bucketVisitor" experiment-id variation-id]]
    []))

(defn- active-variation-ids []
  (let [active-experiments            (set (js->clj js/optimizely.data.state.activeExperiments))
        experiment->active-variations (js->clj js/optimizely.data.state.variationIdsMap)]
    (mapcat #(get experiment->active-variations %) active-experiments)))

(defn insert-optimizely [store]
  (set! (.-optimizely js/window)
        (clj->js (mapcat (partial calls store) (keys experiment->buckets))))
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely")
                                 (fn []
                                   (doseq [variation-id (active-variation-ids)]
                                     (m/handle-message events/optimizely {:variation (variation-id->name variation-id)}))
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
  (display-variation data "Frontal Closure/Variation #1 - Even Numbers"))

(defn predict-frontals? [data]
  (if (get-in data keypaths/loaded-optimizely)
    (frontals? data)
    (let [store (get-in data keypaths/store)]
      (and (stylist-even? store)
           (not (no-stylist? store))))))

(defn three-steps? [data]
  (display-variation data "three-steps"))

(defn activate-universal-analytics []
  (when (and (.hasOwnProperty js/window "optimizely") js/optimizely.activateUniversalAnalytics)
    (.activateUniversalAnalytics js/optimizely)))
