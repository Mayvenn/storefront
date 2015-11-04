(ns storefront.hooks.analytics
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags remove-tag-by-src]]
            [storefront.config :as config]
            [clojure.string :as s]))

(defn insert-tracking []
  (insert-tag-with-text
   (str "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
 (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
 m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
 })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', '" config/google-analytics-property "', 'auto');
ga('require', 'displayfeatures');

// Optimizely Universal Analytics Integration
window.optimizely = window.optimizely || [];
window.optimizely.push('activateUniversalAnalytics');")
   "analytics")
  (insert-tag-with-text
   "(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    '//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','GTM-TLS2JL');"
   "tag-manager"))

(defn remove-tracking []
  (remove-tags "analytics")
  (remove-tags "tag-manager")
  ;; ga inserts more tags (as expected); remove them to help prevent so many additional ones in development
  (remove-tag-by-src "//www.google-analytics.com/analytics.js")
  (remove-tag-by-src "//www.googletagmanager.com/gtm.js?id=GTM-TLS2JL"))

(defn track-event [category action & [label value non-interaction]]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "send"
           "event"
           category
           action
           label
           value
           (when non-interaction (clj->js {"nonInteraction" (str non-interaction)})))))

(defn track-page [path]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "set" "page" (clj->js path))
    (js/ga "send" "pageview")))
