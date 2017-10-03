(ns leads.accessors)

(defn self-reg?
  "Truthy flow id indicates self-reg"
  [flow-id]
  (boolean flow-id))

