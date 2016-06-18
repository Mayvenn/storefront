(ns storefront.component
  (:require [clojure.string :as str]))

(defn map->styles [m]
  (str/join (map (fn [[k v]] (str (name k) ":" v ";")) m)))

(defn normalize-attrs [{:keys [style] :as attrs}]
  (if style
    (update-in attrs [:style] map->styles)
    attrs))

(declare normalize-elements)

(defn normalize-element [[tag & content]]
  (let [[attrs body] (if (map? (first content))
                       [(first content) (apply normalize-elements (next content))]
                       [nil (apply normalize-elements content)])]
    (if attrs
      `[~tag ~(normalize-attrs attrs) ~@body]
      `[~tag ~@body])))

(defn element? [v]
  (and (vector? v) (keyword? (first v))))

(defn normalize-elements [& content]
  (for [expr content]
    (do
      (cond
        (element? expr) (normalize-element expr)
        (sequential? expr) (apply normalize-elements expr)
        :else expr))))

(defmacro create [content]
  content)

(defmacro build [component data opts]
  `(~component ~data nil ~opts))
