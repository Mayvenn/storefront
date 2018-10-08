(ns storefront.hooks.stringer
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]
            [storefront.platform.messages :refer [handle-message]]))

(def stringer-src "//d6w7wdcyyr51t.cloudfront.net/cdn/stringer/stringer-8c70fb0.js")

(defn insert-tracking [subdomain]
  (let [source (if (= subdomain "freeinstall")
                 "freeinstall"
                 "storefront")]
    (insert-tag-with-text
     (str "(function(d,e){function g(a){return function(){var b=Array.prototype.slice.call(arguments);b.unshift(a);c.push(b);return d.stringer}}var c=d.stringer=d.stringer||[],a=[\"init\",\"track\",\"identify\",\"clear\"];if(!c.snippetRan&&!c.loaded){c.snippetRan=!0;for(var b=0;b<a.length;b++){var f=a[b];c[f]=g(f)}a=e.createElement(\"script\");a.type=\"text/javascript\";a.async=!0;a.src=\"" stringer-src "\";b=e.getElementsByTagName(\"script\")[0];b.parentNode.insertBefore(a,b);c.init({environment:\"" config/environment "\",sourceSite:\""source"\"})}})(window,document);")
     "stringer")))

(defn remove-tracking []
  (remove-tags-by-class "stringer")
  (remove-tag-by-src stringer-src))

(defn track-event
  ([event-name] (track-event event-name {} nil))
  ([event-name payload] (track-event event-name payload nil))
  ([event-name payload callback-event] (track-event event-name payload callback-event nil))
  ([event-name payload callback-event callback-args]
   (when (.hasOwnProperty js/window "stringer")
     (.track js/stringer event-name (clj->js payload)
             (when callback-event
               (fn [] (handle-message callback-event (merge {:tracking-event event-name :payload payload} callback-args))))))))

(defn track-page [store-experience]
  (track-event "pageview" {:store-experience store-experience}))

(defn identify [{:keys [id email]}]
  (when (.hasOwnProperty js/window "stringer")
    (.identify js/stringer email id)))

(defn track-clear []
  (when (.hasOwnProperty js/window "stringer")
    (.clear js/stringer)
    (.track js/stringer "clear_identify")))

(defn browser-id []
  (when (and (.hasOwnProperty js/window "stringer")
             js/stringer.loaded)
    (.getBrowserId js/stringer)))
