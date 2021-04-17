(ns mayvenn.visual.tools)

(defn with
  [key data]
  (let [ks (filter #(= (name key) (namespace %))
                   (keys data))]
    (into {}
          (map (fn [[k v]] [(-> k name keyword) v]))
          (select-keys data ks))))
