(ns catalog.images
  (:require [storefront.components.ui :as ui]
            [storefront.accessors.images :as images]))

(defn ^:private ucare-id [images-catalog use-case skuer]
  (or
   ;; TODO: GROT after we migrate to normalized cellar endpoint
   (some->> skuer
            :selector/images ;; this key is deprecated, use images-cases instead
            (filter #(= (:use-case %) use-case))
            first
            :url
            ui/ucare-img-id)
   (some->> skuer
            :selector/image-cases
            (filter (fn [[img-use-case _ _]] (= img-use-case use-case)))
            first
            ((fn [[_ _ catalog-image-id]] catalog-image-id))
            images-catalog
            :url
            ui/ucare-img-id)))

;; TODO(jeff): Is this function needed? Why not just pass around the ucare id directly?
(defn image [images-catalog use-case skuer]
  {:ucare/id (ucare-id images-catalog use-case skuer)})
