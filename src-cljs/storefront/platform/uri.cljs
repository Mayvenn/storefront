(ns storefront.platform.uri
  (:require [cemerick.url :refer [map->query]])
  (:import [goog Uri]))

(defn set-query-string [s query-params]
  (-> (Uri.parse s)
      (.setQueryData (map->query (if (seq query-params)
                                   query-params
                                   {}))
                     true)
      .toString))
