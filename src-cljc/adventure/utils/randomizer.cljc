(ns adventure.utils.randomizer)

(defn ^:private permutations [s]
  (lazy-seq
   (if (seq (rest s))
     (apply concat (for [x s]
                     (map #(cons x %) (permutations (remove #{x} s)))))
     [s])))

(defn randomize-ordering [random-sequence offset coll]
  (let [perms   (permutations coll)
        mod-val (nth random-sequence (dec offset))]
    (nth perms (mod mod-val (count perms)))))
