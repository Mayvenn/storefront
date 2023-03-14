(ns storefront.hooks.wirewheel-upcp
  (:require [storefront.browser.tags :as tags]
            storefront.config
            spice.core))

(defn ^:private loaded? [] (.hasOwnProperty js/window "cmpJavascriptSdk"))
(defn ^:private iframe-mounted? [] (js/document.getElementById "wwiframe"))

(defn init-iframe []
  (when (and (loaded?)
             (iframe-mounted?))
    (js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent
     #js {:targetIframe (js/document.getElementById "wwiframe")})))

(defn insert []
  (when-not (loaded?)
    (tags/insert-tag-with-callback
     (tags/src-tag storefront.config/wirewheel-upcp-url
                   "ww-upcp")
     init-iframe)))
