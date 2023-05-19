(ns storefront.hooks.wirewheel-upcp
  (:require [storefront.browser.tags :as tags]
            [storefront.platform.messages :as messages]
            [storefront.events :as e]
            cljs.core
            spice.core))


(defn insert [] 
  (when-not (cljs.core/exists? js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent)
    (tags/insert-tag-with-callback
     (tags/src-tag "https://s.upcp.wirewheel.io/sdk/upcp-sdk-0.9.2.min.js"
                   "ww-upcp")
     #(messages/handle-message e/inserted-wirewheel-upcp))))
