(ns storefront.hooks.segment-analytics
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]
            [clojure.string :as s]
            [clojure.string :as str]))

(defn insert-tracking []
  (insert-tag-with-text
   (str "!function(){var analytics=window.analytics=window.analytics||[];if(!analytics.initialize)if(analytics.invoked)window.console&&console.error&&console.error(\"Segment snippet included twice.\");else{analytics.invoked=!0;analytics.methods=[\"trackSubmit\",\"trackClick\",\"trackLink\",\"trackForm\",\"pageview\",\"identify\",\"reset\",\"group\",\"track\",\"ready\",\"alias\",\"page\",\"once\",\"off\",\"on\"];analytics.factory=function(t){return function(){var e=Array.prototype.slice.call(arguments);e.unshift(t);analytics.push(e);return analytics}};for(var t=0;t<analytics.methods.length;t++){var e=analytics.methods[t];analytics[e]=analytics.factory(e)}analytics.load=function(t){var e=document.createElement(\"script\");e.type=\"text/javascript\";e.async=!0;e.src=(\"https:\"===document.location.protocol?\"https://\":\"http://\")+\"cdn.segment.com/analytics.js/v1/\"+t+\"/analytics.min.js\";var n=document.getElementsByTagName(\"script\")[0];n.parentNode.insertBefore(e,n)};analytics.SNIPPET_VERSION=\"3.1.0\";
  analytics.load(\"" config/segment-write-key "\");
  }}(); ")
   "segment-analytics"))

(defn remove-tracking []
  (remove-tags-by-class "segment-analytics"))

(defn track-event [event-name props]
  (when (.hasOwnProperty js/window "analytics")
    (js/analytics.track event-name (clj->js props))))

(defn track-page []
  (when (.hasOwnProperty js/window "analytics")
    (js/analytics.page)))

(defn identify [user]
  (when (.hasOwnProperty js/window "analytics")
    (js/analytics.identify (or (:id user) (:email user))
                           (clj->js {:email (:email user)}))))
