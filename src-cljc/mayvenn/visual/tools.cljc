(ns mayvenn.visual.tools
  (:require [spice.maps :as maps]
            [clojure.string :as string]))

(defn ^:private split-namespace [keyword]
  (string/split (namespace keyword) #"\."))

(defn ^:private split-name [keyword]
  (string/split (name keyword) #"\."))

(defn ^:private kw-in-ns [keyword-namespace target-keyword]
  (let [super (split-namespace target-keyword)
        sub (split-name keyword-namespace)]
    (and
     (>= (count super) (count sub))
     (= sub (subvec super 0 (count sub))))))

(defn ^:private remove-path-prefix [keyword-namespace target-keyword]
  (let [super (split-namespace target-keyword)
        sub (split-name keyword-namespace)
        namespace (string/join "." (subvec super (count sub) (count super)))]
    (when (kw-in-ns keyword-namespace target-keyword)
      (if (seq namespace)
        (keyword (string/join "." (subvec super (count sub) (count super)))
                 (name target-keyword))
        (keyword (name target-keyword))))))


(defn with
  "Given:
    `keyword-namespace` - A keyword which looks like a namespace
    `data` - Map with namespaced keyword keys Return:
    a map keyed by the name of `data`'s keys which were in the namespace of `keyword-namespace`

   Example
     (with :stylist {:stylist.info/name \"Alice\"}) => #info{:name \"Alice\"}
     (with :stylist.info {:stylist.info/name \"Alice\"}) => {:name \"Alice\"} "
  [keyword-namespace data]
  (let [ks (filter (partial kw-in-ns keyword-namespace) (keys data))]
    (into {}
          (map (fn [[k v]] [(remove-path-prefix keyword-namespace k) v]))
          (select-keys data ks))))

;; TODO This needs a better name
(defn within
  "Given:
    `keyword-namespace` - A keyword which looks like a namespace
    `map` - Map with keyword keys
   Return:
    a new map where all keys are namespaced with keyword-namespace

   Example
     (within :stylist.info {:name \"Alice\"}) => #:stylist.info{:name \"Alice\"}
     (within :stylist {:info/name \"Alice\"}) => #:stylist.info{:name \"Alice\"} "
  [keyword-namespace map]
  (maps/map-keys (fn [map-key]
                   (let [map-ns-path (split-namespace map-key)
                         kwns-path   (split-name keyword-namespace)]
                     (keyword (string/join "." (remove empty? (concat kwns-path map-ns-path)))
                              (name map-key))))
                 map))
