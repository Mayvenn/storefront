(ns storefront.jetty
  (:import (org.eclipse.jetty.server.handler.gzip GzipHandler)
           (org.eclipse.jetty.server.handler StatisticsHandler)))

(def stale-reads-timeout 6000)
(def shutdown-timeout (- 29000 stale-reads-timeout))

(defn configure-graceful-shutdown [server]
  (.setHandler server (doto (StatisticsHandler.)
                        (.setHandler (.getHandler server))))
  (.setStopTimeout server shutdown-timeout))

(defn configure-gzip-handler [server]
  (.setHandler server (doto (GzipHandler.)
                        (.setHandler (.getHandler server))
                        (.setIncludedMimeTypes (into-array String ["text/html"
                                                                   "text/plain"
                                                                   "text/xml"
                                                                   "application/xhtml+xml"
                                                                   "text/css"
                                                                   "application/javascript"
                                                                   "application/json"
                                                                   "text/javascript"
                                                                   "image/svg+xml"])))))

(defn configurator [server]
  (doto server
    configure-gzip-handler
    configure-graceful-shutdown))
