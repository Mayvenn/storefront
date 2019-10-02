(ns storefront.hooks.pinterest
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]))

(defn insert-tracking []
  #_
  (insert-tag-with-text
   (str "!function(e){if(!window.pintrk){window.pintrk=function(){window.pintrk.queue.push(Array.prototype.slice.call(arguments))};var n=window.pintrk;n.queue=[],n.version='3.0';var t=document.createElement('script');t.async=!0,t.src=e;var r=document.getElementsByTagName('script')[0];r.parentNode.insertBefore(t,r)}}('https://s.pinimg.com/ct/core.js');pintrk('load','" config/pinterest-tag-id "');pintrk('page');")
   "pinterest-pixel"))

(defn remove-tracking []
  #_
  (remove-tags-by-class "pinterest-pixel"))

(defn track-page
  ([]
   (when (.hasOwnProperty js/window "pintrk")
     (js/pintrk "track" "pagevisit")))
  ([data]
   (when (.hasOwnProperty js/window "pintrk")
     (js/pintrk "track" "pagevisit" data))))

(defn track-event
  ([action]
   (when (.hasOwnProperty js/window "pintrk")
     (js/pintrk "track" action)))
  ([action args]
   (when (.hasOwnProperty js/window "pintrk")
     (js/pintrk "track" action (clj->js args)))))
