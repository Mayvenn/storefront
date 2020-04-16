(ns homepage.core
  "Homepages are apt to change often, fork and use feature-flags."
  (:require [homepage.v2020-04 :as v2020-04]))

(defn page
  [app-state]
  (v2020-04/page app-state))
