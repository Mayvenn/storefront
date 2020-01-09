(ns storefront.macros
  (:require [clojure.string :as string]
            #?(:cljs [cljs.analyzer.api :refer [ns-resolve resolve]])))

(defmacro defpath [name]
  (let [split-name (string/split (str name) #"-")]
    `(def ~name ~(vec (map keyword split-name)))))

(defmacro compile-get-in
  "Identical to get-in, but resolves into a -> macro for performance.

  This is ment to optimize if a constant keypath uses keywords. Otherwise it
  falls back to [[get]].

  If the keypath cannot be resolved at compile time, an exception is raised.

  Examples:

    > (compile-get-in m [:foo :bar :baz 0])          ; => (-> m :foo :bar :baz (get 0))
    > (compile-get-in m [:a \"b\" :c] :default) ; => (-> m :foo (get \"b\") :baz (or :default))

  "
  ([m keypath] (let [kp (if (symbol? keypath)
                          (some-> (if-let [reqs (:requires (:ns &env))]
                                    (ns-resolve *ns* (symbol (some-> keypath namespace symbol reqs name) (name keypath)))
                                    (ns-resolve *ns* keypath))
                                  deref)
                          keypath)]
                 `(-> ~m
                      ~@(if (symbol? kp)
                          (throw (ex-info (str "Could not resolve keypath symbol at compile time: " (pr-str keypath))
                                          {:keypath keypath}))
                          (map (fn [k] (if (keyword? k) k `(get ~k)))
                               kp)))))
  ([m keypath default] (let [kp (if (symbol? keypath)
                                  (some-> (if-let [reqs (:requires (:ns &env))]
                                            (ns-resolve *ns* (symbol (some-> keypath namespace symbol reqs name) (name keypath)))
                                            (ns-resolve *ns* keypath))
                                          deref)
                                  keypath)]
                         `(-> ~m
                              ~@(if (symbol? kp)
                                  (throw (ex-info (str "Could not resolve keypath symbol at compile time: " (pr-str keypath))
                                                  {:keypath keypath}))
                                  (map (fn [k] (if (keyword? k) k `(get ~k)))
                                       kp))
                              (or ~default)))))
