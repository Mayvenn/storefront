(ns storefront.analytics
  (:require [storefront.script-tags :refer [insert-tag-with-text remove-tag]]))

(defn insert-tracking []
  (insert-tag-with-text
   "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
 (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
 m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
 })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', 'UA-36226630-1', 'auto');
ga('require', 'displayfeatures');"
   "analytics")
  (insert-tag-with-text
   "(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    '//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','GTM-TLS2JL');"
   "tag-manager"))

(defn remove-tracking []
  (remove-tag "analytics")
  (remove-tag "tag-manager")
  ;; ga inserts more tags (as expected); remove them to help prevent so many additional ones in development
  (when-let [additional-tracking-tag (aget (.querySelector js/document "[src=\"//www.google-analytics.com/analytics.js\"]") 0)]
    (.remove additional-tracking-tag)))

(defn track-page [path]
  (js/ga "set" "page" (clj->js path))
  (js/ga "send" "pageview"))
