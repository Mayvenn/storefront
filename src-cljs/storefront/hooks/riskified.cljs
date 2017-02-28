(ns storefront.hooks.riskified
  (:require [storefront.browser.tags :refer [insert-tag-with-src remove-tags-by-class]]
            [storefront.hooks.exception-handler :as exception-handler]))

(def store-domain "mayvenn.com")

(defn insert-tracking [session-id]
  (insert-tag-with-src
   (str "//beacon.riskified.com?shop=" store-domain "&sid=" session-id)
   "riskified-beacon"))

(defn remove-tracking []
  (remove-tags-by-class "riskified-beacon"))

(defn track-page [path]
  (when (.hasOwnProperty js/window "RISKX")
    (try
      (.go js/RISKX (clj->js path))
      (catch :default e
        (exception-handler/report e)))))
