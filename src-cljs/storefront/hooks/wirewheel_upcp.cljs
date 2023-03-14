(ns storefront.hooks.wirewheel-upcp
  (:require [storefront.browser.tags :as tags]
            cljs.core
            spice.core))

(defn ^:private loaded? [] (cljs.core/exists? js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent))
(defn ^:private iframe-mounted? [] (js/document.getElementById "wwiframe"))

(defn init-iframe []
  (when (and (loaded?)
             (iframe-mounted?))
    (js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent
     #js {:targetIframe (js/document.getElementById "wwiframe")})))

(defn insert []
  (when-not (loaded?)
    (tags/insert-tag-with-callback
     (tags/src-tag "https://ui.upcp.wirewheel.io/extensions/upcp-sdk-0.8.3.min.js"
              "ww-upcp")
     init-iframe)))
