(ns storefront.assets
  (:require [storefront.platform.asset-mappings :as asset-mappings]))

(defn path [resource-path]
  (let [mapped-asset (when asset-mappings/manifest
                       (some-> resource-path (subs 1) asset-mappings/manifest))]
    (if (and asset-mappings/cdn-host mapped-asset)
      (str "https://" asset-mappings/cdn-host "/cdn/" mapped-asset)
      resource-path)))

;; We need http, because facebook doesn't recognize https for og:image tags
;; https://stackoverflow.com/questions/8855361/fb-opengraph-ogimage-not-pulling-images-possibly-https
(def canonical-image
  "http://ucarecdn.com/ba713bd3-8fab-4a5a-b289-84ef4e58ecde/-/format/auto/canonical_image")

(defn css-url [url] (str "url(" url ")"))