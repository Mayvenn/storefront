(ns storefront.riskified
  (:require [storefront.script-tags :refer [insert-tag-with-src remove-tag]]))

(def store-domain "mayvenn.com")

(defn insert-beacon [session-id]
  (insert-tag-with-src
   (str "//beacon.riskified.com?shop=" store-domain "&sid=" session-id)
   "riskified-beacon"))

(defn remove-beacon []
  (remove-tag "riskified-beacon"))

(defn track-page [path]
    (when (.hasOwnProperty js/window "RISKX")
    (.go js/RISKX (clj->js path))))
