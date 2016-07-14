(ns storefront.assets
  (:require [clojure.java.io :as io]
            [cheshire.core :refer [parse-string]]
            [environ.core :refer [env]]))

(def cdn-host (env :cdn-host))

(def asset-map
  (when-let [mapping (io/resource "rev-manifest.json")]
    (parse-string (slurp mapping))))

(defn asset-path [resource-path]
  (let [mapped-asset (when asset-map (-> resource-path (subs 1) asset-map))]
    (if (and cdn-host mapped-asset)
      (str "//" cdn-host "/cdn/" mapped-asset)
      resource-path)))
