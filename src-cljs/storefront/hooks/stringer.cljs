(ns storefront.hooks.stringer
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class]]))

(def stringer-src "/js/stringer.js")
(def server-uri "http://localhost:3013")

(defn insert-tracking []
  (insert-tag-with-text
   (str "(function() {var stringer = window.stringer = window.stringer || [],methods = ['init', 'track']; function stub(method) {return function(...arguments) {var args = Array.prototype.slice.call(arguments); args.unshift(method); console.log(args); stringer.push(args); console.log('stringer: ', stringer); return stringer;};} for (var i = 0; i < methods.length; i++) {var k = methods[i]; stringer[k] = stub(k);}

var script = document.createElement('script'); script.type = 'text/javascript'; script.async = true; script.src = '" stringer-src "';

var first = document.getElementsByTagName('script')[0]; first.parentNode.insertBefore(script, first);

stringer.init({debug: true,serverURI: '" server-uri "',sourceSite: 'storefront'});})(); ") "stringer"))

(defn remove-tracking [])

(defn track-event [event-name payload]
  (.track js/window.stringer event-name (clj->js payload)))

(defn track-page [path]
  (when (.hasOwnProperty js/window "stringer")
    (track-event "pageview" {:path path})))

(defn track-identify [{:keys [id email]}]
  (when (.hasOwnProperty js/window "stringer")
    (.identify js/window.stringer email id)))
