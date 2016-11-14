(ns storefront.hooks.stringer
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]))

(defn insert-tracking []
  (insert-tag-with-text
   (str "(function() {var stringer = window.stringer = window.stringer || [],methods = ['init', 'track', 'identify', 'clear']; function stub(method) {return function() {var args = Array.prototype.slice.call(arguments); args.unshift(method); stringer.push(args); return stringer;};} for (var i = 0; i < methods.length; i++) {var k = methods[i]; stringer[k] = stub(k);}

var script = document.createElement('script'); script.type = 'text/javascript'; script.async = true; script.src = '" config/stringer-src "';

var first = document.getElementsByTagName('script')[0]; first.parentNode.insertBefore(script, first);

stringer.init({serverURI: '" config/byliner-uri "',sourceSite: 'storefront'});})(); ")
   "stringer"))

(defn remove-tracking []
  (js-delete js/window "stringer")
  (remove-tags-by-class "stringer")
  (remove-tag-by-src config/stringer-src))

(defn track-event [event-name payload]
  (.track js/window.stringer event-name (clj->js payload)))

(defn track-page [path]
  (when (.hasOwnProperty js/window "stringer")
    (track-event "pageview" {:path path})))

(defn track-identify [{:keys [id email]}]
  (when (.hasOwnProperty js/window "stringer")
    (.identify js/window.stringer email id)))

(defn track-clear []
  (when (.hasOwnProperty js/window "stringer")
    (.clear js/window.stringer)))
