(ns storefront.components.share-links
  (:require [cemerick.url :as url]
            [clojure.string :as string]))

(defn with-utm-medium [share-url medium]
  (assoc-in share-url [:query :utm_medium] medium))

(defn facebook-link [share-url]
  (-> (url/url "https://www.facebook.com/sharer/sharer.php")
      (assoc :query {:u share-url})
      str))

(defn sms-link
  ([body] (sms-link body nil))
  ([body number]
   ;; NOTE: the ?& is to get this to work on iOS8 and Android at the same time
   ;; NOTE: Android Messenger crashes if %25 is included in body, so we're using Full-Width Percent Sign
   (str "sms:" number "?&body=" (string/replace (url/url-encode body) "%25" "%EF%BC%85"))))

(defn twitter-link [share-url tweet]
  (-> (url/url "https://twitter.com/intent/tweet")
      (assoc :query {:url      share-url
                     :text     tweet
                     :hashtags "mayvennhair"})
      str))

(defn email-link [subject body]
  (str "mailto:?Subject=" (url/url-encode subject) "&body=" (url/url-encode body)))
