(ns storefront.hooks.reviews
  (:require [storefront.browser.tags :refer [src-tag insert-tag-with-callback]]
            [storefront.messages :refer [send]]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]
            [storefront.events :as events]))

(def ^:private tag-class "product-review-tag")

(defn insert-reviews [data]
  (when-not (aget (.getElementsByClassName js/document tag-class) 0)
    (insert-tag-with-callback
     (src-tag config/review-tag-url tag-class)
     #(send data events/inserted-reviews))))

(defn start []
  (when (and (.hasOwnProperty js/window "yotpo") js/yotpo)
    (set! (.-initialized js/yotpo) false)
    (.init js/yotpo)))

(defn stop []
  (when (and (.hasOwnProperty js/window "yotpo") js/yotpo)
    (.pop (.-widgets js/yotpo))
    (.pop (.. js/yotpo -callbacks -ready))))
