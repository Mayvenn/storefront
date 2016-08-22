(ns storefront.hooks.wistia
  (:require [storefront.browser.tags :as tags]))

(defn ^:private js-loaded? []
  (.hasOwnProperty js/window "Wistia"))

(defn attach [video-id]
  (tags/insert-tag-with-src (str "//fast.wistia.com/embed/medias/" video-id ".jsonp") (str "wistia_" video-id))
  (when-not (js-loaded?)
    (tags/insert-tag-with-src "//fast.wistia.com/assets/external/E-v1.js" "wistia")))

(defn detach [video-id]
  (when (js-loaded?)
    (.. js/Wistia
        (api video-id)
        remove)))

