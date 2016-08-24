(ns storefront.assets
  (:require [storefront.platform.asset-mappings :as asset-mappings]))

(defn path [resource-path]
  (let [mapped-asset (when asset-mappings/manifest
                       (-> resource-path (subs 1) asset-mappings/manifest))]
    (if (and asset-mappings/cdn-host mapped-asset)
      (str "//" asset-mappings/cdn-host "/cdn/" mapped-asset)
      resource-path)))

(def canonical-image (path "/images/canonical_image.jpg"))

(defn css-url [url] (str "url(" url ")"))
