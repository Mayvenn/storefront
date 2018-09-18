(ns storefront.hooks.facebook-analytics
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]))

(defn insert-tracking []
  (insert-tag-with-text
   (str
    "!function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,
document,'script','https://connect.facebook.net/en_US/fbevents.js');
fbq('init', '" config/facebook-pixel-id "');")
   "fb-pixel"))

(defn remove-tracking []
  (remove-tags-by-class "fb-pixel")
  ;; fb inserts more tags (as expected); remove them to help prevent so many additional ones in development
  (remove-tag-by-src "//connect.facebook.net/en_US/fbevents.js"))

(defn track-event
  ([action]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "track" action)))
  ([action args]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "track" action (clj->js args)))))

(defn track-custom-event
  ([action]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "trackCustom" action)))
  ([action args]
   (when (.hasOwnProperty js/window "fbq")
     (js/fbq "trackCustom" action (clj->js args)))))

(defn track-page [path]
  (track-event "PageView"))

(defn subscribe []
  (track-event "Subscribe"))
