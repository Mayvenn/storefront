(ns storefront.hooks.stringer
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]))

(defn insert-tracking []
  (insert-tag-with-text
   (str "(function(d,e){function g(a){return function(){var b=Array.prototype.slice.call(arguments);b.unshift(a);c.push(b);return d.stringer}}var c=d.stringer=d.stringer||[],a=[\"init\",\"track\",\"identify\",\"clear\"];if(!c.snippetRan&&!c.loaded){c.snippetRan=!0;for(var b=0;b<a.length;b++){var f=a[b];c[f]=g(f)}a=e.createElement(\"script\");a.type=\"text/javascript\";a.async=!0;a.src=\"//d6w7wdcyyr51t.cloudfront.net/cdn/stringer/stringer-a657521.js\";b=e.getElementsByTagName(\"script\")[0];b.parentNode.insertBefore(a,b);c.init({environment:\"" config/environment "\",sourceSite:\"storefront\"})}})(window,document);")
   "stringer"))

(defn remove-tracking []
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
