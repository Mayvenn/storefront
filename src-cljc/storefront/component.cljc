(ns storefront.component
  (:require [clojure.string :as str]
            [clojure.set :as set]
            #?@(:cljs [[cljsjs.react]
                       [om.core]
                       [sablono.core :as sablono :refer-macros [html]]]
                :clj [[storefront.safe-hiccup :as safe-hiccup]])))


(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defn map->styles [m]
  (str/join (map (fn [[k v]] (str (name k) ":" v ";")) m)))

(defn normalize-style [{:keys [style] :as attrs}]
  (if style
    (update-in attrs [:style] map->styles)
    attrs))

(defn remove-handlers [m]
  (into {} (remove (fn [[k _]] (.startsWith (name k) "on-")) m)))

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

#?(:clj (defn normalize-element [[tag & content]]
          (let [[attrs body] (if (map? (first content))
                               [(first content) (apply normalize-elements (next content))]
                               [nil (apply normalize-elements content)])]
            (cond
              (:dangerouslySetInnerHTML attrs) [tag (normalize-attrs attrs) (safe-hiccup/raw (-> attrs :dangerouslySetInnerHTML :__html))]
              attrs `[~tag ~(normalize-attrs attrs) ~@body]
              :else `[~tag ~@body])))
   :cljs (def normalize-element identity))

(defn ^:private element? [v]
  (and (vector? v) (keyword? (first v))))

(defn normalize-elements [& content]
  (for [expr content]
    (cond
      (element? expr) (normalize-element expr)
      (sequential? expr) (apply normalize-elements expr)
      :else expr)))

(defn build* [component data opts]
  #?(:clj (component data nil (:opts opts))
     :cljs (om.core/build component data opts)))

(defn create* [f]
  #?(:clj (f)
     :cljs (reify
             om.core/IRender
             (render [this]
               (f)))))

(defmacro html [content]
  `(if-cljs
     (sablono.core/html ~content)
     ~content))

(defmacro create [body]
  `(create* (fn [] (or (html ~body)
                       (html [:div.component-empty])))))

(defmacro build
  ([component] `(build* ~component nil nil))
  ([component data] `(build* ~component ~data nil))
  ([component data opts] `(build* ~component ~data ~opts)))


