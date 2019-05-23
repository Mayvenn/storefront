(ns storefront.platform.uri
  (:import [org.apache.http.client.utils URIBuilder]
           [org.apache.http.message BasicNameValuePair]))


(defn set-query-string [s query-params]
  (if (seq query-params)
    (.. (URIBuilder. s)
        (setParameters (map (fn [[key value]]
                              (BasicNameValuePair. (name key) (str value)))
                            query-params))
        (build)
        (toString))
    s))
