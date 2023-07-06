(ns storefront.hooks.calendly
  (:require [storefront.browser.tags :as tags]
            [storefront.platform.messages :as messages]
            [storefront.events :as e]
            cljs.core))


(defn insert []
  "https://assets.calendly.com/assets/external/widget.css"
  (tags/insert-tag-with-callback
   (tags/src-tag "https://assets.calendly.com/assets/external/widget.js"
                 "calendly")
   #(messages/handle-message e/inserted-calendly)))
