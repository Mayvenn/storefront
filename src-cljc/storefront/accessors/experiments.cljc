(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]
            [clojure.set :as set]))

(def stylist-shop? (comp #{"shop"} :store_slug :store))
(def stylist-store? (comp #{"store"} :store_slug :store))
(defn stylist-mod? [m v]
  (fn [data]
    (some-> data :store :stylist_id (mod m) (= v))))

(def control-1 0)
(def control-2 1)
(def variation 2)
(def shop-control 3)
(def store-control 4)

(def color-option-experiment-prod 6407130806)
(def color-option-experiment-sandbox 6368621011)

(defn experiments [environment]
  [{:name    "color-option"
    :id      (if (#{"development" "acceptance"} environment)
               color-option-experiment-sandbox
               color-option-experiment-prod)
    :buckets [[stylist-shop?      shop-control]
              [stylist-store?     store-control]
              [(stylist-mod? 3 0) control-1]
              [(stylist-mod? 3 1) control-2]
              [(stylist-mod? 3 2) variation]]}])

(def experiment->features
  {[color-option-experiment-sandbox variation]  #{"color-option"}
   [color-option-experiment-prod variation] #{"color-option"}})

(defn bucket-applies [data [pred variation-index]]
  (when (pred data) variation-index))

(defn experiment-applies [data {:keys [buckets id]}]
  (when-let [variation-index (some (partial bucket-applies data) buckets)]
    [id variation-index]))

(defn applicable-experiments [experiment-config data]
  (doall (keep (partial experiment-applies data) experiment-config)))

(defn determine-experiments [data environment]
  (assoc-in data
            keypaths/optimizely-buckets
            (applicable-experiments (experiments environment) data)))

(defn determine-features [data]
  (let [features (->> (get-in data keypaths/optimizely-buckets)
                      (map experiment->features)
                      (apply set/union))]
    (assoc-in data keypaths/features features)))

(defn display-feature? [data feature]
  ((set (get-in data keypaths/features)) feature))

(defn color-option? [data]
  (display-feature? data "color-option"))

(defn option-memory? [data]
  (display-feature? data "option-memory"))

(defn accounts-redesign? [data]
  (display-feature? data "accounts-redesign"))

(defn stylist-banner? [data]
  (display-feature? data "stylist-banner"))
