(ns storefront.experiments
  (:require [storefront.script-tags :refer [insert-tag-with-src
                                            remove-tag]]
            [storefront.config :as config]))

(defn insert-optimizely []
  (insert-tag-with-src (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely"))

(defn remove-optimizely []
  (remove-tag "optimizely"))

