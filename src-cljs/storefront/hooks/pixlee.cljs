(ns storefront.hooks.pixlee
  (:require [storefront.events :as events]
            [storefront.config :as config]
            [storefront.browser.tags :refer [insert-tag-pair remove-tag-pair]]
            [storefront.platform.messages :as m]))

(defn loaded? []
  (js/window.hasOwnProperty "Pixlee"))

(defn insertion-callback-str []
  (str "window.PixleeAsyncInit=function(){"
       "Pixlee.init({apiKey:'" (:api-key config/pixlee) "'});"
       "};"))

(defn insert
  []
  (when-not (loaded?)
    (set! (.-PixleeAsyncInit js/window)
          (fn []
            (m/handle-message events/inserted-pixlee)))
    (insert-tag-pair
     "//assets.pixlee.com/assets/pixlee_widget_1_0_0.js"
     (insertion-callback-str)
     "pixlee-widget")))

(defn remove-tracking
  []
  (remove-tag-pair "pixlee-widget"))

(defn widget-config []
  (clj->js {:widgetId (:mayvenn-made-widget-id config/pixlee)}))

(defn open-uploader []
  (when (loaded?)
    (js/Pixlee.openUploader (widget-config))))

(defn add-simple-widget
  []
  (when (loaded?)
    (js/Pixlee.addSimpleWidget (widget-config))))
