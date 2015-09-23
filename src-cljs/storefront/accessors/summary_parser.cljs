(ns storefront.accessors.summary-parser
  (:require [clojure.string :as string]))



(def ^:private summary-grade {"premier" "6A premier"
                    "deluxe" "7A deluxe"
                    "ultra" "8A ultra"})

(def ^:private closure-summary [:style :material :origin :length (constantly "closure")])
(def ^:private bundle-summary [:color :grade :origin :length :style])
(def ^:private fallback-summary [:default-name])

(defn- join-spaces [& strs]
  (string/join " " (filter seq strs)))

(defn- default-origin [origin]
  (if (#{"ultra" "deluxe"} origin)
    "brazilian"
    origin))

(defn- summary-material [variant-slug]
  (let [[maybe-closure maybe-material] (take 2 (reverse (string/split variant-slug "-")))]
    (prn maybe-material maybe-closure)
    (when (= maybe-closure "closure") maybe-material)))

(defn- summary-origin [variant-slug]
  (default-origin (apply str (take-while (partial not= \-) variant-slug))))

(defn- summary-color [variant-slug]
  (when (= (summary-origin variant-slug) "indian")
    (apply join-spaces (drop 2 (string/split variant-slug "-")))))

(defn- summary-stripped-category [variant-category]
  (string/replace (apply str (rest (drop-while (partial not= \/) variant-category))) "-" " "))

(defn- variant->summary [variant]
  {:color (summary-color (:slug variant))
   :material (summary-material (:slug variant))
   :grade (summary-grade (:collection_name variant))
   :origin (summary-origin (:slug variant))
   :length (-> variant :option_values first :name (str \"))
   :style (summary-stripped-category (:category variant))
   :default-name (:name variant)})

(defn- show-summary [summary]
  (prn summary)
  (let [format-vector (cond (:material summary)    closure-summary
                            (seq (:style summary)) bundle-summary
                            :else                  fallback-summary)]
    (apply join-spaces ((apply juxt format-vector) summary))))

(def summary (comp show-summary variant->summary))
