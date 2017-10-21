(ns storefront.platform.messages)

(def handle-message) ;; Dependency Injection, populated by storefront.core/main

(defn handle-later [event & [args timeout]]
  (js/setTimeout #(handle-message event args) timeout))
