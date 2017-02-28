(ns storefront.hooks.sift
  (:require [storefront.browser.tags :refer [insert-tag-with-src remove-tags-by-class]]
            [storefront.hooks.exception-handler :as exception-handler]))

(def store-domain ".mayvenn.com")

(defn insert-beacon []
  (insert-tag-with-text
   (str "var _sift=window._sift=window._sift||[];_sift.push(['_setAccount','" config/sift-api-key "']),function(t,n){function c(){var n=t.createElement('script');n.src='https://cdn.siftscience.com/s.js',t.body.appendChild(n)}n.attachEvent?n.attachEvent('onload',c):n.addEventListener('load',c,!1)}(document,window);")
   "sift"))

(defn remove-beacon []
  (remove-tags-by-class "sift"))

(defn track-page [path user-id session-id]
  (when (.hasOwnProperty js/window "_sift")
    (try
      (doto js/_sift
        (.push (clj->js ["_setUserID" user-id]))
        (.push (clj->js ["_setCookieDomain" store-domain]))
        (.push (clj->js ["_setSessionId" session-id]))
        (.push (clj->js ["_trackPageview"])))
      (catch :default e
        (exception-handler/report e))))
