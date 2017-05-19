(ns storefront.components.share-links
  (:require [cemerick.url :as url]))

(defn with-utm-medium [share-url medium]
  (assoc-in share-url [:query :utm_medium] medium))

(defn facebook-link [share-url]
  (-> (url/url "https://www.facebook.com/sharer/sharer.php")
      (assoc :query {:u share-url})
      str))

(defn sms-link [body]
  ;; the ?& is to get this to work on iOS8 and Android at the same time
  (str "sms:?&body=" (url/url-encode body)))

(defn twitter-link [share-url tweet]
  (-> (url/url "https://twitter.com/intent/tweet")
      (assoc :query {:url      share-url
                     :text     tweet
                     :hashtags "mayvennhair"})
      str))

(defn email-link [subject body]
  (str "mailto:?Subject=" (url/url-encode subject) "&body=" (url/url-encode body)))
