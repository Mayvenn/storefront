(ns user
  (:use clojure.test)
  (:require [clojure.pprint :refer (pprint)]
            [figwheel-sidecar.repl-api :as figwheel]
            [storefront.system :as system]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [dev-system :refer [the-system]]))

(defn init
  []
  (alter-var-root #'the-system
                  (constantly (system/create-system))))

(defn start [] (alter-var-root #'the-system component/start))

(defn stop
  []
  (alter-var-root #'the-system
                  (fn [s] (when s (component/stop s)))))

(defn go
  []
  (init)
  (start))

(defn reset
  []
  (stop)
  (refresh :after 'user/go))

(defn cljs []
  (figwheel/cljs-repl))
