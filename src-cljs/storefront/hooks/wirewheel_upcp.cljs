(ns storefront.hooks.wirewheel-upcp
  (:require [storefront.browser.tags :as tags]
            [storefront.platform.messages :as messages]
            [storefront.events :as events]
            cljs.core
            spice.core))

(defn ^:private loaded? [] (cljs.core/exists? js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent))
(defn ^:private iframe-mounted? [] (js/document.getElementById "wwiframe"))

;; The UPCP has some difficult initialization characteristics. To work properly the following
;; events must occur in order:
;; 1) The ww iframe must be mounted without its src set - AND - the ww UPCP sdk must be loaded
;; 2) The `initEmbeddedParent` function must be called
;; 3) The ww iframe must have its src set (so that its JS code will run)
;; If the iframe hasn't been "init"ed before its JS code can run it will result in an error page
;; with the title "We can't find the page you're looking for."

(defn init-iframe []
  (prn "init-iframe")
  (when (and (spice.core/spy "init-iframe - SDK loaded?" (loaded?))
             (spice.core/spy "init-iframe - iframe-mounted?" (iframe-mounted?)))
    (prn "init-iframe - actually initing")
    (js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent
     #js {:targetIframe (spice.core/spy "init-iframe - wwiframe" (js/document.getElementById "wwiframe"))})
    (messages/handle-message events/inited-wirewheel-upcp)))

(defn insert []
  (prn "insert")
  (when-not (loaded?)
    (js/window.addEventListener "message"
                                (fn [e]
                                  (when (= "https://ui.upcp.wirewheel.io" (.-origin e))
                                    (spice.core/spy "postMessage" e)))
                                false)
    (prn "insert - adding SDK tag")
    (tags/insert-tag-with-callback
     (tags/src-tag "https://ui.upcp.wirewheel.io/extensions/upcp-sdk-0.8.3.min.js"
                   "ww-upcp")
     (fn []
       (messages/handle-message events/inserted-wirewheel-upcp)
       (init-iframe)))))
