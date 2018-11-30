(ns storefront.hooks.reviews
  (:require [storefront.browser.tags :refer [src-tag insert-tag-with-callback]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.config :as config]
            [storefront.events :as events]))

(def ^:private tag-class "product-review-tag")

(defn insert-reviews []
  (when-not (aget (.getElementsByClassName js/document tag-class) 0)
    (insert-tag-with-callback
     (src-tag config/review-tag-url tag-class)
     #(.init js/yotpo))))

(defn start []
  (when (and (.hasOwnProperty js/window "yotpo") js/yotpo)
    (.refreshWidgets js/yotpo)))
