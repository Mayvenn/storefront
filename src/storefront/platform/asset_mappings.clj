(ns storefront.platform.asset-mappings
  (:require [clojure.java.io :as io]
            [cheshire.core :refer [parse-string]]
            [environ.core :refer [env]]))

(def cdn-host (env :cdn-host))

(def manifest
  (when-let [mapping (io/resource "rev-manifest.json")]
    (parse-string (slurp mapping))))

(def image-manifest
  (->> manifest
       (filter (fn [[filename filerev]]
                 (re-find #"^images/" filename)))
       (into {})))

(def template-manifest
  (reduce (fn [acc [key value]]
            (assoc-in acc
                      (map keyword (clojure.string/split key #"[.]"))
                      {:url value}))
          {}
          manifest))

