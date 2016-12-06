(ns storefront.hooks.wistia
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.platform.messages :as m]))

(defn ^:private js-loaded? []
  (.hasOwnProperty js/window "Wistia"))

(defn load []
  (when-not (js-loaded?)
    (tags/insert-tag-with-src "//fast.wistia.com/assets/external/E-v1.js" "wistia")
    (or (.hasOwnProperty js/window "_wq")
        (set! (.-_wq js/window) (clj->js [])))
    (.push js/_wq (clj->js {:id "_all"
                            :onReady (fn [video]
                                       (.bind video "play" #(m/handle-message events/video-played {:video-id (.hashedId video)})))}))))

(defn attach [video-id]
  (tags/insert-tag-with-src (str "//fast.wistia.com/embed/medias/" video-id ".jsonp") (str "wistia_" video-id))
  (load))

(defn detach [video-id]
  (when (js-loaded?)
    (when-let [player (.api js/Wistia video-id)]
      (.remove player))))
