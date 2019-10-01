(ns storefront.hooks.google-analytics
  (:require [storefront.browser.tags :refer [insert-tag-with-text remove-tags-by-class remove-tag-by-src]]
            [storefront.config :as config]))

(defn insert-tracking []
  #_
  (insert-tag-with-text
   (str "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
 (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
 m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
 })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', '" config/google-analytics-property "', 'auto');
ga('require', 'displayfeatures');")
   "analytics")
  #_
  (insert-tag-with-text
   "(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    '//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','GTM-TLS2JL');"
   "tag-manager"))

(defn remove-tracking []
  #_
  (remove-tags-by-class "analytics")
  #_
  (remove-tags-by-class "tag-manager")
  ;; ga inserts more tags (as expected); remove them to help prevent so many additional ones in development
  #_
  (remove-tag-by-src "//www.google-analytics.com/analytics.js")
  #_
  (remove-tag-by-src "//www.googletagmanager.com/gtm.js?id=GTM-TLS2JL"))

(defn track-event [category action & [label value]]
  {:pre [(if label (number? value) true)]}
  (when (.hasOwnProperty js/window "ga")
    (js/ga "send"
           (clj->js {"hitType" "event"
                     "eventCategory" category
                     "eventAction" action
                     "eventLabel" label
                     "eventValue" value}))))

(defn track-page [path]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "set" "page" (clj->js path))
    (js/ga "send" "pageview")))

(defn set-dimension [key value]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "set" (str key) (str value))))
