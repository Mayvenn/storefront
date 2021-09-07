(ns user
  (:use clojure.test)
  (:require [clojure.pprint :refer (pprint)]
            [storefront.system :as system]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all) :as repl-tools]
            [dev-system :refer [the-system]]))

(defn get-source-paths []
  (->> (slurp "project.clj")
       read-string
       (drop-while (partial not= :source-paths))
       second))

(defn init
  []
  (alter-var-root #'the-system
                  (constantly (system/create-system))))

(defn start [] (alter-var-root #'the-system component/start) :start)

(defn stop
  []
  (alter-var-root #'the-system
                  (fn [s] (when s (component/stop s))))
  :stop)

(defn go
  []
  (init)
  (start))

(defn reset
  []
  (apply repl-tools/set-refresh-dirs (get-source-paths))
  (stop)
  (refresh :after 'user/go)
  ;; TODO: at-at/MutablePool seems to have an infinite recursive print
  nil)
