(ns storefront.jetty
  (:import (org.eclipse.jetty.server.handler.gzip GzipHandler)))

(defn configurator [server]
  (.setHandler server (doto (GzipHandler.)
                        (.setHandler (.getHandler server))
                        (.setIncludedMimeTypes (into-array String ["text/html"
                                                                   "text/plain"
                                                                   "text/xml"
                                                                   "application/xhtml+xml"
                                                                   "text/css"
                                                                   "application/javascript"
                                                                   "text/javascript"
                                                                   "image/svg+xml"])))))
