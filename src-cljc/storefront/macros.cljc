(ns storefront.macros
  (:require [clojure.string :as string]))

(defmacro defpath [name]
  (let [split-name (string/split (str name) #"-")]
    `(def ~name ~(vec (map keyword split-name)))))
