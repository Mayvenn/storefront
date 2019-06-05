(ns storefront.hooks.js-qr
  (:require [storefront.assets :as assets]
            [storefront.browser.tags :as tags]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]))

(defn insert []
  (when-not (.hasOwnProperty js/window "jsQR")
    (tags/insert-tag-with-callback (tags/src-tag (str "/" (assets/path "js/out/src-cljs/storefront/jsQR.js")) "jsQR")
                                   (fn []
                                     (handle-message events/inserted-jsQR)))))
