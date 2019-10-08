(ns storefront.platform.asset-mappings
  (:require [cljs-bean.core :refer [->clj]]))

(def cdn-host js/cdnHost)

(def manifest (->clj js/assetManifest))
