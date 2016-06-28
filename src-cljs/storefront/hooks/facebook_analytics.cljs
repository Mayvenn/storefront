(ns storefront.hooks.facebook-analytics
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]
            [clojure.string :as s]))

(defn insert-tracking []
  (insert-tag-with-text
   "!function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,
document,'script','https://connect.facebook.net/en_US/fbevents.js');
fbq('init', '721931104522825');"
   "fb-pixel"))

(defn remove-tracking []
  (remove-tags-by-class "fb-pixel")
  ;; fb inserts more tags (as expected); remove them to help prevent so many additional ones in development
  (remove-tag-by-src "//connect.facebook.net/en_US/fbevents.js"))

(defn track-event [category action & [label value]])

(defn track-page [path]
  (when (.hasOwnProperty js/window "fbq")
    (js/fbq "track" "PageView")))
