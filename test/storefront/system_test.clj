(ns storefront.system-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [storefront.system :refer [create-system]]))

(def test-overrides {:server-opts {:port 2390}})

(defmacro with-resource
  [bindings close-fn & body]
  `(let ~bindings
     (try
       ~@body
       (finally
         (~close-fn ~(bindings 0))))))

(defmacro with-test-system
  [sys & body]
  `(let [unstarted-system# (create-system test-overrides)]
     (with-resource [~sys (component/start unstarted-system#)]
      component/stop
      ~@body)))
