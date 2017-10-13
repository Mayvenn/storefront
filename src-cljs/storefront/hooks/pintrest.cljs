(ns storefront.hooks.pintrest
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]
            [clojure.string :as s]))

(def pintrest-tag-id
  (if (not= "production" js/environment)
    ;;TODO GROT when/if we get a sandbox account
    "A Fake Development tag id"
    2617847432239))

(defn insert-tracking []
  (insert-tag-with-text
   (str "!function(e){if(!window.pintrk){window.pintrk=function(){window.pintrk.queue.push(Array.prototype.slice.call(arguments))};var n=window.pintrk;n.queue=[],n.version='3.0';var t=document.createElement('script');t.async=!0,t.src=e;var r=document.getElementsByTagName('script')[0];r.parentNode.insertBefore(t,r)}}('https://s.pinimg.com/ct/core.js');pintrk('load','" pintrest-tag-id "');pintrk('page');")
   "pintrest-pixel"))

(defn remove-tracking []
  (remove-tags-by-class "pintrest-pixel"))

(defn track-page
  ([]
   (js/pintrk "track" "pagevisit"))
  ([data]
   (js/pintrk "track" "pagevisit" data)))
