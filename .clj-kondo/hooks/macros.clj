(ns hooks.macros
  (:require
   [clj-kondo.hooks-api :as api]))

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

(defn defdynamic-component [{:keys [node]}]
  (let [[name & methods] (rest (:children node))
        fns              (into {}
                               (comp
                                (map :children)
                                (map (fn [[name binding & body]]
                                       [(keyword (str name))
                                        (list*
                                         (api/token-node 'fn)
                                         binding
                                         body)])))
                               methods)
        new-node         (api/list-node
                          (list
                           (api/token-node 'def)
                           name
                           fns))]
    {:node new-node}))

(defn create-dynamic [{:keys [node]}]
  (let [[name & methods] (rest (:children node))
        fns              (into {}
                               (comp
                                (map :children)
                                (map (fn [[name binding & body :as thing]]
                                       (prn (api/sexpr binding))
                                       [(keyword (str name))
                                        (list*
                                         (api/token-node 'fn)
                                         binding
                                         body)])))
                               methods)
        new-node         (api/list-node
                          [name fns])]
    (prn (api/sexpr new-node))
    {:node new-node}))
