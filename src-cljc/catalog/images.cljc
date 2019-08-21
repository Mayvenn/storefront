(ns catalog.images
  (:require [storefront.components.ui :as ui]))

(defn ucare-id [use-case skuer]
  (some->> skuer
           :selector/images
           (filter #(= (:use-case %) use-case))
           first
           :url
           ui/ucare-img-id))

(defn image [use-case skuer]
  {:ucare/id (ucare-id use-case skuer)})
