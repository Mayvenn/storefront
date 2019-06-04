(ns storefront.hooks.twitter-analytics
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]))

(defn insert-tracking []
  (insert-tag-with-text
   (str
    "!function(e,t,n,s,u,a){e.twq||(s=e.twq=function(){s.exe?s.exe.apply(s,arguments):s.queue.push(arguments);
},s.version='1.1',s.queue=[],u=t.createElement(n),u.async=!0,u.src='//static.ads-twitter.com/uwt.js',
a=t.getElementsByTagName(n)[0],a.parentNode.insertBefore(u,a))}(window,document,'script');
twq('init','" config/twitter-pixel-id "');")
   "twitter-pixel"))

(defn remove-tracking []
  (remove-tags-by-class "twitter-pixel"))

(defn track-event
  ([action]
   (when (.hasOwnProperty js/window "twq")
     (js/twq "track" action)))
  ([action args]
   (when (.hasOwnProperty js/window "twq")
     (js/twq "track" action (clj->js args)))))

(defn track-custom-event
  ([action]
   (when (.hasOwnProperty js/window "twq")
     (js/twq "trackCustom" action)))
  ([action args]
   (when (.hasOwnProperty js/window "twq")
     (js/twq "trackCustom" action (clj->js args)))))

(defn track-page [path]
  (track-event "PageView"))
