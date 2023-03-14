(ns storefront.hooks.wirewheel-upcp
  (:require [storefront.browser.tags :as tags]
            spice.core))

(defn ^:private loaded? [] (and (.hasOwnProperty js/window "cmpJavascriptSdk")
                                (.hasOwnProperty js/window.cmpJavascriptSdk "WireWheelSDK")
                                (.hasOwnProperty js/window.cmpJavascriptSdk.WireWheelSDK "initEmbeddedParent")))
(defn ^:private iframe-mounted? [] (js/document.getElementById "wwiframe"))

(defn init-iframe []
  (prn "INITING")
  (when (and (loaded?)
             (iframe-mounted?))
    (prn "RLY INITING")
    (js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent
     #js {:targetIframe (js/document.getElementById "wwiframe")})))

(defn insert []
  (prn "INSERTING")
  (when-not (loaded?)
    (prn "RLY INSERTING")
    (tags/insert-tag-with-callback
     (tags/src-tag "https://ui.upcp.wirewheel.io/extensions/upcp-sdk-0.8.3.min.js"
              "ww-upcp")
     init-iframe)))
