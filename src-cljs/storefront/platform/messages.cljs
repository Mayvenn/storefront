(ns storefront.platform.messages)

(def handle-message) ;; Dependency Injection, populated by storefront.core/main

(defn handle-later
  "Given an event and a timeout, set and then return a js timer"
  [event & [args timeout]]
  (js/setTimeout #(handle-message event args) timeout))
