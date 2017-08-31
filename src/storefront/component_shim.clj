(ns storefront.component-shim
  (:require [clojure.string :as str]
            [storefront.safe-hiccup :refer [raw]]
            [clojure.set :as set]))

(defn map->styles [m]
  (str/join (map (fn [[k v]] (str (name k) ":" v ";")) m)))

(defn normalize-style [{:keys [style] :as attrs}]
  (if style
    (update-in attrs [:style] map->styles)
    attrs))

(defn remove-handlers [m]
  (into {} (remove (fn [[k v]] (.startsWith (name k) "on-")) m)))

(defn normalize-attrs [attrs]
  (-> attrs
      (set/rename-keys {:item-prop  :itemprop
                        :item-scope :itemscope
                        :item-type  :itemtype
                        :src-set    :srcset})
      (dissoc :dangerouslySetInnerHTML :key :data-test)
      remove-handlers
      normalize-style))

(declare normalize-elements)

(defn normalize-element [[tag & content]]
  (let [[attrs body] (if (map? (first content))
                       [(first content) (apply normalize-elements (next content))]
                       [nil (apply normalize-elements content)])]
    (cond
      (:dangerouslySetInnerHTML attrs) [tag (normalize-attrs attrs) (raw (-> attrs :dangerouslySetInnerHTML :__html))]
      attrs `[~tag ~(normalize-attrs attrs) ~@body]
      :else `[~tag ~@body])))

(defn ^:private element? [v]
  (and (vector? v) (keyword? (first v))))

(defn normalize-elements [& content]
  (for [expr content]
    (cond
      (element? expr) (normalize-element expr)
      (sequential? expr) (apply normalize-elements expr)
      :else expr)))

(defmacro create [content]
  content)

(defmacro build [component data opts]
  `(~component ~data nil ~opts))

(defmacro html [content]
  content)
