(ns storefront.server
  (:import [org.eclipse.jetty.server.handler GzipHandler]))

(def gzip-mime-types "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,text/javascript,image/svg+xml,application/json,application/clojure")

(defn configurator [server]
  (.setHandler server
               (doto (new GzipHandler)
                 (.setMimeTypes gzip-mime-types)
                 (.setHandler (.getHandler server)))))
