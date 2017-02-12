(ns storefront.platform.messages
  (:require [storefront.keypaths :as keypaths]))

(def handle-message) ;; Dependency Injection, populated by storefront.core/main

(defn handle-later [event & [args timeout]]
  (js/setTimeout #(handle-message event args) timeout))
