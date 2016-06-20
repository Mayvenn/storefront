(ns storefront.platform.uri
  (:import [org.apache.http.client.utils URIBuilder]
           [org.apache.http.message BasicNameValuePair]))


(defn set-query-string [s query-params]
  (if (seq query-params)
    (.. (URIBuilder. s)
        (setParameters (map #(BasicNameValuePair. %1 %2) query-params))
        (build)
        (toString))
    s))
