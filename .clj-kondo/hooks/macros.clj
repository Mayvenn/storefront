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
