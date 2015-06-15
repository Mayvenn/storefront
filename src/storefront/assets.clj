(ns storefront.assets
  (:require [clojure.java.io :as io]
            [cheshire.core :refer [parse-string]]
            [environ.core :refer [env]]))

(def asset-map
  (when-let [mapping (io/resource "rev-manifest.json")]
    (parse-string (slurp mapping))))

(defn asset-path [resource-path]
  (let [cdn-host (env :cdn-host)
        mapped-asset (-> resource-path (subs 1) asset-map)]
    (if (and cdn-host mapped-asset)
      (str "//" cdn-host "/cdn/" mapped-asset)
      resource-path)))
