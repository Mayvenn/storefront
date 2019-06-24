(ns storefront.assets
  (:require [storefront.platform.asset-mappings :as asset-mappings]))

(defn path [resource-path]
  (let [mapped-asset (when asset-mappings/manifest
                       (-> resource-path (subs 1) asset-mappings/manifest))]
    (if (and asset-mappings/cdn-host mapped-asset)
      (str "https://" asset-mappings/cdn-host "/cdn/" mapped-asset)
      resource-path)))

(def canonical-image "//ucarecdn.com/efeb9b11-04a1-4f50-bec3-9cda594407ae/canonical_image.png")

(defn css-url [url] (str "url(" url ")"))
