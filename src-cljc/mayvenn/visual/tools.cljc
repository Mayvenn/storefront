(ns mayvenn.visual.tools)

(defn with
  "Given:
    `keyword-namespace` - A keyword which looks like a namespace
    `data` - Map with keyword keys
   Return:
    a map keyed by the name of `data`'s keys which were in the namespace of `keyword-namespace`

  Example
    (with :stylist.info {:stylist.info/name "Alice"}) => {:name "Alice"}"
  [keyword-namespace data]
  (let [ks (filter #(= (name keyword-namespace) (namespace %))
                   (keys data))]
    (into {}
          (map (fn [[k v]] [(-> k name keyword) v]))
          (select-keys data ks))))
