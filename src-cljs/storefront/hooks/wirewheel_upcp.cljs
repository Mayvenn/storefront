(ns storefront.hooks.wirewheel-upcp
  (:require [storefront.browser.tags :as tags]
            [storefront.platform.messages :as messages]
            [storefront.events :as e]
            cljs.core
            spice.core))

(defn sdk-loaded? [] (cljs.core/exists? js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent))
(defn ^:private iframe-mounted? [] (js/document.getElementById "wwiframe"))

(defn logstuf []
  (println "loaded? " (sdk-loaded?))
  (println "iframe-mounted? " (boolean (iframe-mounted?))))
(defn init-iframe []
  (println "init-iframe") 
  (logstuf)
  (js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent
   #js {:targetIframe (js/document.getElementById "wwiframe")})
  (messages/handle-message e/initialized-wirewheel-upcp))

(defn insert [] 
  (when-not (sdk-loaded?)
    (println "inserting SDK tag")
    (tags/insert-tag-with-callback
     (tags/src-tag "https://ui.upcp.wirewheel.io/extensions/upcp-sdk-0.8.3.min.js"
                   "ww-upcp")
     #(messages/handle-message e/inserted-wirewheel-upcp))))