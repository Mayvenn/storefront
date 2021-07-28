(ns hooks.macros
  (:require [clj-kondo.hooks-api :as api]))

(defn defpath [{:keys [node]}]
  (let [sym      (second (:children node))
        new-node (api/list-node
                  (list
                   (api/token-node 'def)
                   sym
                   (api/vector-node [(api/keyword-node (keyword (:string-value sym)))])))]
    {:node new-node}))

(defn with-handler [{:keys [node]}]
  (let [[handler-sym & body] (rest (:children node))
        new-node             (api/list-node
                              (list*
                               (api/token-node 'let)
                               (api/vector-node [handler-sym '{}])
                               body))]
    {:node new-node}))

(defn with-services [{:keys [node]}]
  (let [[config & body] (rest (:children node))
        new-node         (api/list-node
                              (list*
                               (api/token-node 'do)
                               config
                               body))]
    {:node new-node}))
