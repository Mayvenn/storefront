(ns storefront.uri
  (:require [cemerick.url :as cemerick-url])
  (:import #?@(:cljs [[goog Uri]]
               :clj [[org.apache.http.client.utils URIBuilder]
                     [org.apache.http.message BasicNameValuePair]])))

(defn map->query [m]
  ;; We don't use cemerick's implementation because it undoes the sorting we'd
  ;; like to perform on the query params
  (some->> (seq m)
           (map (fn [[k v]]
                  [(cemerick-url/url-encode (name k))
                   "="
                   (cemerick-url/url-encode (str v))]))
           (interpose "&")
           flatten
           (apply str)))

(defn set-query-string [s query-params]
  #?(:cljs (-> (Uri.parse s)
               (.setQueryData (map->query (if (seq query-params)
                                            query-params
                                            {}))
                              true)
               .toString)
     :clj (if (seq query-params)
            (.. (URIBuilder. s)
                (setParameters
                 (map (fn [[key value]]
                        (BasicNameValuePair. (name key) (str value)))
                      query-params))
                (build)
                (toString))
            s)))
