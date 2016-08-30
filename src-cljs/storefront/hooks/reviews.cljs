(ns storefront.hooks.reviews
  (:require [storefront.browser.tags :refer [src-tag insert-tag-with-callback]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]
            [storefront.events :as events]))

(def ^:private tag-class "product-review-tag")

(defn insert-reviews []
  (when-not (aget (.getElementsByClassName js/document tag-class) 0)
    (insert-tag-with-callback
     (src-tag config/review-tag-url tag-class)
     #(handle-message events/inserted-reviews))))

(defn start []
  (when (and (.hasOwnProperty js/window "yotpo") js/yotpo)
    (set! (.-initialized js/yotpo) false)
    (.init js/yotpo)))

(defn stop []
  (when (and (.hasOwnProperty js/window "yotpo") js/yotpo)
    (.splice (.-widgets js/yotpo) 0 (.. js/yotpo -widgets -length))
    (when-let [ready-callbacks (and (.-callbacks js/yotpo) (.. js/yotpo -callbacks -ready))]
      (.pop ready-callbacks))))
