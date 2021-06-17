#!/usr/bin/env bb
(require '[clojure.walk :refer [postwalk]])
(require '[clojure.string :as string])


(def events-file "src-cljc/storefront/events.cljc")


(def file (first *command-line-args*))
;; (def file "src-cljc/mayvenn/shopping_quiz/unified_freeinstall.cljc")

(defn read-file [filename]
  (drop 1 (read-string
           (str "(do"
                (slurp filename)
                ")"))))

(def event-names (into #{}
                     (comp (drop 1)
                           (map second)
                           (map #(name %)))
                     (read-file events-file)))

(defn get-referred-events [defmethod-form]
  (let [dm-body (drop 3 defmethod-form)
        events (atom [])]
    (postwalk
     (fn inner [form]
       (if (symbol? form)
         (when-let [event (some-> form name event-names)]
           (swap! events conj event))
         form))
     dm-body)
    @events))



(defn no-pipe [s]
  (string/replace s "|" "/"))

(defn print-mermaid [graph]
  (let [all-events
        (map no-pipe (vec (set (concat (keys graph) (mapcat second graph)))))
        event->id  (into {} (map-indexed (fn [idx event] [event idx]) all-events))]
    (println "graph TD;")
    (doseq [e all-events
          :let [eid (event->id e)]]
      (println (format "\t%s[%s];" eid e)))
    (doseq [[event targets] graph
          t targets]
      (println (format "\t%s --> %s;"
                       (event->id (no-pipe event))
                       (event->id (no-pipe t)))))))

(print-mermaid (into {}
                     (comp
                      (filter #(= "defmethod" (name (first %))))
                      (filter #(= "perform-effects" (name (keyword (second %)))))
                      (map (fn [defmethod-form]
                             [(name (keyword (nth defmethod-form 2)))
                              (get-referred-events defmethod-form)])))
                     (read-file file)))
